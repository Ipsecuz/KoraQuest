package dev.ipseucz.koraquest.cycle;

import dev.ipseucz.koraquest.model.Difficulty;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record CycleState(
        String name,
        String instanceId,
        long startedAt,
        long nextResetAt,
        Map<Difficulty, List<String>> selectedQuests
) {
    public CycleState {
        name = CycleSettings.normalize(name);
        instanceId = instanceId == null || instanceId.isBlank() ? name + ":" + startedAt : instanceId;
        Map<Difficulty, List<String>> copy = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            copy.put(difficulty, List.copyOf(selectedQuests == null ? List.of() : selectedQuests.getOrDefault(difficulty, List.of())));
        }
        selectedQuests = Map.copyOf(copy);
    }

    public List<String> questIds(Difficulty difficulty) {
        return selectedQuests.getOrDefault(difficulty, List.of());
    }

    public boolean contains(String questId) {
        if (questId == null) return false;
        return selectedQuests.values().stream().anyMatch(ids -> ids.contains(questId));
    }
}
