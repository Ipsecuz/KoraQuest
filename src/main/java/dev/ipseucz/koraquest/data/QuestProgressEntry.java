package dev.ipseucz.koraquest.data;

public record QuestProgressEntry(
        String cycleName,
        String cycleId,
        String questId,
        Status status,
        int progress,
        long acceptedAt,
        long completedAt
) {
    public QuestProgressEntry {
        cycleName = cycleName == null ? "daily" : cycleName;
        cycleId = cycleId == null ? cycleName + ":legacy" : cycleId;
        questId = questId == null ? "" : questId;
        status = status == null ? Status.ACTIVE : status;
        progress = Math.max(0, progress);
    }

    public boolean active() { return status == Status.ACTIVE; }
    public boolean completed() { return status == Status.COMPLETED; }

    public enum Status { ACTIVE, COMPLETED }
}
