package dev.ipseucz.koraquest.api;

import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.model.ObjectiveType;
import org.bukkit.entity.Player;

/**
 * Lightweight API for quest types that are not covered by Bukkit events.
 * Calls must be made from the player's owning thread on Folia.
 */
public final class KoraQuestAPI {
    private static volatile QuestManager manager;

    private KoraQuestAPI() {
    }

    public static void bootstrap(QuestManager questManager) {
        manager = questManager;
    }

    public static void shutdown() {
        manager = null;
    }

    public static boolean progressQuest(Player player, String questId, int amount) {
        QuestManager current = manager;
        return current != null && player != null && current.progressById(player, questId, amount);
    }

    public static void progressCustom(Player player, String target, int amount) {
        QuestManager current = manager;
        if (current != null && player != null) {
            current.increment(player, ObjectiveType.CUSTOM, target, amount);
        }
    }
}
