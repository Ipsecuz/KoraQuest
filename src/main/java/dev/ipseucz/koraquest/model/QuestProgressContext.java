package dev.ipseucz.koraquest.model;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;

public record QuestProgressContext(
        String world,
        int y,
        String weapon,
        String spawnReason,
        boolean criticalHit
) {
    public static QuestProgressContext from(Player player) {
        Material held = player.getInventory().getItemInMainHand().getType();
        return new QuestProgressContext(
                player.getWorld().getName().toUpperCase(Locale.ROOT),
                player.getLocation().getBlockY(),
                held.name(),
                "",
                false
        );
    }

    public QuestProgressContext {
        world = normalize(world);
        weapon = normalize(weapon);
        spawnReason = normalize(spawnReason);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
