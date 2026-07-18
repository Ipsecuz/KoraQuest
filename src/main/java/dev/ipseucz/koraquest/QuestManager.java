package dev.ipseucz.koraquest;

<<<<<<< HEAD
import dev.ipseucz.koraquest.api.KoraQuestAPI;
import dev.ipseucz.koraquest.api.KoraQuestService;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.cycle.CycleEngine;
import dev.ipseucz.koraquest.cycle.CycleSettings;
import dev.ipseucz.koraquest.cycle.CycleState;
import dev.ipseucz.koraquest.data.PlayerDataService;
import dev.ipseucz.koraquest.data.PlayerProfile;
import dev.ipseucz.koraquest.data.PlayerQuestData;
import dev.ipseucz.koraquest.data.QuestProgressEntry;
import dev.ipseucz.koraquest.data.RerollRecord;
import dev.ipseucz.koraquest.event.QuestAcceptEvent;
import dev.ipseucz.koraquest.event.QuestCancelEvent;
import dev.ipseucz.koraquest.event.QuestCompleteEvent;
import dev.ipseucz.koraquest.event.QuestProgressEvent;
import dev.ipseucz.koraquest.event.QuestReadyEvent;
import dev.ipseucz.koraquest.event.QuestRerollEvent;
import dev.ipseucz.koraquest.event.QuestRewardEvent;
import dev.ipseucz.koraquest.integration.IntegrationManager;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
import dev.ipseucz.koraquest.model.QuestFilters;
import dev.ipseucz.koraquest.model.QuestProgressContext;
import dev.ipseucz.koraquest.model.RewardClaim;
import dev.ipseucz.koraquest.util.Text;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
=======
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.data.PlayerDataService;
import dev.ipseucz.koraquest.data.PlayerQuestData;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
<<<<<<< HEAD
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuestManager implements KoraQuestService {
    private static final Pattern RANDOM_RANGE = Pattern.compile("\\{random:(-?\\d+)-(-?\\d+)\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_COMPARE = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*(>=|<=|==|!=|>|<)\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");
=======
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class QuestManager {
    private static final Pattern RANDOM_RANGE = Pattern.compile("\\{random:(-?\\d+)-(-?\\d+)\\}", Pattern.CASE_INSENSITIVE);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final MessageService messages;
    private final PlayerDataService data;
<<<<<<< HEAD
    private final IntegrationManager integrations;
    private final CycleEngine cycles;
    private final Set<String> claimsInFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> rewardClaimsInFlight = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Map<String, Long>> cancelCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> insideVisitLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Double>> boosterRemainders = new ConcurrentHashMap<>();

    public QuestManager(KoraQuestPlugin plugin, QuestConfig config, MessageService messages,
                        PlayerDataService data, IntegrationManager integrations) {
=======
    private final Object cycleLock = new Object();

    public QuestManager(KoraQuestPlugin plugin, QuestConfig config, MessageService messages, PlayerDataService data) {
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.data = data;
<<<<<<< HEAD
        this.integrations = integrations;
        this.cycles = new CycleEngine(plugin, config, messages, data);
    }

    public void ensureCycles(boolean broadcast) { cycles.ensureAll(broadcast); }
    public void ensureCycle(boolean broadcast) { ensureCycles(broadcast); }
    public void forceRotate(CommandSender sender) { forceRotate("daily", sender); }
    public void forceRotate(String cycleName, CommandSender sender) { cycles.forceRotate(cycleName, sender); }

    public Collection<QuestDefinition> quests(String cycleName) {
        CycleState state = data.cycle(cycleName);
        if (state == null) return List.of();
        List<QuestDefinition> result = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            for (String id : state.questIds(difficulty)) {
                QuestDefinition quest = config.quest(id);
                if (quest != null) result.add(quest);
            }
        }
        result.sort(Comparator.comparing((QuestDefinition quest) -> quest.difficulty().ordinal()).thenComparing(QuestDefinition::id));
        return List.copyOf(result);
    }

    public Collection<QuestDefinition> quests(Player player, String cycleName) {
        CycleState state = data.cycle(cycleName);
        if (state == null) return List.of();
        PlayerQuestData playerData = data.player(player.getUniqueId());
        List<QuestDefinition> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (QuestDefinition original : quests(cycleName)) {
            String replacement = playerData.replacementFor(state.instanceId(), original.id());
            QuestDefinition visible = replacement == null ? original : config.quest(replacement);
            if (visible != null && added.add(visible.id())) result.add(visible);
        }
        result.sort(Comparator.comparing((QuestDefinition quest) -> quest.difficulty().ordinal()).thenComparing(QuestDefinition::id));
        return List.copyOf(result);
    }

    public Collection<QuestDefinition> dailyQuests() { return quests("daily"); }
    public Collection<QuestDefinition> dailyQuests(Difficulty difficulty) {
        return quests("daily").stream().filter(quest -> quest.difficulty() == difficulty).toList();
    }

    public PlayerQuestData playerData(UUID uuid) { return data.player(uuid); }
    public PlayerDataService playerDataService() { return data; }
    public IntegrationManager integrations() { return integrations; }
    public Optional<QuestDefinition> getQuest(String id) { return Optional.ofNullable(config.quest(id)); }
    public Collection<QuestDefinition> getActiveQuests(UUID uuid) {
        return data.player(uuid).activeEntries().stream().map(QuestProgressEntry::questId)
                .map(config::quest).filter(java.util.Objects::nonNull).toList();
    }

    public long millisUntilReset() { return millisUntilReset("daily"); }
    public long millisUntilReset(String cycleName) { return cycles.millisUntilReset(cycleName); }

    /**
     * Human friendly, cycle-specific reset text used by every interface.
     * Manual cycles intentionally do not display 0s because they only end when an admin rotates them.
     */
    public String resetTimeText(String cycleName) {
        CycleSettings settings = config.cycle(cycleName);
        if (settings == null) {
            return messages.rawOr("gui.time.unknown", "Không xác định");
        }
        if (settings.resetType() == CycleSettings.ResetType.MANUAL) {
            return messages.rawOr("gui.time.manual", "Đến khi quản trị reset");
        }
        CycleState state = data.cycle(settings.name());
        if (state == null) {
            return messages.rawOr("gui.time.loading", "Đang đồng bộ");
        }
        long remaining = millisUntilReset(settings.name());
        if (remaining <= 0L) {
            return messages.rawOr("gui.time.refreshing", "Sắp làm mới");
        }
        return Text.formatDuration(remaining);
    }

    public int activeLimit(Player player) {
        int limit = config.maxActiveQuests();
        for (int value = 100; value >= 1; value--) if (player.hasPermission("koraquest.limit." + value)) return Math.max(limit, value);
        return limit;
    }

    public int cycleActiveLimit(Player player, String cycleName) {
        CycleSettings settings = config.cycle(cycleName);
        int limit = settings == null ? activeLimit(player) : settings.activeLimit();
        for (int value = 100; value >= 1; value--) if (player.hasPermission("koraquest.limit." + cycleName + "." + value)) return Math.max(limit, value);
        return limit;
=======
    }

    public void ensureCycle(boolean broadcast) {
        synchronized (cycleLock) {
            long now = System.currentTimeMillis();
            boolean empty = dailyQuests().isEmpty();
            boolean expired = data.cycleStartedAt() <= 0L || now - data.cycleStartedAt() >= config.resetIntervalMillis();
            if (empty || expired) {
                rotateDaily(now, broadcast);
            }
        }
    }

    public void forceRotate(CommandSender sender) {
        synchronized (cycleLock) {
            rotateDaily(System.currentTimeMillis(), true);
        }
        plugin.sendSafe(sender, "forced-reset", Map.of());
    }

    private void rotateDaily(long now, boolean broadcast) {
        Map<Difficulty, List<String>> selected = config.weightedDailyEnabled() ? selectWeighted() : selectFixed();
        data.replaceCycle(now, selected);
        if (broadcast && config.broadcastReset()) {
            plugin.scheduler().runGlobal(() -> Bukkit.broadcastMessage(messages.format("messages.daily-reset", Map.of())));
        }
    }

    private Map<Difficulty, List<String>> selectFixed() {
        Map<Difficulty, List<String>> selected = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            List<String> ids = questsByDifficulty(difficulty);
            Collections.shuffle(ids);
            int amount = Math.min(ids.size(), config.dailyAmount(difficulty));
            selected.put(difficulty, List.copyOf(ids.subList(0, amount)));
        }
        return selected;
    }

    private Map<Difficulty, List<String>> selectWeighted() {
        Map<Difficulty, List<String>> remaining = new EnumMap<>(Difficulty.class);
        Map<Difficulty, List<String>> selected = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            List<String> ids = questsByDifficulty(difficulty);
            Collections.shuffle(ids);
            remaining.put(difficulty, ids);
            selected.put(difficulty, new ArrayList<>());
        }
        int total = Math.min(config.dailyTotalQuests(), config.quests().size());
        for (int index = 0; index < total; index++) {
            Difficulty rolled = rollAvailableDifficulty(remaining);
            if (rolled == null) {
                break;
            }
            List<String> pool = remaining.get(rolled);
            selected.get(rolled).add(pool.remove(0));
        }
        Map<Difficulty, List<String>> immutable = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            immutable.put(difficulty, List.copyOf(selected.get(difficulty)));
        }
        return immutable;
    }

    private Difficulty rollAvailableDifficulty(Map<Difficulty, List<String>> remaining) {
        int totalWeight = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            if (!remaining.getOrDefault(difficulty, List.of()).isEmpty()) {
                totalWeight += config.dailyWeight(difficulty);
            }
        }
        if (totalWeight <= 0) {
            return remaining.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).map(Map.Entry::getKey).findFirst().orElse(null);
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cursor = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            if (remaining.getOrDefault(difficulty, List.of()).isEmpty()) {
                continue;
            }
            cursor += config.dailyWeight(difficulty);
            if (roll < cursor) {
                return difficulty;
            }
        }
        return null;
    }

    private List<String> questsByDifficulty(Difficulty difficulty) {
        return config.quests().values().stream()
                .filter(quest -> quest.difficulty() == difficulty)
                .map(QuestDefinition::id)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Collection<QuestDefinition> dailyQuests() {
        List<QuestDefinition> result = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            result.addAll(dailyQuests(difficulty));
        }
        result.sort(Comparator.comparing((QuestDefinition quest) -> quest.difficulty().ordinal()).thenComparing(QuestDefinition::id));
        return result;
    }

    public Collection<QuestDefinition> dailyQuests(Difficulty difficulty) {
        List<QuestDefinition> result = new ArrayList<>();
        for (String id : data.dailyQuestIds(difficulty)) {
            QuestDefinition quest = config.quest(id);
            if (quest != null) {
                result.add(quest);
            }
        }
        return result;
    }

    public PlayerQuestData playerData(UUID uuid) {
        return data.player(uuid);
    }

    public long millisUntilReset() {
        long started = data.cycleStartedAt();
        if (started <= 0L) {
            return 0L;
        }
        return Math.max(0L, started + config.resetIntervalMillis() - System.currentTimeMillis());
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    public boolean accept(Player player, String questIdInput) {
        QuestDefinition quest = config.quest(questIdInput);
<<<<<<< HEAD
        if (quest == null) return fail(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
        CycleState state = data.cycle(quest.cycle());
        if (state == null || !isVisibleQuest(player, quest, state)) return fail(player, "quest-not-in-cycle", Map.of("%cycle%", quest.cycle()));
        CycleSettings settings = config.cycle(quest.cycle());
        if (settings != null && !settings.permission().isBlank() && !player.hasPermission(settings.permission())) return fail(player, "no-permission", Map.of());
        if (!quest.permission().isBlank() && !player.hasPermission(quest.permission())) return fail(player, "no-permission", Map.of());
        if (!requirementsMet(player, quest, true)) return false;
        long cooldownEnd = cancelCooldowns.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(quest.id(), 0L);
        if (cooldownEnd > System.currentTimeMillis()) {
            return fail(player, "quest-cooldown", Map.of("%time%", Text.formatDuration(cooldownEnd - System.currentTimeMillis())));
        }
        QuestAcceptEvent event = new QuestAcceptEvent(player, quest);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        PlayerQuestData playerData = data.player(player.getUniqueId());
        PlayerQuestData.AcceptResult result = playerData.accept(quest.cycle(), state.instanceId(), quest.id(),
                activeLimit(player), cycleActiveLimit(player, quest.cycle()));
        switch (result) {
            case ALREADY_ACTIVE -> messages.send(player, "quest-already-active");
            case COMPLETED -> messages.send(player, "quest-already-completed");
            case LIMIT -> messages.send(player, "quest-active-limit", Map.of("%limit%", String.valueOf(cycleActiveLimit(player, quest.cycle()))));
=======
        if (quest == null) {
            messages.send(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
            return false;
        }
        if (!data.isDailyQuest(quest.id())) {
            messages.send(player, "quest-not-daily");
            return false;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        PlayerQuestData.AcceptResult result = playerData.accept(quest.id(), config.maxActiveQuests());
        switch (result) {
            case ALREADY_ACTIVE -> messages.send(player, "quest-already-active");
            case COMPLETED -> messages.send(player, "quest-already-completed");
            case LIMIT -> messages.send(player, "quest-active-limit", Map.of("%limit%", String.valueOf(config.maxActiveQuests())));
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            case ACCEPTED -> {
                data.markDirty(player.getUniqueId(), true);
                messages.send(player, "quest-accepted", placeholders(player, quest, 0));
                return true;
            }
        }
        return false;
    }

    public boolean cancel(Player player, String questIdInput) {
        QuestDefinition quest = config.quest(questIdInput);
<<<<<<< HEAD
        if (quest == null) return fail(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
        CycleState state = data.cycle(quest.cycle());
        if (state == null) return fail(player, "quest-not-active", Map.of());
        QuestCancelEvent event = new QuestCancelEvent(player, quest);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        PlayerQuestData playerData = data.player(player.getUniqueId());
        if (!playerData.cancel(state.instanceId(), quest.id())) return fail(player, "quest-not-active", Map.of());
        if (quest.cooldownSeconds() > 0L) {
            cancelCooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                    .put(quest.id(), System.currentTimeMillis() + quest.cooldownSeconds() * 1000L);
=======
        if (quest == null) {
            messages.send(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
            return false;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        if (!playerData.cancel(quest.id())) {
            messages.send(player, "quest-not-active");
            return false;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        }
        data.markDirty(player.getUniqueId(), true);
        messages.send(player, "quest-cancelled", placeholders(player, quest, 0));
        return true;
    }

    public boolean complete(Player player, String questIdInput) {
        QuestDefinition quest = config.quest(questIdInput);
<<<<<<< HEAD
        if (quest == null) return fail(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
        CycleState state = data.cycle(quest.cycle());
        if (state == null) return fail(player, "quest-not-active", Map.of());
        PlayerQuestData playerData = data.player(player.getUniqueId());
        if (playerData.isCompleted(state.instanceId(), quest.id())) return fail(player, "quest-already-completed", Map.of());
        if (!playerData.isActive(state.instanceId(), quest.id())) return fail(player, "quest-not-active", Map.of());
        int progress = playerData.progressOf(state.instanceId(), quest.id());
        if (!playerData.isReady(state.instanceId(), quest.id(), quest.required())) {
            return fail(player, "quest-not-ready", placeholders(player, quest, progress));
        }
        if (!requirementsMet(player, quest, false)) return false;
        return beginRewardClaim(player, quest, state, quest.rewards(), true, quest.id());
    }

    public int claimAll(Player player, String cycleName) {
        int started = 0;
        CycleState state = data.cycle(cycleName);
        if (state == null) return 0;
        PlayerQuestData playerData = data.player(player.getUniqueId());
        for (QuestDefinition quest : quests(player, cycleName)) {
            if (playerData.isReady(state.instanceId(), quest.id(), quest.required()) && complete(player, quest.id())) started++;
        }
        return started;
    }

    public boolean testRewards(Player player, String questId) {
        QuestDefinition quest = config.quest(questId);
        if (quest == null) return fail(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questId)));
        if (!player.hasPermission("koraquest.admin.editor")) return fail(player, "no-permission", Map.of());
        CycleState state = data.cycle(quest.cycle());
        if (state == null) return fail(player, "cycle-not-found", Map.of("%cycle%", quest.cycle()));
        String unique = "__test_" + quest.id() + "_" + System.currentTimeMillis();
        messages.send(player, "editor-test-reward", Map.of("%quest_id%", quest.id()));
        return beginRewardClaim(player, quest, state, quest.rewards(), false, unique);
    }

    private boolean beginRewardClaim(Player player, QuestDefinition quest, CycleState state, List<String> rewards,
                                     boolean markQuestCompleted, String claimQuestId) {
        String inFlightKey = player.getUniqueId() + ":" + state.instanceId() + ":" + claimQuestId;
        if (!claimsInFlight.add(inFlightKey)) return fail(player, "reward-processing", Map.of());
        Map<String, String> completion = placeholders(player, quest, quest.required());
        List<String> frozen = new ArrayList<>();
        for (String raw : rewards) {
            String command = prepareRewardCommand(raw, completion, player);
            if (!command.isBlank() && !command.startsWith("#")) frozen.add(command);
        }
        data.createRewardClaim(player.getUniqueId(), claimQuestId, state.name(), state.instanceId(), List.copyOf(frozen))
                .whenComplete((claim, throwable) -> {
                    if (throwable != null || claim == null) {
                        claimsInFlight.remove(inFlightKey);
                        plugin.scheduler().runEntity(player, () -> messages.send(player, "reward-command-failed"));
                        return;
                    }
                    if ("DELIVERED".equalsIgnoreCase(claim.status())) {
                        claimsInFlight.remove(inFlightKey);
                        if (markQuestCompleted) plugin.scheduler().runEntity(player, () -> messages.send(player, "quest-already-completed"));
                        return;
                    }
                    if (!rewardClaimsInFlight.add(claim.claimId())) {
                        claimsInFlight.remove(inFlightKey);
                        plugin.scheduler().runEntity(player, () -> messages.send(player, "reward-processing"));
                        return;
                    }
                    plugin.scheduler().runGlobal(() -> deliverClaim(claim, player, quest, true, inFlightKey, markQuestCompleted));
                });
        messages.send(player, "reward-processing");
        return true;
    }

    private void deliverClaim(RewardClaim claim, Player player, QuestDefinition quest, boolean notify,
                              String inFlightKey, boolean markQuestCompleted) {
        data.markRewardAttempt(claim.claimId()).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                failClaimDelivery(claim, player, notify, inFlightKey, "Could not persist reward attempt: " + throwable.getMessage());
                return;
            }
            plugin.scheduler().runGlobal(() -> deliverClaimCommand(claim, Math.max(0, claim.nextCommandIndex()),
                    player, quest, notify, inFlightKey, markQuestCompleted));
        });
    }

    private void deliverClaimCommand(RewardClaim claim, int commandIndex, Player player, QuestDefinition quest,
                                     boolean notify, String inFlightKey, boolean markQuestCompleted) {
        if (commandIndex >= claim.commands().size()) {
            finalizeDeliveredClaim(claim, player, quest, notify, inFlightKey, markQuestCompleted);
            return;
        }
        String raw = claim.commands().get(commandIndex);
        String command = raw == null ? "" : raw.trim();
        if (command.startsWith("provider:")) {
            String[] parts = command.split(":", 3);
            if (parts.length < 3 || player == null || !player.isOnline()) {
                failClaimDelivery(claim, player, notify, inFlightKey, "Reward provider needs an online player: " + command);
                return;
            }
            Optional<KoraQuestAPI.RewardProvider> provider = KoraQuestAPI.rewardProvider(parts[1]);
            if (provider.isEmpty()) {
                failClaimDelivery(claim, player, notify, inFlightKey, "Reward provider is not registered: " + parts[1]);
                return;
            }
            plugin.scheduler().runEntity(player, () -> {
                boolean delivered;
                try {
                    delivered = player.isOnline() && provider.get().deliver(player, quest, parts[2]);
                } catch (RuntimeException exception) {
                    failClaimDelivery(claim, player, notify, inFlightKey,
                            "Reward provider threw: " + exception.getMessage());
                    return;
                }
                if (!delivered) {
                    failClaimDelivery(claim, player, notify, inFlightKey, "Reward provider failed: " + parts[1]);
                    return;
                }
                persistCommandAdvance(claim, commandIndex + 1, player, quest, notify, inFlightKey, markQuestCompleted);
            });
            return;
        }
        if (command.startsWith("/")) command = command.substring(1).trim();
        if (command.isBlank() || command.startsWith("#")) {
            persistCommandAdvance(claim, commandIndex + 1, player, quest, notify, inFlightKey, markQuestCompleted);
            return;
        }
        try {
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                failClaimDelivery(claim, player, notify, inFlightKey, "Reward command returned false: " + command);
                return;
            }
        } catch (RuntimeException exception) {
            failClaimDelivery(claim, player, notify, inFlightKey, "Reward command threw: " + exception.getMessage());
            return;
        }
        persistCommandAdvance(claim, commandIndex + 1, player, quest, notify, inFlightKey, markQuestCompleted);
    }

    private void persistCommandAdvance(RewardClaim claim, int nextIndex, Player player, QuestDefinition quest,
                                       boolean notify, String inFlightKey, boolean markQuestCompleted) {
        data.advanceRewardCommand(claim.claimId(), nextIndex).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                failClaimDelivery(claim, player, notify, inFlightKey, "Could not persist reward checkpoint: " + throwable.getMessage());
                return;
            }
            plugin.scheduler().runGlobal(() -> deliverClaimCommand(claim, nextIndex, player, quest, notify,
                    inFlightKey, markQuestCompleted));
        });
    }

    private void finalizeDeliveredClaim(RewardClaim claim, Player player, QuestDefinition quest, boolean notify,
                                        String inFlightKey, boolean markQuestCompleted) {
        boolean currentCycle = claim.cycleId().equals(data.cycleId(claim.cycleName()));
        data.finalizeRewardSuccess(claim, markQuestCompleted).whenComplete((ignored, throwable) -> {
            rewardClaimsInFlight.remove(claim.claimId());
            if (inFlightKey != null) claimsInFlight.remove(inFlightKey);
            if (throwable != null) {
                if (notify && player != null && player.isOnline()) plugin.scheduler().runEntity(player, () -> messages.send(player, "reward-command-failed"));
                return;
            }
            if (currentCycle && markQuestCompleted) data.markCompletedFromReward(claim.uuid(), claim.cycleName(), claim.cycleId(), claim.questId());
            if (currentCycle && player != null && player.isOnline()) {
                plugin.scheduler().runEntity(player, () -> {
                    QuestDefinition currentQuest = quest == null ? config.quest(claim.questId()) : quest;
                    if (currentQuest != null && markQuestCompleted) {
                        Bukkit.getPluginManager().callEvent(new QuestRewardEvent(player, currentQuest, claim));
                        Bukkit.getPluginManager().callEvent(new QuestCompleteEvent(player, currentQuest));
                        addSeasonProgress(player, currentQuest);
                        updateStreakIfEligible(player, currentQuest.cycle());
                        playCompletionEffects(player, currentQuest);
                        if (notify) messages.send(player, "quest-completed", placeholders(player, currentQuest, currentQuest.required()));
                    }
                });
=======
        if (quest == null) {
            messages.send(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
            return false;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        int progress = playerData.progressOf(quest.id());
        PlayerQuestData.CompleteResult result = playerData.complete(quest.id(), quest.required());
        switch (result) {
            case ALREADY_COMPLETED -> messages.send(player, "quest-already-completed");
            case NOT_ACTIVE -> messages.send(player, "quest-not-active");
            case NOT_READY -> messages.send(player, "quest-not-ready", placeholders(player, quest, progress));
            case COMPLETED -> {
                Map<String, String> completion = placeholders(player, quest, quest.required());
                data.markDirty(player.getUniqueId(), true);
                dispatchRewards(player, quest, completion);
                messages.send(player, "quest-completed", completion);
                return true;
            }
        }
        return false;
    }

    private void dispatchRewards(Player player, QuestDefinition quest, Map<String, String> placeholders) {
        List<String> commands = quest.rewards();
        if (commands.isEmpty()) {
            return;
        }
        plugin.scheduler().runGlobal(() -> {
            boolean failed = false;
            for (String raw : commands) {
                String command = prepareRewardCommand(raw, placeholders);
                if (command.isBlank() || command.startsWith("#")) {
                    continue;
                }
                if (command.startsWith("/")) {
                    command = command.substring(1).trim();
                }
                try {
                    if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                        failed = true;
                        plugin.getLogger().warning("Reward command returned false for quest '" + quest.id() + "': " + command);
                    }
                } catch (RuntimeException exception) {
                    failed = true;
                    plugin.getLogger().warning("Reward command failed for quest '" + quest.id() + "': " + exception.getMessage());
                }
            }
            if (failed && player.isOnline()) {
                plugin.scheduler().runEntity(player, () -> messages.send(player, "reward-command-failed"));
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            }
        });
    }

<<<<<<< HEAD
    private void failClaimDelivery(RewardClaim claim, Player player, boolean notify, String inFlightKey, String reason) {
        rewardClaimsInFlight.remove(claim.claimId());
        if (inFlightKey != null) claimsInFlight.remove(inFlightKey);
        plugin.getLogger().warning("Reward claim '" + claim.claimId() + "' paused: " + reason);
        if (notify && player != null && player.isOnline()) plugin.scheduler().runEntity(player, () -> messages.send(player, "reward-command-failed"));
    }

    public void retryPendingRewards(CommandSender sender) {
        data.pendingRewards(config.rewardRetryBatchSize()).whenComplete((claims, throwable) ->
                retryClaims(sender, claims, throwable, null));
    }

    public void retryPendingRewardsFor(Player player) {
        if (player == null) return;
        data.pendingRewards(player.getUniqueId(), config.rewardRetryBatchSize()).whenComplete((claims, throwable) ->
                retryClaims(null, claims, throwable, player));
    }

    private void retryClaims(CommandSender sender, List<RewardClaim> claims, Throwable throwable, Player preferredPlayer) {
        if (throwable != null) {
            plugin.sendSafe(sender, "reward-retry-failed", Map.of());
            return;
        }
        if (sender != null) plugin.sendSafe(sender, "reward-retry-started", Map.of("%amount%", String.valueOf(claims.size())));
        for (RewardClaim claim : claims) {
            if (!rewardClaimsInFlight.add(claim.claimId())) continue;
            QuestDefinition quest = config.quest(claim.questId());
            boolean markQuest = quest != null;
            Player online = preferredPlayer != null && preferredPlayer.getUniqueId().equals(claim.uuid()) && preferredPlayer.isOnline()
                    ? preferredPlayer : Bukkit.getPlayer(claim.uuid());
            if (markQuest && online == null) {
                rewardClaimsInFlight.remove(claim.claimId());
                continue;
            }
            plugin.scheduler().runGlobal(() -> deliverClaim(claim, online, quest, false, null, markQuest));
        }
    }

    public boolean reroll(Player player, String questIdInput) {
        if (!config.rerollEnabled()) return fail(player, "reroll-disabled", Map.of());
        QuestDefinition oldQuest = config.quest(questIdInput);
        if (oldQuest == null) return fail(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
        CycleState state = data.cycle(oldQuest.cycle());
        if (state == null || !isVisibleQuest(player, oldQuest, state)) return fail(player, "quest-not-in-cycle", Map.of("%cycle%", oldQuest.cycle()));
        PlayerQuestData playerData = data.player(player.getUniqueId());
        if (playerData.isActive(state.instanceId(), oldQuest.id()) || playerData.isCompleted(state.instanceId(), oldQuest.id())) {
            return fail(player, "reroll-active-denied", Map.of());
        }
        if (!oldQuest.rerollable() || config.rerollBlockedQuests().contains(oldQuest.id())) return fail(player, "reroll-blocked", Map.of());
        List<QuestDefinition> candidates = rerollCandidates(player, oldQuest, state);
        if (candidates.isEmpty()) return fail(player, "reroll-no-candidate", Map.of());
        int uses = playerData.rerollUses(state.instanceId());
        int free = rerollAllowance(player);
        boolean paid = uses >= free;
        Map<String, String> costPlaceholders = new LinkedHashMap<>(placeholders(player, oldQuest, 0));
        costPlaceholders.put("%cost%", String.valueOf(config.rerollCostAmount()));
        if (paid && !integrations.hasCost(player, config.rerollCostType(), config.rerollCostAmount(),
                config.rerollCheckCommand(), costPlaceholders)) return fail(player, "reroll-no-funds", costPlaceholders);
        QuestDefinition replacement = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        QuestRerollEvent event = new QuestRerollEvent(player, oldQuest, replacement);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        if (paid && !integrations.takeCost(player, config.rerollCostType(), config.rerollCostAmount(),
                config.rerollTakeCommand(), costPlaceholders)) return fail(player, "reroll-payment-failed", costPlaceholders);
        String originalId = originalQuestIdForVisible(playerData, state, oldQuest.id());
        playerData.addReroll(new RerollRecord(state.instanceId(), originalId, replacement.id(), System.currentTimeMillis()));
        playerData.incrementRerollUses(state.instanceId());
        data.markDirty(player.getUniqueId(), true);
        messages.send(player, "reroll-success", Map.of(
                "%old_quest%", plugin.applyExternalPlaceholders(player, messages.questName(oldQuest)),
                "%new_quest%", plugin.applyExternalPlaceholders(player, messages.questName(replacement)),
                "%remaining%", String.valueOf(Math.max(0, free - uses - 1))
        ));
        return true;
    }

    private String originalQuestIdForVisible(PlayerQuestData playerData, CycleState state, String visibleId) {
        for (QuestDefinition original : quests(state.name())) {
            String replacement = playerData.replacementFor(state.instanceId(), original.id());
            if (replacement != null && replacement.equalsIgnoreCase(visibleId)) return original.id();
        }
        return visibleId;
    }

    private List<QuestDefinition> rerollCandidates(Player player, QuestDefinition oldQuest, CycleState state) {
        Set<String> visible = quests(player, oldQuest.cycle()).stream().map(QuestDefinition::id).collect(java.util.stream.Collectors.toSet());
        PlayerQuestData playerData = data.player(player.getUniqueId());
        return config.quests().values().stream()
                .filter(quest -> quest.cycle().equalsIgnoreCase(oldQuest.cycle()))
                .filter(quest -> quest.difficulty() == oldQuest.difficulty())
                .filter(QuestDefinition::rerollable)
                .filter(quest -> !config.rerollBlockedQuests().contains(quest.id()))
                .filter(quest -> !visible.contains(quest.id()))
                .filter(quest -> !playerData.wasRerolledTo(state.instanceId(), quest.id()))
                .filter(quest -> quest.permission().isBlank() || player.hasPermission(quest.permission()))
                .toList();
    }

    private int rerollAllowance(Player player) {
        int allowance = config.freeRerolls();
        for (int value = 100; value >= 1; value--) if (player.hasPermission("koraquest.reroll." + value)) return Math.max(allowance, value);
        return allowance;
    }

    public boolean submitItems(Player player, String questIdInput, int requestedAmount) {
        QuestDefinition quest = config.quest(questIdInput);
        if (quest == null || quest.type() != ObjectiveType.ITEM_SUBMIT) return fail(player, "submit-invalid-quest", Map.of());
        CycleState state = data.cycle(quest.cycle());
        if (state == null || !data.player(player.getUniqueId()).isActive(state.instanceId(), quest.id())) return fail(player, "quest-not-active", Map.of());
        String target = quest.target().toUpperCase(Locale.ROOT);
        int remainingNeeded = Math.max(0, quest.required() - data.player(player.getUniqueId()).progressOf(state.instanceId(), quest.id()));
        int toRemove = requestedAmount <= 0 ? remainingNeeded : Math.min(remainingNeeded, requestedAmount);
        int available = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && integrations.itemTargets(stack).contains(target)) available += stack.getAmount();
        }
        int amount = Math.min(toRemove, available);
        if (amount <= 0) return fail(player, "submit-no-items", Map.of("%material%", target));
        int left = amount;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || left <= 0 || !integrations.itemTargets(stack).contains(target)) continue;
            int take = Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            left -= take;
        }
        incrementAliases(player, ObjectiveType.ITEM_SUBMIT, List.of(target), amount);
        messages.send(player, "submit-success", Map.of("%amount%", String.valueOf(amount), "%item%", target, "%material%", target));
        return true;
    }

    public void checkVisitLocations(Player player, Location current) {
        if (player == null || current == null) return;
        PlayerQuestData playerData = data.player(player.getUniqueId());
        Set<String> inside = insideVisitLocations.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());
        for (QuestProgressEntry entry : playerData.activeEntries()) {
            QuestDefinition quest = config.quest(entry.questId());
            if (quest == null || quest.type() != ObjectiveType.VISIT_LOCATION) continue;
            String definition = quest.filters().location();
            if (definition.isBlank() && quest.target() != null && !quest.target().equalsIgnoreCase("ANY")) definition = quest.target();
            VisitPoint point = VisitPoint.parse(definition, quest.filters().radius());
            if (point == null) continue;
            boolean nowInside = point.contains(current);
            String key = entry.cycleId() + "|" + quest.id();
            if (nowInside && inside.add(key)) increment(player, ObjectiveType.VISIT_LOCATION, quest.target(), 1);
            else if (!nowInside) inside.remove(key);
        }
    }

    public void increment(Player player, ObjectiveType type, String target, int amount) {
        increment(player, type, target, amount, QuestProgressContext.from(player));
    }

    public void increment(Player player, ObjectiveType type, String target, int amount, QuestProgressContext context) {
        incrementAliases(player, type, List.of(target == null ? "ANY" : target), amount, context);
    }

    public void incrementAliases(Player player, ObjectiveType type, Collection<String> targets, int amount) {
        incrementAliases(player, type, targets, amount, QuestProgressContext.from(player));
    }

    public void incrementAliases(Player player, ObjectiveType type, Collection<String> targets, int amount, QuestProgressContext context) {
        if (player == null || amount <= 0) return;
        Set<String> aliases = targets == null ? Set.of("ANY") : targets.stream().filter(java.util.Objects::nonNull)
                .map(value -> value.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        PlayerQuestData playerData = data.player(player.getUniqueId());
        boolean changed = false;
        for (QuestProgressEntry entry : playerData.activeEntries()) {
            QuestDefinition quest = config.quest(entry.questId());
            CycleState state = quest == null ? null : data.cycle(quest.cycle());
            boolean targetMatches = quest != null && (quest.target().equalsIgnoreCase("ANY") || aliases.contains(quest.target().toUpperCase(Locale.ROOT)));
            if (quest == null || state == null || !entry.cycleId().equals(state.instanceId()) || quest.type() != type
                    || !targetMatches || !matchesFilters(player, quest.filters(), context)) continue;
            int effectiveAmount = applyBooster(player, entry.cycleId(), quest.id(), amount);
            PlayerQuestData.ProgressUpdate update = playerData.increment(entry.cycleId(), quest.id(), effectiveAmount, quest.required());
            if (!update.changed()) continue;
            changed = true;
            Bukkit.getPluginManager().callEvent(new QuestProgressEvent(player, quest, update.oldProgress(), update.progress()));
            if (update.becameReady()) {
                Bukkit.getPluginManager().callEvent(new QuestReadyEvent(player, quest));
=======
    private String prepareRewardCommand(String raw, Map<String, String> placeholders) {
        String command = raw == null ? "" : raw.trim();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            command = command.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        Matcher matcher = RANDOM_RANGE.matcher(command);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            long first;
            long second;
            try {
                first = Long.parseLong(matcher.group(1));
                second = Long.parseLong(matcher.group(2));
            } catch (NumberFormatException exception) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            long min = Math.min(first, second);
            long max = Math.max(first, second);
            long rolled = rollInclusive(min, max);
            matcher.appendReplacement(output, Matcher.quoteReplacement(String.valueOf(rolled)));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private long rollInclusive(long min, long max) {
        if (min == max) {
            return min;
        }
        long range = max - min + 1L;
        if (range > 0L) {
            return min + ThreadLocalRandom.current().nextLong(range);
        }
        // Overflow means the requested interval is larger than Long.MAX_VALUE.
        // Rejection sampling remains unbiased for that uncommon configuration.
        long value;
        do {
            value = ThreadLocalRandom.current().nextLong();
        } while (value < min || value > max);
        return value;
    }

    public void increment(Player player, ObjectiveType type, String target, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        boolean changed = false;
        for (String questId : playerData.activeIds()) {
            QuestDefinition quest = config.quest(questId);
            if (quest == null || quest.type() != type || !matches(quest.target(), target)) {
                continue;
            }
            PlayerQuestData.ProgressUpdate update = playerData.increment(quest.id(), amount, quest.required());
            if (!update.changed()) {
                continue;
            }
            changed = true;
            if (update.becameReady()) {
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
                messages.send(player, "quest-ready", placeholders(player, quest, update.progress()));
            } else if (config.sendProgressMessages() && shouldSendProgress(update.oldProgress(), update.progress(), quest.required())) {
                messages.send(player, "quest-progress", placeholders(player, quest, update.progress()));
            }
<<<<<<< HEAD
            sendActionBar(player, quest, update.progress());
        }
        if (changed) data.markDirty(player.getUniqueId(), false);
    }

    private int applyBooster(Player player, String cycleId, String questId, int amount) {
        int percent = boosterPercent(player);
        if (percent <= 0 || amount <= 0) return amount;
        Map<String, Double> remainders = boosterRemainders.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
        String key = cycleId + "|" + questId;
        synchronized (remainders) {
            double extra = amount * (percent / 100.0D) + remainders.getOrDefault(key, 0.0D);
            int wholeExtra = (int) Math.floor(extra);
            double remainder = extra - wholeExtra;
            if (remainder <= 0.0000001D) remainders.remove(key); else remainders.put(key, remainder);
            long total = (long) amount + wholeExtra;
            return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, total));
        }
    }

    private int boosterPercent(Player player) {
        int best = 0;
        for (PermissionAttachmentInfo attachment : player.getEffectivePermissions()) {
            if (!attachment.getValue()) continue;
            String permission = attachment.getPermission().toLowerCase(Locale.ROOT);
            if (!permission.startsWith("koraquest.booster.")) continue;
            try { best = Math.max(best, Math.min(10_000, Integer.parseInt(permission.substring("koraquest.booster.".length())))); }
            catch (NumberFormatException ignored) { }
        }
        return best;
    }

    public void cleanupPlayer(UUID uuid) {
        if (uuid == null) return;
        boosterRemainders.remove(uuid);
        insideVisitLocations.remove(uuid);
        cancelCooldowns.remove(uuid);
    }

    public boolean progressById(Player player, String questIdInput, int amount) {
        QuestDefinition quest = config.quest(questIdInput);
        CycleState state = quest == null ? null : data.cycle(quest.cycle());
        if (quest == null || state == null || amount <= 0) return false;
        PlayerQuestData playerData = data.player(player.getUniqueId());
        PlayerQuestData.ProgressUpdate update = playerData.increment(state.instanceId(), quest.id(), amount, quest.required());
        if (!update.changed()) return false;
        data.markDirty(player.getUniqueId(), false);
        Bukkit.getPluginManager().callEvent(new QuestProgressEvent(player, quest, update.oldProgress(), update.progress()));
        if (update.becameReady()) {
            Bukkit.getPluginManager().callEvent(new QuestReadyEvent(player, quest));
=======
        }
        if (changed) {
            data.markDirty(player.getUniqueId(), false);
        }
    }

    public boolean progressById(Player player, String questIdInput, int amount) {
        QuestDefinition quest = config.quest(questIdInput);
        if (quest == null || amount <= 0) {
            return false;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        PlayerQuestData.ProgressUpdate update = playerData.increment(quest.id(), amount, quest.required());
        if (!update.changed()) {
            return false;
        }
        data.markDirty(player.getUniqueId(), false);
        if (update.becameReady()) {
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            messages.send(player, "quest-ready", placeholders(player, quest, update.progress()));
        } else if (config.sendProgressMessages() && shouldSendProgress(update.oldProgress(), update.progress(), quest.required())) {
            messages.send(player, "quest-progress", placeholders(player, quest, update.progress()));
        }
<<<<<<< HEAD
        sendActionBar(player, quest, update.progress());
        return true;
    }

    private boolean matchesFilters(Player player, QuestFilters filters, QuestProgressContext context) {
        if (filters == null || context == null) return true;
        if (!filters.worlds().isEmpty() && !filters.worlds().contains(context.world())) return false;
        if (context.y() < filters.minY() || context.y() > filters.maxY()) return false;
        if (!filters.weapon().equals("ANY") && !filters.weapon().equals(context.weapon())) return false;
        if (!filters.spawnReasons().isEmpty() && !filters.spawnReasons().contains(context.spawnReason())) return false;
        if (filters.requireCriticalHit() && !context.criticalHit()) return false;
        return integrations.locationAllowed(player, player.getLocation(), filters);
    }

    private boolean shouldSendProgress(int oldValue, int newValue, int required) {
        if (required <= 0) return false;
=======
        return true;
    }

    private boolean shouldSendProgress(int oldValue, int newValue, int required) {
        if (required <= 0) {
            return false;
        }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        int step = config.progressPercentStep();
        int oldPercent = (int) Math.floor(oldValue * 100.0D / required);
        int newPercent = (int) Math.floor(newValue * 100.0D / required);
        return oldPercent / step < newPercent / step;
    }

    private boolean matches(String configured, String actual) {
<<<<<<< HEAD
        return configured == null || configured.equalsIgnoreCase("ANY") || actual != null && configured.equalsIgnoreCase(actual);
=======
        if (configured == null || configured.equalsIgnoreCase("ANY")) {
            return true;
        }
        return actual != null && configured.equalsIgnoreCase(actual);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    public Map<String, String> placeholders(Player player, QuestDefinition quest, int progress) {
        Map<String, String> map = new LinkedHashMap<>();
<<<<<<< HEAD
        PlayerProfile profile = data.player(player.getUniqueId()).profile();
        map.put("%player%", player.getName());
        map.put("%uuid%", player.getUniqueId().toString());
        map.put("%quest_id%", quest.id());
        map.put("%quest_name%", plugin.applyExternalPlaceholders(player, messages.questName(quest)));
        map.put("%cycle%", quest.cycle());
        CycleSettings cycle = config.cycle(quest.cycle());
        map.put("%cycle_name%", cycle == null ? quest.cycle() : cycle.displayName());
=======
        map.put("%player%", player.getName());
        map.put("%uuid%", player.getUniqueId().toString());
        map.put("%quest_id%", quest.id());
        map.put("%quest_name%", messages.questName(quest));
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        map.put("%difficulty%", quest.difficulty().key());
        map.put("%difficulty_name%", messages.difficultyName(quest.difficulty()));
        map.put("%progress%", String.valueOf(progress));
        map.put("%required%", String.valueOf(quest.required()));
<<<<<<< HEAD
        map.put("%percent%", String.valueOf(Math.min(100, (int) Math.floor(progress * 100.0D / Math.max(1, quest.required())))));
        String questResetTime = resetTimeText(quest.cycle());
        map.put("%reset_time%", questResetTime);
        map.put("%quest_reset_time%", questResetTime);
        map.put("%cycle_reset_time%", questResetTime);
        map.put("%streak%", String.valueOf(profile.dailyStreak()));
        map.put("%season_level%", String.valueOf(profile.seasonLevel()));
        map.put("%season_xp%", String.valueOf(profile.seasonXp()));
        return map;
    }

    public int completed(String cycleName, UUID uuid) {
        CycleState state = data.cycle(cycleName);
        if (state == null) return 0;
        PlayerQuestData playerData = data.player(uuid);
        int count = 0;
        for (QuestDefinition quest : quests(cycleName)) if (playerData.isCompleted(state.instanceId(), quest.id())) count++;
        return count;
    }
    public int completedDaily(UUID uuid) { return completed("daily", uuid); }
    public int remaining(String cycleName, UUID uuid) { return Math.max(0, quests(cycleName).size() - completed(cycleName, uuid)); }
    public int dailyRemaining(UUID uuid) { return remaining("daily", uuid); }

    public int progress(UUID uuid, String questId) {
        QuestDefinition quest = config.quest(questId);
        CycleState state = quest == null ? null : data.cycle(quest.cycle());
        return state == null ? 0 : data.player(uuid).progressOf(state.instanceId(), quest.id());
    }

    public String status(UUID uuid, String questId) {
        QuestDefinition quest = config.quest(questId);
        CycleState state = quest == null ? null : data.cycle(quest.cycle());
        if (state == null) return "UNAVAILABLE";
        PlayerQuestData player = data.player(uuid);
        if (player.isCompleted(state.instanceId(), quest.id())) return "COMPLETED";
        if (player.isReady(state.instanceId(), quest.id(), quest.required())) return "READY";
        if (player.isActive(state.instanceId(), quest.id())) return "ACTIVE";
        return "AVAILABLE";
    }

    public void removeQuest(String id) { data.removeQuestEverywhere(id.toLowerCase(Locale.ROOT)); }

    public boolean requirementsMet(Player player, QuestDefinition quest, boolean notify) {
        for (String raw : quest.requirements()) {
            if (raw == null || raw.isBlank() || raw.startsWith("#")) continue;
            String requirement = plugin.applyExternalPlaceholders(player, raw.trim());
            boolean passed;
            if (requirement.regionMatches(true, 0, "permission:", 0, 11)) {
                passed = player.hasPermission(requirement.substring(11).trim());
            } else if (requirement.regionMatches(true, 0, "world:", 0, 6)) {
                passed = player.getWorld().getName().equalsIgnoreCase(requirement.substring(6).trim());
            } else if (requirement.regionMatches(true, 0, "gamemode:", 0, 9)) {
                passed = player.getGameMode().name().equalsIgnoreCase(requirement.substring(9).trim());
            } else if (requirement.regionMatches(true, 0, "papi:", 0, 5)) {
                passed = evaluateComparison(requirement.substring(5));
            } else {
                passed = evaluateBoolean(requirement);
            }
            if (!passed) {
                if (notify) messages.send(player, "requirement-failed", Map.of("%requirement%", raw));
                return false;
            }
        }
        return true;
    }

    private boolean evaluateComparison(String expression) {
        Matcher matcher = NUMBER_COMPARE.matcher(expression);
        if (!matcher.matches()) return evaluateBoolean(expression);
        double left = Double.parseDouble(matcher.group(1));
        double right = Double.parseDouble(matcher.group(3));
        return switch (matcher.group(2)) {
            case ">=" -> left >= right;
            case "<=" -> left <= right;
            case ">" -> left > right;
            case "<" -> left < right;
            case "==" -> Double.compare(left, right) == 0;
            case "!=" -> Double.compare(left, right) != 0;
            default -> false;
        };
    }

    private boolean evaluateBoolean(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.equalsIgnoreCase("true") || normalized.equalsIgnoreCase("yes")) return true;
        if (normalized.equalsIgnoreCase("false") || normalized.equalsIgnoreCase("no") || normalized.isBlank()) return false;
        try { return Double.parseDouble(normalized) > 0D; }
        catch (NumberFormatException ignored) { return !normalized.equals("0"); }
    }

    private boolean isVisibleQuest(Player player, QuestDefinition quest, CycleState state) {
        return quests(player, state.name()).stream().anyMatch(visible -> visible.id().equalsIgnoreCase(quest.id()));
    }

    private void updateStreakIfEligible(Player player, String cycleName) {
        if (!config.streakEnabled() || !cycleName.equalsIgnoreCase("daily")) return;
        CycleState state = data.cycle("daily");
        if (state == null) return;
        PlayerQuestData playerData = data.player(player.getUniqueId());
        List<QuestDefinition> visible = new ArrayList<>(quests(player, "daily"));
        if (visible.isEmpty() || visible.stream().anyMatch(quest -> !playerData.isCompleted(state.instanceId(), quest.id()))) return;
        PlayerProfile profile = playerData.profile();
        if (state.instanceId().equals(profile.lastDailyCycleId())) return;
        long previousCycleStartedAt = cycleStartedAt(profile.lastDailyCycleId());
        long elapsed = previousCycleStartedAt <= 0L ? Long.MAX_VALUE : state.startedAt() - previousCycleStartedAt;
        int streak;
        int tokenDelta = 0;
        if (profile.lastDailyCycleId().isBlank()) streak = 1;
        else if (elapsed > 0L && elapsed <= 36L * 3_600_000L) streak = profile.dailyStreak() + 1;
        else if (config.catchupEnabled() && elapsed > 36L * 3_600_000L
                && elapsed <= 60L * 3_600_000L && profile.catchupTokens() > 0) {
            profile = profile.useCatchupToken();
            streak = profile.dailyStreak() + 1;
        } else streak = 1;
        tokenDelta += config.streakTokenReward(streak);
        int perfectDelta = streak > 0 && streak % 7 == 0 ? 1 : 0;
        PlayerProfile updated = profile.withStreak(streak, state.instanceId(), System.currentTimeMillis(), perfectDelta, tokenDelta);
        playerData.setProfile(updated);
        data.markDirty(player.getUniqueId(), true);
        messages.send(player, "streak-updated", Map.of("%streak%", String.valueOf(streak), "%best%", String.valueOf(updated.bestDailyStreak())));
        deliverBonus(player, state, "__streak_" + streak, config.streakRewardCommands(streak));
        if (perfectDelta > 0) deliverBonus(player, state, "__perfect_week_" + updated.perfectWeeks(), config.perfectWeekCommands());
    }

    private long cycleStartedAt(String cycleId) {
        if (cycleId == null || cycleId.isBlank()) return -1L;
        int separator = cycleId.lastIndexOf(':');
        if (separator < 0 || separator + 1 >= cycleId.length()) return -1L;
        try { return Long.parseLong(cycleId.substring(separator + 1)); }
        catch (NumberFormatException ignored) { return -1L; }
    }

    private void addSeasonProgress(Player player, QuestDefinition quest) {
        if (quest.seasonXp() <= 0) return;
        PlayerQuestData playerData = data.player(player.getUniqueId());
        String seasonalCycleId = data.cycleId("seasonal");
        PlayerProfile profile = playerData.profile().alignSeason(seasonalCycleId);
        long xp = profile.seasonXp() + quest.seasonXp();
        int level = (int) Math.min(Integer.MAX_VALUE, xp / config.seasonXpPerLevel());
        int oldLevel = profile.seasonLevel();
        playerData.setProfile(profile.withSeason(seasonalCycleId, xp, level));
        data.markDirty(player.getUniqueId(), true);
        if (level > oldLevel) {
            messages.send(player, "season-level-up", Map.of("%level%", String.valueOf(level), "%xp%", String.valueOf(xp)));
            CycleState seasonal = data.cycle("seasonal");
            CycleState claimCycle = seasonal == null ? data.cycle(quest.cycle()) : seasonal;
            if (claimCycle != null) for (int current = oldLevel + 1; current <= level; current++) {
                deliverBonus(player, claimCycle, "__season_level_" + current, config.seasonLevelCommands(current));
            }
        }
    }

    private void deliverBonus(Player player, CycleState state, String claimId, List<String> commands) {
        if (state == null || commands == null || commands.isEmpty()) return;
        QuestDefinition synthetic = new QuestDefinition(claimId, state.name(), Difficulty.EASY, ObjectiveType.CUSTOM,
                "ANY", 1, Material.NETHER_STAR, claimId, List.of(), List.of(), commands,
                QuestFilters.NONE, false, "", 0L, 0, "", "");
        beginRewardClaim(player, synthetic, state, commands, false, claimId);
    }

    private void playCompletionEffects(Player player, QuestDefinition quest) {
        if (!config.guiCompletionAnimation()) return;
        try {
            if (!quest.completionSound().isBlank()) player.playSound(player.getLocation(), Sound.valueOf(quest.completionSound().toUpperCase(Locale.ROOT)), 1F, 1F);
            else player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.2F);
        } catch (IllegalArgumentException ignored) { }
        try {
            Particle particle = quest.completionParticle().isBlank() ? Particle.COMPOSTER
                    : Particle.valueOf(quest.completionParticle().toUpperCase(Locale.ROOT));
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 18, 0.4, 0.5, 0.4, 0.02);
        } catch (IllegalArgumentException ignored) { }
    }

    private void sendActionBar(Player player, QuestDefinition quest, int progress) {
        if (!config.actionBarTracker()) return;
        String bar = progressBar(progress, quest.required(), 20);
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(Text.color("&#7DE2FF" + messages.questName(quest) + " &8» " + bar + " &f" + progress + "&7/&f" + quest.required())));
    }

    public String progressBar(int current, int required, int length) {
        int filled = required <= 0 ? 0 : Math.min(length, (int) Math.round(current * length / (double) required));
        return "&#75FF75" + "■".repeat(filled) + "&#4A4A4A" + "■".repeat(Math.max(0, length - filled));
    }

    private String prepareRewardCommand(String raw, Map<String, String> placeholders, Player player) {
        String command = raw == null ? "" : raw.trim();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) command = command.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        command = plugin.applyExternalPlaceholders(player, command);
        Matcher matcher = RANDOM_RANGE.matcher(command);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            try {
                long first = Long.parseLong(matcher.group(1));
                long second = Long.parseLong(matcher.group(2));
                matcher.appendReplacement(output, Matcher.quoteReplacement(String.valueOf(rollInclusive(Math.min(first, second), Math.max(first, second)))));
            } catch (NumberFormatException exception) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private long rollInclusive(long min, long max) {
        if (min == max) return min;
        long range = max - min + 1L;
        if (range > 0L) return min + ThreadLocalRandom.current().nextLong(range);
        long value;
        do value = ThreadLocalRandom.current().nextLong(); while (value < min || value > max);
        return value;
    }

    private boolean fail(Player player, String key, Map<String, String> placeholders) {
        messages.send(player, key, placeholders);
        return false;
    }

    private record VisitPoint(String world, double x, double y, double z, double radius) {
        static VisitPoint parse(String input, double fallbackRadius) {
            if (input == null || input.isBlank()) return null;
            String[] parts = input.split(":");
            if (parts.length < 4) return null;
            try {
                double radius = parts.length >= 5 ? Double.parseDouble(parts[4]) : fallbackRadius;
                return new VisitPoint(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]), Math.max(0.1D, radius));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        boolean contains(Location location) {
            if (location.getWorld() == null || !location.getWorld().getName().equalsIgnoreCase(world)) return false;
            double dx = location.getX() - x;
            double dy = location.getY() - y;
            double dz = location.getZ() - z;
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }
=======
        return map;
    }

    public void removeQuest(String id) {
        data.removeQuestEverywhere(id.toLowerCase(Locale.ROOT));
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }
}
