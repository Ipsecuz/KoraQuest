package dev.ipseucz.koraquest.listener;

<<<<<<< HEAD
import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.data.PlayerDataService;
import dev.ipseucz.koraquest.integration.IntegrationManager;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestProgressContext;
import dev.ipseucz.koraquest.security.AntiExploitService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
=======
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.model.ObjectiveType;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
<<<<<<< HEAD
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.StonecutterInventory;
import org.bukkit.loot.Lootable;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestListener implements Listener {
    private final KoraQuestPlugin plugin;
    private final QuestManager manager;
    private final PlayerDataService data;
    private final AntiExploitService antiExploit;
    private final IntegrationManager integrations;
    private final Map<UUID, EnumMap<ObjectiveType, Double>> movementRemainders = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> lastGroundState = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> lastRegions = new ConcurrentHashMap<>();
    private final Map<String, UUID> brewingOwners = new ConcurrentHashMap<>();
    private final Set<String> lootedContainers = ConcurrentHashMap.newKeySet();

    public QuestListener(KoraQuestPlugin plugin, QuestManager manager, PlayerDataService data,
                         AntiExploitService antiExploit, IntegrationManager integrations) {
        this.plugin = plugin;
        this.manager = manager;
        this.data = data;
        this.antiExploit = antiExploit;
        this.integrations = integrations;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        data.preloadPlayerAsync(player.getUniqueId()).whenComplete((ignored, throwable) ->
                plugin.scheduler().runEntity(player, () -> {
                    if (!player.isOnline() || throwable != null) return;
                    manager.retryPendingRewardsFor(player);
                    if (antiExploit.canCount(player)) manager.increment(player, ObjectiveType.LOGIN, "ANY", 1);
                }));
        lastGroundState.put(player.getUniqueId(), player.isOnGround());
        lastRegions.put(player.getUniqueId(), integrations.worldGuardRegions(player.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        data.savePlayerAsync(uuid);
        movementRemainders.remove(uuid);
        lastGroundState.remove(uuid);
        lastRegions.remove(uuid);
        brewingOwners.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
        lootedContainers.removeIf(key -> key.startsWith(uuid + "|"));
        manager.cleanupPlayer(uuid);
=======
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;

public final class QuestListener implements Listener {
    private final QuestManager manager;

    public QuestListener(QuestManager manager) {
        this.manager = manager;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
<<<<<<< HEAD
        if (killer == null || !antiExploit.allowKill(event)) return;
        String reason;
        try { reason = event.getEntity().getEntitySpawnReason().name(); }
        catch (Throwable ignored) { reason = "DEFAULT"; }
        manager.incrementAliases(killer, ObjectiveType.KILL, manager.integrations().entityTargets(event.getEntity()), 1,
                context(killer, event.getEntity().getLocation().getBlockY(), reason, false));
=======
        if (killer != null) {
            manager.increment(killer, ObjectiveType.KILL, event.getEntityType().name(), 1);
        }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
<<<<<<< HEAD
        if (!antiExploit.allowBreak(event)) return;
        manager.increment(event.getPlayer(), ObjectiveType.BREAK, event.getBlock().getType().name(), 1,
                context(event.getPlayer(), event.getBlock().getY(), "", false));
=======
        manager.increment(event.getPlayer(), ObjectiveType.BREAK, event.getBlock().getType().name(), 1);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
<<<<<<< HEAD
        antiExploit.recordPlacement(event);
        if (!antiExploit.canCount(event.getPlayer())) return;
        manager.increment(event.getPlayer(), ObjectiveType.PLACE, event.getBlockPlaced().getType().name(), 1,
                context(event.getPlayer(), event.getBlockPlaced().getY(), "", false));
=======
        manager.increment(event.getPlayer(), ObjectiveType.PLACE, event.getBlockPlaced().getType().name(), 1);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
<<<<<<< HEAD
        if (!(event.getWhoClicked() instanceof Player player) || !antiExploit.canCount(player)) return;
        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType().isAir()) return;
        int operations = event.isShiftClick() ? calculateShiftCrafts(event.getInventory(), player, result) : 1;
        int crafted = safeMultiply(Math.max(1, result.getAmount()), operations);
        if (crafted > 0) manager.incrementAliases(player, ObjectiveType.CRAFT, manager.integrations().itemTargets(result), crafted);
    }

    private int calculateShiftCrafts(CraftingInventory inventory, Player player, ItemStack result) {
        int operationsByIngredients = Integer.MAX_VALUE;
        boolean found = false;
        for (ItemStack stack : inventory.getMatrix()) {
            if (stack == null || stack.getType().isAir()) continue;
            found = true;
            operationsByIngredients = Math.min(operationsByIngredients, stack.getAmount());
        }
        if (!found || operationsByIngredients <= 0 || operationsByIngredients == Integer.MAX_VALUE) return 0;
        long capacity = 0L;
        int maxStack = Math.max(1, result.getMaxStackSize());
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) capacity += maxStack;
            else if (stack.isSimilar(result)) capacity += Math.max(0, maxStack - stack.getAmount());
        }
        int operationsByCapacity = (int) Math.min(Integer.MAX_VALUE, capacity / Math.max(1, result.getAmount()));
        return Math.max(0, Math.min(operationsByIngredients, operationsByCapacity));
    }

    private int safeMultiply(int first, int second) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) first * second));
