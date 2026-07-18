package dev.ipseucz.koraquest.cycle;

import dev.ipseucz.koraquest.model.Difficulty;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public record CycleSettings(
        String name,
        boolean enabled,
        String permission,
        int activeLimit,
        SelectionMode selectionMode,
        int totalQuests,
        Map<Difficulty, Integer> amounts,
        Map<Difficulty, Integer> weights,
        ResetType resetType,
        String cron,
        ZoneId timezone,
        DayOfWeek weekDay,
        int monthDay,
        LocalTime time,
        long intervalMillis,
        long historyRetentionMillis,
        String displayName
) {
    public CycleSettings {
        name = normalize(name);
        permission = permission == null ? "" : permission.trim();
        activeLimit = Math.max(1, activeLimit);
        selectionMode = selectionMode == null ? SelectionMode.FIXED : selectionMode;
        totalQuests = Math.max(0, totalQuests);
        amounts = immutable(amounts, 0);
        weights = immutable(weights, 0);
        resetType = resetType == null ? ResetType.MANUAL : resetType;
        cron = cron == null ? "" : cron.trim();
        timezone = timezone == null ? ZoneId.systemDefault() : timezone;
        weekDay = weekDay == null ? DayOfWeek.MONDAY : weekDay;
        monthDay = Math.max(1, Math.min(31, monthDay));
        time = time == null ? LocalTime.MIDNIGHT : time;
        intervalMillis = Math.max(60_000L, intervalMillis);
        historyRetentionMillis = Math.max(86_400_000L, historyRetentionMillis);
        displayName = displayName == null || displayName.isBlank() ? name : displayName;
    }

    public int amount(Difficulty difficulty) {
        return Math.max(0, amounts.getOrDefault(difficulty, 0));
    }

    public int weight(Difficulty difficulty) {
        return Math.max(0, weights.getOrDefault(difficulty, 0));
    }

    public static String normalize(String input) {
        return input == null || input.isBlank() ? "daily" : input.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<Difficulty, Integer> immutable(Map<Difficulty, Integer> source, int fallback) {
        Map<Difficulty, Integer> copy = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            copy.put(difficulty, source == null ? fallback : source.getOrDefault(difficulty, fallback));
        }
        return Map.copyOf(copy);
    }

    public enum SelectionMode {
        FIXED,
        WEIGHTED,
        ALL;

        public static SelectionMode parse(String input) {
            try {
                return valueOf(input == null ? "FIXED" : input.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return FIXED;
            }
        }
    }

    public enum ResetType {
        CRON,
        DAILY,
        WEEKLY,
        MONTHLY,
        INTERVAL,
        MANUAL;

        public static ResetType parse(String input) {
            try {
                return valueOf(input == null ? "MANUAL" : input.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return MANUAL;
            }
        }
    }
}
