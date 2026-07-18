package dev.ipseucz.koraquest.integration;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestFilters;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class IntegrationManager implements Listener {
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final Map<UUID, EconomySnapshot> economySnapshots = new ConcurrentHashMap<>();
    private final Set<String> integrationWarnings = ConcurrentHashMap.newKeySet();
    private volatile Object vaultEconomy;
    private volatile boolean worldGuardLookupFailed;

    public IntegrationManager(KoraQuestPlugin plugin, QuestConfig config) {
        this.plugin = plugin;
        this.config = config;
        setupVault();
        logDetectedIntegrations();
    }


    private void logDetectedIntegrations() {
        String[] names = {"PlaceholderAPI", "Vault", "PlayerPoints", "CoinsEngine", "Citizens", "WorldGuard", "MythicMobs",
                "SuperiorSkyblock2", "BentoBox", "PlotSquared", "Jobs", "mcMMO", "ItemsAdder", "Oraxen", "Nexo",
                "MMOItems", "ExecutableItems", "ExcellentCrates", "VotingPlugin", "Votifier"};
        java.util.List<String> enabled = java.util.Arrays.stream(names).filter(name -> Bukkit.getPluginManager().isPluginEnabled(name)).toList();
        if (!enabled.isEmpty()) plugin.getLogger().info("Detected compatible plugins: " + String.join(", ", enabled));
    }

    public void registerExternalObjectiveHooks(QuestManager manager) {
        registerVoteEvent(manager, "com.vexsoftware.votifier.model.VotifierEvent");
        registerVoteEvent(manager, "com.bencodez.votingplugin.events.PlayerVoteEvent");
        registerVoteEvent(manager, "com.bencodez.votingplugin.events.PlayerPostVoteEvent");
        registerProgressEvent(manager, "com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent",
                "MCMMO", new String[]{"getSkill"}, new String[]{"getRawXpGained", "getXpGained"});
        registerProgressEvent(manager, "com.gamingmesh.jobs.api.JobsExpGainEvent",
                "JOBS", new String[]{"getJob"}, new String[]{"getExp"});
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerVoteEvent(QuestManager manager, String className) {
        try {
            Class<?> raw = Class.forName(className);
            if (!Event.class.isAssignableFrom(raw)) return;
            Class<? extends Event> eventClass = (Class<? extends Event>) raw;
            EventExecutor executor = (listener, event) -> {
                Player player = resolveVotePlayer(event);
                if (player != null) plugin.scheduler().runEntity(player,
                        () -> manager.increment(player, ObjectiveType.VOTE, "ANY", 1));
            };
            Bukkit.getPluginManager().registerEvent((Class) eventClass, this, EventPriority.MONITOR, executor, plugin, true);
            plugin.getLogger().info("Hooked vote objective: " + className);
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Could not hook vote event " + className, throwable);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerProgressEvent(QuestManager manager, String className, String namespace,
                                       String[] targetMethods, String[] amountMethods) {
        try {
            Class<?> raw = Class.forName(className);
            if (!Event.class.isAssignableFrom(raw)) return;
            Class<? extends Event> eventClass = (Class<? extends Event>) raw;
            EventExecutor executor = (listener, event) -> {
                Player player = resolveEventPlayer(event);
                if (player == null) return;
                String target = externalTarget(event, targetMethods);
                int amount = externalAmount(event, amountMethods);
                java.util.List<String> aliases = target.isBlank()
                        ? java.util.List.of(namespace + ":ANY")
                        : java.util.List.of(namespace + ":ANY", namespace + ":" + target);
                plugin.scheduler().runEntity(player,
                        () -> manager.incrementAliases(player, ObjectiveType.CUSTOM, aliases, amount));
            };
            Bukkit.getPluginManager().registerEvent((Class) eventClass, this, EventPriority.MONITOR, executor, plugin, true);
            plugin.getLogger().info("Hooked external objective: " + className + " -> CUSTOM " + namespace + ":<target>");
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Could not hook external objective " + className, throwable);
        }
    }

    private Player resolveEventPlayer(Object event) {
        for (String methodName : new String[]{"getPlayer", "getOfflinePlayer"}) {
            try {
                Object value = event.getClass().getMethod(methodName).invoke(event);
                if (value instanceof Player player) return player;
                if (value instanceof OfflinePlayer offline) return offline.getPlayer();
                if (value instanceof UUID uuid) return Bukkit.getPlayer(uuid);
                if (value instanceof String name) return Bukkit.getPlayerExact(name);
            } catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }

    private String externalTarget(Object event, String[] methods) {
        for (String method : methods) {
            try {
                Object value = event.getClass().getMethod(method).invoke(event);
                if (value == null) continue;
                Object named = invokeNoArg(value, "getName", "getKey", "name");
                String result = String.valueOf(named == null ? value : named).trim();
                if (!result.isBlank()) return result.toUpperCase(Locale.ROOT).replace(' ', '_');
            } catch (ReflectiveOperationException ignored) { }
        }
        return "";
    }

    private int externalAmount(Object event, String[] methods) {
        for (String method : methods) {
            try {
                Object value = event.getClass().getMethod(method).invoke(event);
                if (value instanceof Number number) {
                    double raw = number.doubleValue();
                    if (!Double.isFinite(raw) || raw <= 0D) return 1;
                    return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) Math.floor(raw)));
                }
            } catch (ReflectiveOperationException ignored) { }
        }
        return 1;
    }

    private Player resolveVotePlayer(Object event) {
        for (String methodName : new String[]{"getPlayer", "getPlayerName", "getUsername", "getVote"}) {
            try {
                Object value = event.getClass().getMethod(methodName).invoke(event);
                if (value instanceof Player player) return player;
                if (value instanceof OfflinePlayer offline) return offline.getPlayer();
                if (value instanceof String name) return Bukkit.getPlayerExact(name);
                if (value != null && methodName.equals("getVote")) {
                    for (String nested : new String[]{"getUsername", "getPlayerName"}) {
                        try {
                            Object name = value.getClass().getMethod(nested).invoke(value);
                            if (name != null) return Bukkit.getPlayerExact(String.valueOf(name));
                        } catch (ReflectiveOperationException ignored) { }
                    }
                }
            } catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }


    public Set<String> itemTargets(ItemStack item) {
        if (item == null) return Set.of("AIR");
        Set<String> targets = new java.util.LinkedHashSet<>();
        targets.add(item.getType().name());
        String itemsAdder = staticItemId("dev.lone.itemsadder.api.CustomStack", "byItemStack", item, "getNamespacedID");
        if (itemsAdder != null) targets.add(itemsAdder);
        String oraxen = staticItemId("io.th0rgal.oraxen.api.OraxenItems", "getIdByItem", item, null);
        if (oraxen != null) targets.add(oraxen);
        String nexo = staticItemId("com.nexomc.nexo.api.NexoItems", "idFromItem", item, null);
        if (nexo != null) targets.add(nexo);
        String mmo = mmoItemId(item);
        if (mmo != null) targets.add(mmo);
        String executable = persistentId(item, "executableitems", "ei-id", "executableitem");
        if (executable != null) targets.add(executable);
        return targets.stream().filter(value -> value != null && !value.isBlank()).map(value -> value.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Set<String> entityTargets(Entity entity) {
        if (entity == null) return Set.of("UNKNOWN");
        Set<String> targets = new java.util.LinkedHashSet<>();
        targets.add(entity.getType().name());
        String citizen = citizenTarget(entity);
        if (citizen != null) targets.add(citizen);
        if (entity instanceof LivingEntity living && isMythicMob(living)) {
            targets.add("MYTHIC_MOB");
            for (MetadataValue value : living.getMetadata("MythicMobType")) {
                if (value.value() == null) continue;
                String mythicId = String.valueOf(value.value()).trim();
                if (mythicId.isEmpty()) continue;
                targets.add(mythicId);
                // A stable namespaced alias lets quest configs clearly distinguish MythicMobs IDs.
                targets.add("MYTHIC_" + mythicId);
            }
        }
        return targets.stream().map(value -> value.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private String staticItemId(String className, String factory, ItemStack item, String getter) {
        try {
            Class<?> type = Class.forName(className);
            Object value = type.getMethod(factory, ItemStack.class).invoke(null, item);
            if (value == null) return null;
            if (getter == null) return String.valueOf(value);
            Object id = value.getClass().getMethod(getter).invoke(value);
            return id == null ? null : String.valueOf(id);
        } catch (Throwable ignored) { return null; }
    }

    private String mmoItemId(ItemStack item) {
        try {
            Class<?> nbt = Class.forName("net.Indyuce.mmoitems.api.Type");
            Object type = nbt.getMethod("get", ItemStack.class).invoke(null, item);
            Class<?> mmo = Class.forName("net.Indyuce.mmoitems.MMOItems");
            Object instance = mmo.getField("plugin").get(null);
            Object id = instance.getClass().getMethod("getItemId", ItemStack.class).invoke(instance, item);
            return type == null || id == null ? null : String.valueOf(type) + ":" + id;
        } catch (Throwable ignored) { return null; }
    }

    private String persistentId(ItemStack item, String... hints) {
        try {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta == null) return null;
            for (org.bukkit.NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
                String text = key.toString().toLowerCase(Locale.ROOT);
                for (String hint : hints) if (text.contains(hint)) return key.toString();
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private String citizenTarget(Entity entity) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) return null;
        try {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            Object npc = registry.getClass().getMethod("getNPC", Entity.class).invoke(registry, entity);
            if (npc == null) return null;
            Object id = npc.getClass().getMethod("getId").invoke(npc);
            Object name = npc.getClass().getMethod("getName").invoke(npc);
            return "CITIZENS:" + id + ":" + name;
        } catch (Throwable ignored) { return null; }
    }

    public boolean locationAllowed(Player player, Location location, QuestFilters filters) {
        if (player == null || location == null) return false;
        Set<String> regions = worldGuardRegions(location);
        boolean usesRegions = !config.blockedRegions().isEmpty() || !config.allowedRegions().isEmpty()
                || filters != null && !filters.regions().isEmpty();
        if (usesRegions && Bukkit.getPluginManager().isPluginEnabled("WorldGuard") && worldGuardLookupFailed) return false;
        if (!config.blockedRegions().isEmpty() && regions.stream().map(String::toLowerCase).anyMatch(config.blockedRegions()::contains)) return false;
        if (!config.allowedRegions().isEmpty() && regions.stream().map(String::toLowerCase).noneMatch(config.allowedRegions()::contains)) return false;
        if (filters != null && !filters.regions().isEmpty() && regions.stream().noneMatch(filters.regions()::contains)) return false;
        if ((config.requireOwnIslandGlobally() || filters != null && filters.requireOwnIsland()) && !isOwnIsland(player, location)) return false;
        return !(config.requireOwnPlotGlobally() || filters != null && filters.requireOwnPlot()) || isOwnPlot(player, location);
    }

    public Set<String> worldGuardRegions(Location location) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) return Set.of();
        try {
            Class<?> adapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object worldEditLocation = adapter.getMethod("adapt", Location.class).invoke(null, location);
            Class<?> worldGuard = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object instance = worldGuard.getMethod("getInstance").invoke(null);
            Object platform = instance.getClass().getMethod("getPlatform").invoke(instance);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = container.getClass().getMethod("createQuery").invoke(container);
            Method applicable = findCompatibleMethod(query.getClass(), "getApplicableRegions", worldEditLocation.getClass());
            if (applicable == null) return Set.of();
            Object regionSet = applicable.invoke(query, worldEditLocation);
            Set<String> ids = new HashSet<>();
            if (regionSet instanceof Iterable<?> iterable) {
                for (Object region : iterable) {
                    Object id = region.getClass().getMethod("getId").invoke(region);
                    if (id != null) ids.add(String.valueOf(id).toUpperCase(Locale.ROOT));
                }
            }
            worldGuardLookupFailed = false;
            return Set.copyOf(ids);
        } catch (Throwable throwable) {
            worldGuardLookupFailed = true;
            warnIntegrationOnce("worldguard-regions", "WorldGuard region lookup failed; region-restricted quest progress is blocked for safety.", throwable);
            return Set.of();
        }
    }

    public boolean isOwnIsland(Player player, Location location) {
        boolean integrationEnabled = false;
        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            integrationEnabled = true;
            Boolean result = superiorIslandMember(player, location);
            if (result != null) return result;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("BentoBox")) {
            integrationEnabled = true;
            Boolean result = bentoIslandMember(player, location);
            if (result != null) return result;
        }
        if (integrationEnabled) {
            warnIntegrationOnce("island-owner", "Island ownership lookup failed; own-island quest progress is blocked for safety.", null);
            return false;
        }
        return true;
    }

    private Boolean superiorIslandMember(Player player, Location location) {
        try {
            Class<?> api = Class.forName("com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI");
            Object island = api.getMethod("getIslandAt", Location.class).invoke(null, location);
            if (island == null) return false;
            for (String method : new String[]{"isMember", "isCoop", "isPlayerInIsland"}) {
                try {
                    Object result = island.getClass().getMethod(method, UUID.class).invoke(island, player.getUniqueId());
                    if (result instanceof Boolean value && value) return true;
                } catch (ReflectiveOperationException ignored) { }
            }
            Object owner = invokeNoArg(island, "getOwnerUUID", "getOwner");
            if (owner instanceof UUID uuid && uuid.equals(player.getUniqueId())) return true;
            Object members = invokeNoArg(island, "getIslandMembers", "getMembers", "getAllPlayers");
            return containsUuid(members, player.getUniqueId());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Boolean bentoIslandMember(Player player, Location location) {
        try {
            Plugin bento = Bukkit.getPluginManager().getPlugin("BentoBox");
            if (bento == null) return null;
            Object islands = bento.getClass().getMethod("getIslands").invoke(bento);
            Object optional = islands.getClass().getMethod("getIslandAt", Location.class).invoke(islands, location);
            Object island = optional instanceof Optional<?> value ? value.orElse(null) : optional;
            if (island == null) return false;
            Object members = invokeNoArg(island, "getMemberSet", "getMembers", "getMemberSetWithoutOwners");
            if (containsUuid(members, player.getUniqueId())) return true;
            Object owner = invokeNoArg(island, "getOwner", "getOwnerUUID");
            return owner instanceof UUID uuid && uuid.equals(player.getUniqueId());
        } catch (Throwable ignored) {
            return null;
        }
    }

    public boolean isOwnPlot(Player player, Location location) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlotSquared")) return true;
        try {
            Class<?> util = Class.forName("com.plotsquared.bukkit.util.BukkitUtil");
            Object plotLocation = util.getMethod("adapt", Location.class).invoke(null, location);
            Object plot = plotLocation.getClass().getMethod("getPlot").invoke(plotLocation);
            if (plot == null) return false;
            for (String method : new String[]{"isAdded", "isOwner", "isTrusted", "isMember"}) {
                try {
                    Object result = plot.getClass().getMethod(method, UUID.class).invoke(plot, player.getUniqueId());
                    if (result instanceof Boolean value && value) return true;
                } catch (ReflectiveOperationException ignored) { }
            }
            return false;
        } catch (Throwable throwable) {
            warnIntegrationOnce("plotsquared-owner", "PlotSquared ownership lookup failed; own-plot quest progress is blocked for safety.", throwable);
            return false;
        }
    }

    private void warnIntegrationOnce(String key, String message, Throwable throwable) {
        if (!integrationWarnings.add(key)) return;
        if (throwable == null) plugin.getLogger().warning(message);
        else plugin.getLogger().log(Level.WARNING, message, throwable);
    }

    public boolean isMythicMob(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.hasMetadata("MythicMob")) return true;
        for (String key : new String[]{"MythicMobType", "MythicMobLevel", "mythicmob"}) {
            if (!entity.hasMetadata(key)) continue;
            for (MetadataValue value : entity.getMetadata(key)) if (value.value() != null) return true;
        }
        try {
            Class<?> apiClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object api = apiClass.getMethod("inst").invoke(null);
            Object mobManager = api.getClass().getMethod("getMobManager").invoke(api);
            Object active = mobManager.getClass().getMethod("isActiveMob", UUID.class).invoke(mobManager, entity.getUniqueId());
            if (active instanceof Boolean value) return value;
        } catch (Throwable ignored) { }
        return entity.getPersistentDataContainer().getKeys().stream()
                .anyMatch(key -> key.getNamespace().toLowerCase(Locale.ROOT).contains("mythic"));
    }

    public void tickEconomyObjectives(QuestManager manager) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.scheduler().runEntity(player, () -> {
                if (!player.isOnline()) return;
                double money = vaultBalance(player);
                int points = playerPointsBalance(player);
                EconomySnapshot old = economySnapshots.put(player.getUniqueId(), new EconomySnapshot(money, points));
                if (old == null) return;
                double moneyDelta = money - old.money();
                if (moneyDelta > 0.009D) manager.increment(player, ObjectiveType.MONEY_EARN, "VAULT", Math.max(1, (int) Math.floor(moneyDelta)));
                else if (moneyDelta < -0.009D) manager.increment(player, ObjectiveType.MONEY_SPEND, "VAULT", Math.max(1, (int) Math.floor(-moneyDelta)));
                int pointDelta = points - old.points();
                if (pointDelta > 0) manager.increment(player, ObjectiveType.MONEY_EARN, "PLAYERPOINTS", pointDelta);
                else if (pointDelta < 0) manager.increment(player, ObjectiveType.MONEY_SPEND, "PLAYERPOINTS", -pointDelta);
            });
        }
        economySnapshots.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    public boolean takeCost(Player player, String type, double amount, String takeCommand, Map<String, String> placeholders) {
        if (amount <= 0D || type.equalsIgnoreCase("FREE")) return true;
        if (type.equalsIgnoreCase("VAULT")) return withdrawVault(player, amount);
        if (type.equalsIgnoreCase("PLAYERPOINTS")) return takePlayerPoints(player, (int) Math.ceil(amount));
        if (type.equalsIgnoreCase("COMMAND")) {
            String command = replace(takeCommand, placeholders);
            if (command.startsWith("/")) command = command.substring(1);
            return !command.isBlank() && Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        return false;
    }

    public boolean hasCost(Player player, String type, double amount, String checkCommand, Map<String, String> placeholders) {
        if (amount <= 0D || type.equalsIgnoreCase("FREE")) return true;
        if (type.equalsIgnoreCase("VAULT")) return vaultBalance(player) >= amount;
        if (type.equalsIgnoreCase("PLAYERPOINTS")) return playerPointsBalance(player) >= Math.ceil(amount);
        if (type.equalsIgnoreCase("COMMAND")) {
            if (checkCommand == null || checkCommand.isBlank()) return true;
            String command = replace(checkCommand, placeholders);
            if (command.startsWith("/")) command = command.substring(1);
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        return false;
    }

    private void setupVault() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = Bukkit.getServicesManager().getClass().getMethod("getRegistration", Class.class)
                    .invoke(Bukkit.getServicesManager(), economyClass);
            if (registration != null) vaultEconomy = registration.getClass().getMethod("getProvider").invoke(registration);
        } catch (Throwable ignored) {
            vaultEconomy = null;
        }
    }

    private double vaultBalance(Player player) {
        if (vaultEconomy == null) return 0D;
        try {
            Object value = vaultEconomy.getClass().getMethod("getBalance", OfflinePlayer.class).invoke(vaultEconomy, player);
            return value instanceof Number number ? number.doubleValue() : 0D;
        } catch (Throwable ignored) {
            return 0D;
        }
    }

    private boolean withdrawVault(Player player, double amount) {
        if (vaultEconomy == null || vaultBalance(player) < amount) return false;
        try {
            Object response = vaultEconomy.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class)
                    .invoke(vaultEconomy, player, amount);
            Object success = response.getClass().getMethod("transactionSuccess").invoke(response);
            return success instanceof Boolean value && value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int playerPointsBalance(Player player) {
        try {
            Class<?> main = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
            Object instance = main.getMethod("getInstance").invoke(null);
            Object api = instance.getClass().getMethod("getAPI").invoke(instance);
            Object value = api.getClass().getMethod("look", UUID.class).invoke(api, player.getUniqueId());
            return value instanceof Number number ? number.intValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private boolean takePlayerPoints(Player player, int amount) {
        if (amount <= 0 || playerPointsBalance(player) < amount) return amount <= 0;
        try {
            Class<?> main = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
            Object instance = main.getMethod("getInstance").invoke(null);
            Object api = instance.getClass().getMethod("getAPI").invoke(instance);
            Object result = api.getClass().getMethod("take", UUID.class, int.class).invoke(api, player.getUniqueId(), amount);
            return !(result instanceof Boolean value) || value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private String replace(String value, Map<String, String> placeholders) {
        String result = value == null ? "" : value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) result = result.replace(entry.getKey(), entry.getValue());
        return result;
    }

    private boolean containsUuid(Object value, UUID uuid) {
        if (value instanceof Map<?, ?> map) return map.keySet().contains(uuid) || map.values().contains(uuid);
        if (value instanceof Collection<?> collection) return collection.contains(uuid);
        return false;
    }

    private Object invokeNoArg(Object target, String... names) {
        for (String name : names) {
            try { return target.getClass().getMethod(name).invoke(target); }
            catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }

    private Method findCompatibleMethod(Class<?> type, String name, Class<?> parameterType) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(parameterType)) return method;
        }
        return null;
    }

    private record EconomySnapshot(double money, int points) { }
}
