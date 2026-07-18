package dev.ipseucz.koraquest.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PlayerQuestData {
    private final Map<String, QuestProgressEntry> entries = new LinkedHashMap<>();
    private final Map<String, Integer> rerollUses = new HashMap<>();
    private final List<RerollRecord> rerolls = new ArrayList<>();
    private PlayerProfile profile = PlayerProfile.EMPTY;

    private String key(String cycleId, String questId) {
        return cycleId + "\u0000" + questId;
    }

    public synchronized boolean isActive(String cycleId, String questId) {
        QuestProgressEntry entry = entries.get(key(cycleId, questId));
        return entry != null && entry.active();
    }

    public synchronized boolean isCompleted(String cycleId, String questId) {
        QuestProgressEntry entry = entries.get(key(cycleId, questId));
        return entry != null && entry.completed();
    }

    public synchronized int progressOf(String cycleId, String questId) {
        QuestProgressEntry entry = entries.get(key(cycleId, questId));
        return entry == null ? 0 : entry.progress();
    }

    /** Compatibility lookup for API callers that only know a quest id. */
    public synchronized boolean isActive(String questId) {
        return entries.values().stream().anyMatch(entry -> entry.questId().equalsIgnoreCase(questId) && entry.active());
    }

    public synchronized boolean isCompleted(String questId) {
        return entries.values().stream().anyMatch(entry -> entry.questId().equalsIgnoreCase(questId) && entry.completed());
    }

    public synchronized int progressOf(String questId) {
        return entries.values().stream().filter(entry -> entry.questId().equalsIgnoreCase(questId))
                .mapToInt(QuestProgressEntry::progress).max().orElse(0);
    }

    public synchronized int activeCount() {
        return (int) entries.values().stream().filter(QuestProgressEntry::active).count();
    }

    public synchronized int activeCount(String cycleName) {
        return (int) entries.values().stream().filter(QuestProgressEntry::active)
                .filter(entry -> entry.cycleName().equalsIgnoreCase(cycleName)).count();
    }

    public synchronized int completedCount(String cycleId) {
        return (int) entries.values().stream().filter(QuestProgressEntry::completed)
                .filter(entry -> entry.cycleId().equals(cycleId)).count();
    }

    public synchronized boolean isReady(String cycleId, String questId, int required) {
        return isActive(cycleId, questId) && progressOf(cycleId, questId) >= required;
    }

    public synchronized List<QuestProgressEntry> entries() {
        return List.copyOf(entries.values());
    }

    public synchronized List<QuestProgressEntry> activeEntries() {
        return entries.values().stream().filter(QuestProgressEntry::active).toList();
    }

    public synchronized List<String> activeIds() {
        return entries.values().stream().filter(QuestProgressEntry::active).map(QuestProgressEntry::questId).distinct().toList();
    }

    public synchronized Optional<QuestProgressEntry> entry(String cycleId, String questId) {
        return Optional.ofNullable(entries.get(key(cycleId, questId)));
    }

    public synchronized AcceptResult accept(String cycleName, String cycleId, String questId, int globalLimit, int cycleLimit) {
        QuestProgressEntry existing = entries.get(key(cycleId, questId));
        if (existing != null && existing.completed()) return AcceptResult.COMPLETED;
        if (existing != null && existing.active()) return AcceptResult.ALREADY_ACTIVE;
        if (activeCount() >= globalLimit || activeCount(cycleName) >= cycleLimit) return AcceptResult.LIMIT;
        long now = System.currentTimeMillis();
        entries.put(key(cycleId, questId), new QuestProgressEntry(cycleName, cycleId, questId,
                QuestProgressEntry.Status.ACTIVE, 0, now, 0L));
        return AcceptResult.ACCEPTED;
    }

    public synchronized boolean cancel(String cycleId, String questId) {
        QuestProgressEntry existing = entries.get(key(cycleId, questId));
        if (existing == null || !existing.active()) return false;
        entries.remove(key(cycleId, questId));
        return true;
    }

    public synchronized CompleteResult complete(String cycleId, String questId, int required) {
        QuestProgressEntry existing = entries.get(key(cycleId, questId));
        if (existing == null) return CompleteResult.NOT_ACTIVE;
        if (existing.completed()) return CompleteResult.ALREADY_COMPLETED;
        if (existing.progress() < required) return CompleteResult.NOT_READY;
        entries.put(key(cycleId, questId), new QuestProgressEntry(existing.cycleName(), cycleId, questId,
                QuestProgressEntry.Status.COMPLETED, 0, existing.acceptedAt(), System.currentTimeMillis()));
        return CompleteResult.COMPLETED;
    }

    public synchronized ProgressUpdate increment(String cycleId, String questId, int amount, int required) {
        QuestProgressEntry existing = entries.get(key(cycleId, questId));
        if (existing == null || !existing.active() || amount <= 0) {
            int current = existing == null ? 0 : existing.progress();
            return new ProgressUpdate(current, current, false, false);
        }
        int old = existing.progress();
        int updated = Math.min(required, Math.max(0, old + amount));
        if (updated == old) return new ProgressUpdate(old, updated, false, false);
        entries.put(key(cycleId, questId), new QuestProgressEntry(existing.cycleName(), existing.cycleId(), existing.questId(),
                existing.status(), updated, existing.acceptedAt(), existing.completedAt()));
        return new ProgressUpdate(old, updated, true, old < required && updated >= required);
    }

    public synchronized PlayerQuestSnapshot snapshot() {
        return new PlayerQuestSnapshot(List.copyOf(entries.values()), profile, Map.copyOf(rerollUses), List.copyOf(rerolls));
    }

    public synchronized void loadEntry(QuestProgressEntry entry) {
        entries.put(key(entry.cycleId(), entry.questId()), entry);
    }

    public synchronized void loadProfile(PlayerProfile loaded) {
        profile = loaded == null ? PlayerProfile.EMPTY : loaded;
    }

    public synchronized PlayerProfile profile() {
        return profile;
    }

    public synchronized void setProfile(PlayerProfile updated) {
        profile = updated == null ? PlayerProfile.EMPTY : updated;
    }

    public synchronized int rerollUses(String cycleId) {
        return rerollUses.getOrDefault(cycleId, 0);
    }

    public synchronized int incrementRerollUses(String cycleId) {
        int next = rerollUses.getOrDefault(cycleId, 0) + 1;
        rerollUses.put(cycleId, next);
        return next;
    }

    public synchronized void loadRerollUse(String cycleId, int uses) {
        rerollUses.put(cycleId, Math.max(0, uses));
    }

    public synchronized void addReroll(RerollRecord record) {
        rerolls.removeIf(existing -> existing.cycleId().equals(record.cycleId())
                && existing.originalQuestId().equalsIgnoreCase(record.originalQuestId()));
        rerolls.add(record);
    }

    public synchronized void loadReroll(RerollRecord record) {
        addReroll(record);
    }

    public synchronized List<RerollRecord> rerolls(String cycleId) {
        return rerolls.stream().filter(record -> record.cycleId().equals(cycleId)).toList();
    }

    public synchronized String replacementFor(String cycleId, String originalQuestId) {
        return rerolls.stream().filter(record -> record.cycleId().equals(cycleId))
                .filter(record -> record.originalQuestId().equalsIgnoreCase(originalQuestId))
                .map(RerollRecord::replacementQuestId).findFirst().orElse(null);
    }

    public synchronized boolean wasRerolledTo(String cycleId, String questId) {
        return rerolls.stream().anyMatch(record -> record.cycleId().equals(cycleId)
                && record.replacementQuestId().equalsIgnoreCase(questId));
    }

    public synchronized void markCompleted(String cycleName, String cycleId, String questId) {
        QuestProgressEntry existing = entries.get(key(cycleId, questId));
        long acceptedAt = existing == null ? System.currentTimeMillis() : existing.acceptedAt();
        entries.put(key(cycleId, questId), new QuestProgressEntry(cycleName, cycleId, questId,
                QuestProgressEntry.Status.COMPLETED, 0, acceptedAt, System.currentTimeMillis()));
    }

    public synchronized void removeQuest(String questId) {
        entries.entrySet().removeIf(entry -> entry.getValue().questId().equalsIgnoreCase(questId));
        rerolls.removeIf(record -> record.originalQuestId().equalsIgnoreCase(questId)
                || record.replacementQuestId().equalsIgnoreCase(questId));
    }

    public synchronized void removeCycle(String cycleName, String keepCycleId) {
        entries.entrySet().removeIf(entry -> entry.getValue().cycleName().equalsIgnoreCase(cycleName)
                && !entry.getValue().cycleId().equals(keepCycleId));
        rerolls.removeIf(record -> record.cycleId().startsWith(cycleName + ":") && !record.cycleId().equals(keepCycleId));
        rerollUses.keySet().removeIf(id -> id.startsWith(cycleName + ":") && !id.equals(keepCycleId));
    }

    public enum AcceptResult { ACCEPTED, ALREADY_ACTIVE, COMPLETED, LIMIT }
    public enum CompleteResult { COMPLETED, NOT_ACTIVE, NOT_READY, ALREADY_COMPLETED }
    public record ProgressUpdate(int oldProgress, int progress, boolean changed, boolean becameReady) { }
}
