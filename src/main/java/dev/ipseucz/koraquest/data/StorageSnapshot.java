package dev.ipseucz.koraquest.data;

<<<<<<< HEAD
import dev.ipseucz.koraquest.cycle.CycleState;

import java.util.Map;

public record StorageSnapshot(Map<String, CycleState> cycles) {
    public StorageSnapshot {
        cycles = cycles == null ? Map.of() : Map.copyOf(cycles);
    }

    public boolean isEmpty() {
        return cycles.isEmpty();
=======
import dev.ipseucz.koraquest.model.Difficulty;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StorageSnapshot(long cycleStartedAt, Map<Difficulty, List<String>> dailyQuests,
                              Map<UUID, PlayerQuestData> players) {
    public boolean isEmpty() {
        if (cycleStartedAt > 0L || !players.isEmpty()) {
            return false;
        }
        return dailyQuests.values().stream().allMatch(List::isEmpty);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }
}
