package dev.ipseucz.koraquest.api;

import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal service contract exposed through the lightweight API artifact.
 * Implementations belong to the full KoraQuest plugin; API consumers only
 * depend on this interface and the public model classes.
 */
public interface KoraQuestService {
    Optional<QuestDefinition> getQuest(String id);
    Collection<QuestDefinition> getActiveQuests(UUID uuid);
    int progress(UUID uuid, String questId);
    String status(UUID uuid, String questId);
    boolean accept(Player player, String questId);
    boolean cancel(Player player, String questId);
    boolean complete(Player player, String questId);
    boolean reroll(Player player, String questId);
    boolean progressById(Player player, String questId, int amount);
    void increment(Player player, ObjectiveType type, String target, int amount);
}
