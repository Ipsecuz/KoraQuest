package dev.ipseucz.koraquest.placeholder;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Isolates all direct PlaceholderAPI linkage from the plugin main class.
 * This class is loaded reflectively only after PlaceholderAPI is confirmed enabled,
 * allowing KoraQuest to start normally when the optional dependency is absent.
 */
public final class PlaceholderBridge {
    private PlaceholderBridge() { }

    public static Object register(KoraQuestPlugin plugin, QuestManager manager) {
        KoraQuestExpansion expansion = new KoraQuestExpansion(plugin, manager);
        if (!expansion.register()) {
            throw new IllegalStateException("PlaceholderAPI rejected the KoraQuest expansion registration");
        }
        return expansion;
    }

    public static void unregister(Object expansion) {
        if (expansion instanceof KoraQuestExpansion koraQuestExpansion) {
            koraQuestExpansion.unregister();
        }
    }

    public static String apply(Player player, String input) {
        return PlaceholderAPI.setPlaceholders(player, input);
    }
}
