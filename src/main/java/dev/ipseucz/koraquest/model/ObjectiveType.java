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
    PLAYTIME(TargetKind.FREE),
    WALK(TargetKind.FREE),
    SPRINT(TargetKind.FREE),
    SWIM(TargetKind.FREE),
    FLY(TargetKind.FREE),
    GLIDE(TargetKind.FREE),
    JUMP(TargetKind.FREE),
    CHAT(TargetKind.FREE),
    COMMAND(TargetKind.FREE),
    LOGIN(TargetKind.FREE),
    VOTE(TargetKind.FREE),
    ITEM_PICKUP(TargetKind.MATERIAL),
    ITEM_DROP(TargetKind.MATERIAL),
    ITEM_SUBMIT(TargetKind.MATERIAL),
    INTERACT_BLOCK(TargetKind.MATERIAL),
    INTERACT_ENTITY(TargetKind.ENTITY),
    DAMAGE_DEALT(TargetKind.ENTITY),
    DAMAGE_TAKEN(TargetKind.FREE),
    HEAL(TargetKind.FREE),
    DIE(TargetKind.FREE),
    VISIT_LOCATION(TargetKind.FREE),
    ENTER_WORLD(TargetKind.FREE),
    ENTER_REGION(TargetKind.FREE),
    EXP_GAIN(TargetKind.FREE),
    LEVEL_GAIN(TargetKind.FREE),
    MONEY_EARN(TargetKind.FREE),
    MONEY_SPEND(TargetKind.FREE),
    POTION_BREW(TargetKind.MATERIAL),
    BUCKET_FILL(TargetKind.MATERIAL),
    TRADE_VILLAGER(TargetKind.MATERIAL),
    REPAIR(TargetKind.MATERIAL),
    ANVIL(TargetKind.MATERIAL),
    SMITHING(TargetKind.MATERIAL),
    STONECUT(TargetKind.MATERIAL),
    LOOT_CHEST(TargetKind.MATERIAL),
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
        boolean externalId = normalized.matches("[A-Z0-9_.:-]{2,128}") && (normalized.contains(":")
                || normalized.startsWith("MYTHIC_") || normalized.startsWith("CITIZENS_"));
        return switch (targetKind) {
            case MATERIAL -> Material.matchMaterial(normalized) != null || externalId;
            case ENTITY -> entityExists(normalized) || externalId;
            case MIXED -> Material.matchMaterial(normalized) != null || entityExists(normalized) || externalId;
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
