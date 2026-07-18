package dev.ipseucz.koraquest.data;

import java.util.List;
import java.util.Map;

public record PlayerQuestSnapshot(
        List<QuestProgressEntry> entries,
        PlayerProfile profile,
        Map<String, Integer> rerollUses,
        List<RerollRecord> rerolls
) {
    public PlayerQuestSnapshot {
        entries = entries == null ? List.of() : List.copyOf(entries);
        profile = profile == null ? PlayerProfile.EMPTY : profile;
        rerollUses = rerollUses == null ? Map.of() : Map.copyOf(rerollUses);
        rerolls = rerolls == null ? List.of() : List.copyOf(rerolls);
    }
}
