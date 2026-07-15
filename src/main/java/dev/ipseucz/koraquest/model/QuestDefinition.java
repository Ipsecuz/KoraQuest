package dev.ipseucz.koraquest.model;

import org.bukkit.Material;

import java.util.List;

public record QuestDefinition(
        String id,
        Difficulty difficulty,
        ObjectiveType type,
        String target,
        int required,
        Material material,
        String title,
        List<String> lore,
        List<String> rewards
) {
    public QuestDefinition {
        lore = lore == null ? List.of() : List.copyOf(lore);
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
    }
}