=======
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == Material.AIR) {
            return;
        }
        manager.increment(player, ObjectiveType.CRAFT, result.getType().name(), Math.max(1, result.getAmount()));
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
<<<<<<< HEAD
        if (!antiExploit.allowFish(event)) return;
        Entity caught = event.getCaught();
        java.util.Collection<String> targets = caught instanceof Item item ? manager.integrations().itemTargets(item.getItemStack())
                : caught == null ? java.util.List.of("ANY") : manager.integrations().entityTargets(caught);
        manager.incrementAliases(event.getPlayer(), ObjectiveType.FISH, targets, 1);
=======
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                && event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }
        Entity caught = event.getCaught();
        String target = "ANY";
        if (caught instanceof Item item) {
            target = item.getItemStack().getType().name();
        } else if (caught != null) {
            target = caught.getType().name();
        }
        manager.increment(event.getPlayer(), ObjectiveType.FISH, target, 1);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
<<<<<<< HEAD
        if (antiExploit.canCount(event.getEnchanter())) manager.incrementAliases(event.getEnchanter(), ObjectiveType.ENCHANT, manager.integrations().itemTargets(event.getItem()), 1);
=======
        manager.increment(event.getEnchanter(), ObjectiveType.ENCHANT, event.getItem().getType().name(), 1);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
<<<<<<< HEAD
        if (event.getBreeder() instanceof Player player && antiExploit.canCount(player)) manager.incrementAliases(player, ObjectiveType.BREED, manager.integrations().entityTargets(event.getEntity()), 1);
=======
        if (event.getBreeder() instanceof Player player) {
            manager.increment(player, ObjectiveType.BREED, event.getEntityType().name(), 1);
        }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
<<<<<<< HEAD
        if (event.getOwner() instanceof Player player && antiExploit.canCount(player)) manager.incrementAliases(player, ObjectiveType.TAME, manager.integrations().entityTargets(event.getEntity()), 1);
=======
        if (event.getOwner() instanceof Player player) {
            manager.increment(player, ObjectiveType.TAME, event.getEntityType().name(), 1);
        }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
<<<<<<< HEAD
        if (antiExploit.canCount(event.getPlayer())) manager.incrementAliases(event.getPlayer(), ObjectiveType.CONSUME, manager.integrations().itemTargets(event.getItem()), 1);
=======
        manager.increment(event.getPlayer(), ObjectiveType.CONSUME, event.getItem().getType().name(), 1);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
<<<<<<< HEAD
        if (antiExploit.canCount(event.getPlayer())) manager.increment(event.getPlayer(), ObjectiveType.SMELT, event.getItemType().name(), Math.max(1, event.getItemAmount()));
