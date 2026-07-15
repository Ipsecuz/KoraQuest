package dev.ipseucz.koraquest.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlayerQuestData {
    private final Set<String> active = new HashSet<>();
    private final Set<String> completed = new HashSet<>();
    private final Map<String, Integer> progress = new HashMap<>();

    public synchronized boolean isActive(String id) {
        return active.contains(id);
    }

    public synchronized boolean isCompleted(String id) {
        return completed.contains(id);
    }

    public synchronized int progressOf(String id) {
        return progress.getOrDefault(id, 0);
    }

    public synchronized int activeCount() {
        return active.size();
    }

    public synchronized List<String> activeIds() {
        return List.copyOf(active);
    }

    public synchronized AcceptResult accept(String id, int limit) {
        if (completed.contains(id)) {
            return AcceptResult.COMPLETED;
        }
        if (active.contains(id)) {
            return AcceptResult.ALREADY_ACTIVE;
        }
        if (active.size() >= limit) {
            return AcceptResult.LIMIT;
        }
        active.add(id);
        progress.put(id, 0);
        return AcceptResult.ACCEPTED;
    }

    public synchronized boolean cancel(String id) {
        if (!active.remove(id)) {
            return false;
        }
        progress.remove(id);
        return true;
    }

    public synchronized CompleteResult complete(String id, int required) {
        if (completed.contains(id)) {
            return CompleteResult.ALREADY_COMPLETED;
        }
        if (!active.contains(id)) {
            return CompleteResult.NOT_ACTIVE;
        }
        if (progress.getOrDefault(id, 0) < required) {
            return CompleteResult.NOT_READY;
        }
        active.remove(id);
        progress.remove(id);
        completed.add(id);
        return CompleteResult.COMPLETED;
    }

    public synchronized ProgressUpdate increment(String id, int amount, int required) {
        if (!active.contains(id) || amount <= 0) {
            int current = progress.getOrDefault(id, 0);
            return new ProgressUpdate(current, current, false, false);
        }
        int old = progress.getOrDefault(id, 0);
        int updated = Math.min(required, Math.max(0, old + amount));
        if (updated == old) {
            return new ProgressUpdate(old, updated, false, false);
        }
        progress.put(id, updated);
        return new ProgressUpdate(old, updated, true, old < required && updated >= required);
    }

    public synchronized PlayerQuestSnapshot snapshot() {
        return new PlayerQuestSnapshot(Set.copyOf(active), Set.copyOf(completed), Map.copyOf(progress));
    }

    public synchronized void loadActive(String id, int value) {
        active.add(id);
        progress.put(id, Math.max(0, value));
    }

    public synchronized void loadCompleted(String id) {
        completed.add(id);
        active.remove(id);
        progress.remove(id);
    }

    public synchronized void removeQuest(String id) {
        active.remove(id);
        completed.remove(id);
        progress.remove(id);
    }

    public enum AcceptResult {
        ACCEPTED,
        ALREADY_ACTIVE,
        COMPLETED,
        LIMIT
    }

    public enum CompleteResult {
        COMPLETED,
        NOT_ACTIVE,
        NOT_READY,
        ALREADY_COMPLETED
    }

    public record ProgressUpdate(int oldProgress, int progress, boolean changed, boolean becameReady) {
    }
}
