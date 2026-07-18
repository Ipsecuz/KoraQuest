package dev.ipseucz.koraquest.model;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

public record QuestDefinition(
        String id,
        String cycle,
        Difficulty difficulty,
        ObjectiveType type,
        String target,
        int required,
        Material material,
        String title,
        List<String> lore,
        List<String> requirements,
        List<String> rewards,
        QuestFilters filters,
        boolean rerollable,
        String permission,
        long cooldownSeconds,
        int seasonXp,
        String completionSound,
        String completionParticle
) {
    public QuestDefinition {
        cycle = cycle == null || cycle.isBlank() ? "daily" : cycle.toLowerCase(Locale.ROOT);
        lore = lore == null ? List.of() : List.copyOf(lore);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
        filters = filters == null ? QuestFilters.NONE : filters;
        permission = permission == null ? "" : permission.trim();
        cooldownSeconds = Math.max(0L, cooldownSeconds);
        seasonXp = Math.max(0, seasonXp);
        completionSound = completionSound == null ? "" : completionSound.trim();
        completionParticle = completionParticle == null ? "" : completionParticle.trim();
    }

    public QuestDefinition(String id, String cycle, Difficulty difficulty, ObjectiveType type, String target,
                           int required, Material material, String title, List<String> lore, List<String> rewards,
                           QuestFilters filters, boolean rerollable) {
        this(id, cycle, difficulty, type, target, required, material, title, lore, List.of(), rewards,
                filters, rerollable, "", 0L, 0, "", "");
    }
}
