package dev.ipseucz.koraquest.data;

<<<<<<< HEAD
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
=======
import java.util.Map;
import java.util.Set;

public record PlayerQuestSnapshot(Set<String> active, Set<String> completed, Map<String, Integer> progress) {
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
}