=======
        manager.increment(event.getPlayer(), ObjectiveType.SMELT, event.getItemType().name(), Math.max(1, event.getItemAmount()));
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
<<<<<<< HEAD
        if (antiExploit.canCount(event.getPlayer())) manager.incrementAliases(event.getPlayer(), ObjectiveType.SHEAR, manager.integrations().entityTargets(event.getEntity()), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && antiExploit.canCount(player)) {
            ItemStack stack = event.getItem().getItemStack();
            manager.incrementAliases(player, ObjectiveType.ITEM_PICKUP, manager.integrations().itemTargets(stack), stack.getAmount());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!antiExploit.canCount(event.getPlayer())) return;
        ItemStack stack = event.getItemDrop().getItemStack();
        manager.incrementAliases(event.getPlayer(), ObjectiveType.ITEM_DROP, manager.integrations().itemTargets(stack), stack.getAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!antiExploit.canCount(event.getPlayer()) || event.getClickedBlock() == null) return;
        manager.increment(event.getPlayer(), ObjectiveType.INTERACT_BLOCK, event.getClickedBlock().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (antiExploit.canCount(event.getPlayer())) manager.incrementAliases(event.getPlayer(), ObjectiveType.INTERACT_ENTITY, manager.integrations().entityTargets(event.getRightClicked()), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker != null && antiExploit.canCount(attacker)) {
            boolean critical = attacker.getFallDistance() > 0.0F && !attacker.isOnGround() && !attacker.isInsideVehicle()
                    && !attacker.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            manager.incrementAliases(attacker, ObjectiveType.DAMAGE_DEALT, manager.integrations().entityTargets(event.getEntity()),
                    Math.max(1, (int) Math.ceil(event.getFinalDamage())),
                    context(attacker, event.getEntity().getLocation().getBlockY(), "", critical));
        }
        if (event.getEntity() instanceof Player player && antiExploit.canCount(player)) {
            manager.increment(player, ObjectiveType.DAMAGE_TAKEN, event.getDamager().getType().name(),
                    Math.max(1, (int) Math.ceil(event.getFinalDamage())));
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOtherDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent || !(event.getEntity() instanceof Player player) || !antiExploit.canCount(player)) return;
        manager.increment(player, ObjectiveType.DAMAGE_TAKEN, event.getCause().name(), Math.max(1, (int) Math.ceil(event.getFinalDamage())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player && antiExploit.canCount(player)) manager.increment(player, ObjectiveType.HEAL, event.getRegainReason().name(), Math.max(1, (int) Math.ceil(event.getAmount())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (antiExploit.canCount(event.getEntity())) manager.increment(event.getEntity(), ObjectiveType.DIE, "ANY", 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!antiExploit.canCount(event.getPlayer())) return;
        String command = event.getMessage().substring(1).split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        manager.increment(event.getPlayer(), ObjectiveType.COMMAND, command, 1);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (antiExploit.canCount(player)) plugin.scheduler().runEntity(player, () -> manager.increment(player, ObjectiveType.CHAT, "ANY", 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!antiExploit.canCount(event.getPlayer())) return;
        ItemStack result = event.getItemStack();
        manager.increment(event.getPlayer(), ObjectiveType.BUCKET_FILL,
                result == null ? event.getBucket().name() : result.getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMend(PlayerItemMendEvent event) {
        if (antiExploit.canCount(event.getPlayer())) manager.increment(event.getPlayer(), ObjectiveType.REPAIR, event.getItem().getType().name(), event.getRepairAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (antiExploit.canCount(player)) manager.increment(player, ObjectiveType.ENTER_WORLD, player.getWorld().getName(), 1);
        lastRegions.put(player.getUniqueId(), integrations.worldGuardRegions(player.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExp(PlayerExpChangeEvent event) {
        if (event.getAmount() > 0 && antiExploit.canCount(event.getPlayer())) manager.increment(event.getPlayer(), ObjectiveType.EXP_GAIN, "ANY", event.getAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevel(PlayerLevelChangeEvent event) {
        int gained = event.getNewLevel() - event.getOldLevel();
        if (gained > 0 && antiExploit.canCount(event.getPlayer())) manager.increment(event.getPlayer(), ObjectiveType.LEVEL_GAIN, "ANY", gained);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResultInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !antiExploit.canCount(player)) return;
        Inventory inventory = event.getInventory();
        ObjectiveType type;
        int resultSlot;
        if (inventory instanceof AnvilInventory) { type = ObjectiveType.ANVIL; resultSlot = 2; }
        else if (inventory instanceof SmithingInventory) { type = ObjectiveType.SMITHING; resultSlot = 3; }
        else if (inventory instanceof StonecutterInventory) { type = ObjectiveType.STONECUT; resultSlot = 1; }
        else if (inventory instanceof MerchantInventory) { type = ObjectiveType.TRADE_VILLAGER; resultSlot = 2; }
        else return;
        if (event.getRawSlot() != resultSlot) return;
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;
        manager.incrementAliases(player, type, manager.integrations().itemTargets(result), Math.max(1, result.getAmount()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !antiExploit.canCount(player)) return;
        if (event.getInventory() instanceof BrewerInventory brewer && brewer.getHolder() instanceof BrewingStand stand) {
            brewingOwners.put(locationKey(stand.getLocation()), player.getUniqueId());
        }
        if (event.getInventory().getHolder() instanceof Container container && container instanceof Lootable lootable
                && lootable.getLootTable() != null) {
            String key = player.getUniqueId() + "|" + locationKey(container.getLocation());
            if (lootedContainers.add(key)) manager.increment(player, ObjectiveType.LOOT_CHEST, container.getType().name(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() instanceof BrewerInventory brewer && brewer.getHolder() instanceof BrewingStand stand) {
            brewingOwners.remove(locationKey(stand.getLocation()), event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!(event.getContents().getHolder() instanceof BrewingStand stand)) return;
        UUID uuid = brewingOwners.get(locationKey(stand.getLocation()));
        Player player = uuid == null ? null : plugin.getServer().getPlayer(uuid);
        if (player == null || !antiExploit.canCount(player)) return;
        String target = "POTION";
        for (ItemStack stack : event.getContents().getContents()) {
            if (stack != null && stack.getType() != Material.AIR && stack.getType().name().contains("POTION")) {
                target = stack.getType().name();
                break;
            }
        }
        manager.increment(player, ObjectiveType.POTION_BREW, target, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        Player player = event.getPlayer();
        if (to == null || from.getWorld() != to.getWorld() || !antiExploit.canCount(player)) return;
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > 0.0D && horizontal < 20.0D) addMovement(player, movementType(player), horizontal);
        boolean previousGround = lastGroundState.getOrDefault(player.getUniqueId(), player.isOnGround());
        boolean currentGround = player.isOnGround();
        if (previousGround && !currentGround && to.getY() > from.getY() && player.getVelocity().getY() > 0.0D) manager.increment(player, ObjectiveType.JUMP, "ANY", 1);
        lastGroundState.put(player.getUniqueId(), currentGround);
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            Set<String> currentRegions = integrations.worldGuardRegions(to);
            Set<String> previous = lastRegions.getOrDefault(player.getUniqueId(), Set.of());
            for (String region : currentRegions) if (!previous.contains(region)) manager.increment(player, ObjectiveType.ENTER_REGION, region, 1);
            lastRegions.put(player.getUniqueId(), currentRegions);
            manager.checkVisitLocations(player, to);
        }
    }

    private ObjectiveType movementType(Player player) {
        if (player.isGliding()) return ObjectiveType.GLIDE;
        if (player.isFlying()) return ObjectiveType.FLY;
        if (player.isSwimming()) return ObjectiveType.SWIM;
        if (player.isSprinting()) return ObjectiveType.SPRINT;
        return ObjectiveType.WALK;
    }

    private void addMovement(Player player, ObjectiveType type, double amount) {
        EnumMap<ObjectiveType, Double> values = movementRemainders.computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(ObjectiveType.class));
        double total = values.getOrDefault(type, 0.0D) + amount;
        int whole = (int) Math.floor(total);
        values.put(type, total - whole);
        if (whole > 0) manager.increment(player, type, "ANY", whole);
    }

    private QuestProgressContext context(Player player, int y, String spawnReason, boolean critical) {
        return new QuestProgressContext(player.getWorld().getName(), y,
                player.getInventory().getItemInMainHand().getType().name(), spawnReason, critical);
    }

    private String locationKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
=======
        manager.increment(event.getPlayer(), ObjectiveType.SHEAR, event.getEntity().getType().name(), 1);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }
}
