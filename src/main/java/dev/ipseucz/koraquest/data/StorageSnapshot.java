package dev.ipseucz.koraquest.data;

import dev.ipseucz.koraquest.cycle.CycleState;

import java.util.Map;

public record StorageSnapshot(Map<String, CycleState> cycles) {
    public StorageSnapshot {
        cycles = cycles == null ? Map.of() : Map.copyOf(cycles);
    }

    public boolean isEmpty() {
        return cycles.isEmpty();
    }
}
