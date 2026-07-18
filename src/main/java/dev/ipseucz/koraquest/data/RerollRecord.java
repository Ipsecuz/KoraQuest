package dev.ipseucz.koraquest.data;

public record RerollRecord(String cycleId, String originalQuestId, String replacementQuestId, long createdAt) {
}
