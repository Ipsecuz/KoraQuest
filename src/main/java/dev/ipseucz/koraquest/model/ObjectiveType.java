package dev.ipseucz.koraquest.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Locale;

public enum ObjectiveType {
    KILL(TargetKind.ENTITY),
    BREAK(TargetKind.MATERIAL),
    PLACE(TargetKind.MATERIAL),
    CRAFT(TargetKind.MATERIAL),
    FISH(TargetKind.MIXED),
    ENCHANT(TargetKind.MATERIAL),
    BREED(TargetKind.ENTITY),
    TAME(TargetKind.ENTITY),
    CONSUME(TargetKind.MATERIAL),
    SMELT(TargetKind.MATERIAL),
    SHEAR(TargetKind.ENTITY),
    CUSTOM(TargetKind.FREE);

    private final TargetKind targetKind;

    ObjectiveType(TargetKind targetKind) {
        this.targetKind = targetKind;
    }

    public static ObjectiveType from(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Objective type is missing");
        }
        return valueOf(input.trim().toUpperCase(Locale.ROOT));
    }

    public boolean validTarget(String target) {
        if (target == null || target.isBlank() || target.equalsIgnoreCase("ANY")) {
            return true;
        }
        String normalized = target.trim().toUpperCase(Locale.ROOT);
        return switch (targetKind) {
            case MATERIAL -> Material.matchMaterial(normalized) != null;
            case ENTITY -> entityExists(normalized);
            case MIXED -> Material.matchMaterial(normalized) != null || entityExists(normalized);
            case FREE -> true;
        };
    }

    public String normalizeTarget(String target) {
        if (target == null || target.isBlank()) {
            return "ANY";
        }
        return target.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean entityExists(String target) {
        try {
            EntityType.valueOf(target);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private enum TargetKind {
        MATERIAL,
        ENTITY,
        MIXED,
        FREE
    }
}
