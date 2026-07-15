package dev.ipseucz.koraquest.data;

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
    }
}
