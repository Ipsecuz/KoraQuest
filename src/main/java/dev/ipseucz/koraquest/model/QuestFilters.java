package dev.ipseucz.koraquest.model;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record QuestFilters(
        Set<String> worlds,
        int minY,
        int maxY,
        String weapon,
        Set<String> spawnReasons,
        boolean requireCriticalHit,
        Set<String> regions,
        boolean requireOwnIsland,
        boolean requireOwnPlot,
        String location,
        double radius
) {
    public static final QuestFilters NONE = new QuestFilters(Set.of(), Integer.MIN_VALUE, Integer.MAX_VALUE,
            "ANY", Set.of(), false, Set.of(), false, false, "", 3.0D);

    public QuestFilters {
        worlds = normalize(worlds);
        weapon = weapon == null || weapon.isBlank() ? "ANY" : weapon.trim().toUpperCase(Locale.ROOT);
        spawnReasons = normalize(spawnReasons);
        regions = normalize(regions);
        location = location == null ? "" : location.trim();
        radius = Math.max(0.1D, radius);
        if (minY > maxY) {
            int swap = minY;
            minY = maxY;
            maxY = swap;
        }
    }

    public static QuestFilters of(List<String> worlds, int minY, int maxY, String weapon,
                                  List<String> spawnReasons, boolean requireCriticalHit) {
        return new QuestFilters(Set.copyOf(worlds == null ? List.of() : worlds), minY, maxY, weapon,
                Set.copyOf(spawnReasons == null ? List.of() : spawnReasons), requireCriticalHit,
                Set.of(), false, false, "", 3.0D);
    }

    public static QuestFilters of(List<String> worlds, int minY, int maxY, String weapon,
                                  List<String> spawnReasons, boolean requireCriticalHit, List<String> regions,
                                  boolean requireOwnIsland, boolean requireOwnPlot, String location, double radius) {
        return new QuestFilters(Set.copyOf(worlds == null ? List.of() : worlds), minY, maxY, weapon,
                Set.copyOf(spawnReasons == null ? List.of() : spawnReasons), requireCriticalHit,
                Set.copyOf(regions == null ? List.of() : regions), requireOwnIsland, requireOwnPlot, location, radius);
    }

    private static Set<String> normalize(Set<String> input) {
        return input == null ? Set.of() : input.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
