package dev.ipseucz.koraquest.api;

import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public runtime API. Player mutations must run on that player's owning thread
 * when a server uses Folia. Query methods are safe for already-loaded data.
 */
public final class KoraQuestAPI {
    private static volatile KoraQuestService manager;
    private static final Map<String, ObjectiveProvider> objectiveProviders = new ConcurrentHashMap<>();
    private static final Map<String, RewardProvider> rewardProviders = new ConcurrentHashMap<>();

    private KoraQuestAPI() { }
    public static void bootstrap(KoraQuestService questService) { manager = questService; }
    public static boolean isAvailable() { return manager != null; }

    public static void shutdown() {
        manager = null;
        objectiveProviders.clear();
        rewardProviders.clear();
    }

    public static Optional<QuestDefinition> getQuest(String id) {
        KoraQuestService current = manager;
        return current == null ? Optional.empty() : current.getQuest(id);
    }

    public static Collection<QuestDefinition> getActiveQuests(UUID uuid) {
        KoraQuestService current = manager;
        return current == null || uuid == null ? java.util.List.of() : current.getActiveQuests(uuid);
    }

    public static Collection<QuestDefinition> getActiveQuests(UUID uuid, String cycle) {
        return getActiveQuests(uuid).stream().filter(quest -> quest.cycle().equalsIgnoreCase(cycle)).toList();
    }

    public static int getProgress(UUID uuid, String questId) {
        KoraQuestService current = manager;
        return current == null || uuid == null ? 0 : current.progress(uuid, questId);
    }

    public static int getProgress(UUID uuid, String cycle, String questId) {
        QuestDefinition quest = getQuest(questId).orElse(null);
        return quest == null || !quest.cycle().equalsIgnoreCase(cycle) ? 0 : getProgress(uuid, questId);
    }

    public static String getStatus(UUID uuid, String questId) {
        KoraQuestService current = manager;
        return current == null || uuid == null ? "UNAVAILABLE" : current.status(uuid, questId);
    }

    public static boolean acceptQuest(UUID uuid, String questId) {
        Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
        return acceptQuest(player, questId);
    }
    public static boolean acceptQuest(Player player, String questId) {
        KoraQuestService current = manager;
        return current != null && player != null && current.accept(player, questId);
    }

    public static boolean cancelQuest(UUID uuid, String questId) {
        Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
        return cancelQuest(player, questId);
    }
    public static boolean cancelQuest(Player player, String questId) {
        KoraQuestService current = manager;
        return current != null && player != null && current.cancel(player, questId);
    }

    public static boolean completeQuest(UUID uuid, String questId) {
        Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
        return completeQuest(player, questId);
    }
    public static boolean completeQuest(Player player, String questId) {
        KoraQuestService current = manager;
        return current != null && player != null && current.complete(player, questId);
    }

    public static boolean rerollQuest(Player player, String questId) {
        KoraQuestService current = manager;
        return current != null && player != null && current.reroll(player, questId);
    }

    public static boolean progressQuest(Player player, String questId, int amount) {
        KoraQuestService current = manager;
        return current != null && player != null && current.progressById(player, questId, amount);
    }

    public static void progress(Player player, ObjectiveType type, String target, int amount) {
        KoraQuestService current = manager;
        if (current != null && player != null && type != null) current.increment(player, type, target, amount);
    }

    public static void progressCustom(Player player, String target, int amount) { progress(player, ObjectiveType.CUSTOM, target, amount); }

    public static void registerObjectiveProvider(String id, ObjectiveProvider provider) {
        if (valid(id) && provider != null) objectiveProviders.put(normalize(id), provider);
    }
    public static void unregisterObjectiveProvider(String id) { if (id != null) objectiveProviders.remove(normalize(id)); }

    /** Dispatches progress to a registered custom provider. The provider decides how that progress is validated and forwarded. */
    public static boolean dispatchObjectiveProvider(String id, Player player, String target, int amount) {
        ObjectiveProvider provider = id == null ? null : objectiveProviders.get(normalize(id));
        if (provider == null || player == null || amount <= 0) return false;
        provider.progress(player, target == null ? "ANY" : target, amount);
        return true;
    }

    public static void registerRewardProvider(String id, RewardProvider provider) {
        if (valid(id) && provider != null) rewardProviders.put(normalize(id), provider);
    }
    public static void unregisterRewardProvider(String id) { if (id != null) rewardProviders.remove(normalize(id)); }
    public static Optional<ObjectiveProvider> objectiveProvider(String id) { return Optional.ofNullable(id == null ? null : objectiveProviders.get(normalize(id))); }
    public static Optional<RewardProvider> rewardProvider(String id) { return Optional.ofNullable(id == null ? null : rewardProviders.get(normalize(id))); }

    private static boolean valid(String id) { return id != null && id.matches("[A-Za-z0-9_-]{2,64}"); }
    private static String normalize(String id) { return id.toLowerCase(java.util.Locale.ROOT); }

    @FunctionalInterface
    public interface ObjectiveProvider { void progress(Player player, String target, int amount); }
    @FunctionalInterface
    public interface RewardProvider { boolean deliver(Player player, QuestDefinition quest, String value); }
}
