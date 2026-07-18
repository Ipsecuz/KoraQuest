package dev.ipseucz.koraquest.placeholder;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.cycle.CycleSettings;
import dev.ipseucz.koraquest.data.PlayerProfile;
import dev.ipseucz.koraquest.data.PlayerQuestData;
import dev.ipseucz.koraquest.model.QuestDefinition;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

public final class KoraQuestExpansion extends PlaceholderExpansion {
    private final KoraQuestPlugin plugin;
    private final QuestManager manager;

    public KoraQuestExpansion(KoraQuestPlugin plugin, QuestManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public @NotNull String getIdentifier() { return "koraquest"; }
    @Override public @NotNull String getAuthor() { return "Ipsecuz_"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        UUID uuid = player == null ? null : player.getUniqueId();
        // Placeholder requests can run very frequently. Never block their thread on database I/O.
        PlayerQuestData data = uuid == null ? null : manager.playerDataService().cachedPlayer(uuid);
        PlayerProfile profile = data == null ? PlayerProfile.EMPTY : data.profile();

        if (key.equals("active_count")) return data == null ? "0" : String.valueOf(data.activeCount());
        if (key.equals("completed_daily")) return String.valueOf(completed("daily", data));
        if (key.equals("daily_total")) return String.valueOf(manager.quests("daily").size());
        if (key.equals("daily_remaining")) return String.valueOf(Math.max(0, manager.quests("daily").size() - completed("daily", data)));
        if (key.equals("reset_time")) return manager.resetTimeText("daily");
        if (key.equals("streak")) return String.valueOf(profile.dailyStreak());
        if (key.equals("best_streak")) return String.valueOf(profile.bestDailyStreak());
        if (key.equals("perfect_weeks")) return String.valueOf(profile.perfectWeeks());
        if (key.equals("catchup_tokens")) return String.valueOf(profile.catchupTokens());
        if (key.equals("season_level")) return String.valueOf(profile.seasonLevel());
        if (key.equals("season_xp")) return String.valueOf(profile.seasonXp());
        if (key.equals("season_next_level_xp")) return String.valueOf((profile.seasonLevel() + 1L) * plugin.questConfig().seasonXpPerLevel());

        String cycleValue = cyclePlaceholder(key, uuid, data);
        if (cycleValue != null) return cycleValue;
        return questPlaceholder(key, data);
    }

    private String cyclePlaceholder(String key, UUID uuid, PlayerQuestData data) {
        for (CycleSettings cycle : plugin.questConfig().cycles().values()) {
            String prefix = cycle.name().toLowerCase(Locale.ROOT) + "_";
            if (!key.startsWith(prefix)) continue;
            String suffix = key.substring(prefix.length());
            return switch (suffix) {
                case "total" -> String.valueOf(manager.quests(cycle.name()).size());
                case "completed" -> String.valueOf(completed(cycle.name(), data));
                case "remaining" -> String.valueOf(Math.max(0, manager.quests(cycle.name()).size() - completed(cycle.name(), data)));
                case "active" -> data == null ? "0" : String.valueOf(data.activeCount(cycle.name()));
                case "reset_time" -> manager.resetTimeText(cycle.name());
                case "cycle_id" -> manager.playerDataService().cycleId(cycle.name());
                default -> null;
            };
        }
        return null;
    }

    private String questPlaceholder(String key, PlayerQuestData data) {
        if (!key.startsWith("quest_")) return null;
        String body = key.substring("quest_".length());
        String[] suffixes = {"_progress", "progress", "_required", "required", "_percent", "percent", "_status", "status", "_cycle", "cycle"};
        for (String suffix : suffixes) {
            if (!body.endsWith(suffix)) continue;
            String id = body.substring(0, body.length() - suffix.length());
            while (id.endsWith("_")) id = id.substring(0, id.length() - 1);
            QuestDefinition quest = manager.getQuest(id).orElse(null);
            if (quest == null) return suffix.contains("status") ? "UNAVAILABLE" : "0";
            dev.ipseucz.koraquest.cycle.CycleState state = manager.playerDataService().cycle(quest.cycle());
            int progress = data == null || state == null ? 0 : data.progressOf(state.instanceId(), quest.id());
            if (suffix.contains("progress")) return String.valueOf(progress);
            if (suffix.contains("required")) return String.valueOf(quest.required());
            if (suffix.contains("percent")) return String.valueOf(Math.min(100, (int) Math.floor(progress * 100D / Math.max(1, quest.required()))));
            if (suffix.contains("status")) return status(data, state, quest, progress);
            if (suffix.contains("cycle")) return quest.cycle();
        }
        return null;
    }
    private int completed(String cycleName, PlayerQuestData data) {
        dev.ipseucz.koraquest.cycle.CycleState state = manager.playerDataService().cycle(cycleName);
        if (data == null || state == null) return 0;
        int count = 0;
        for (QuestDefinition quest : manager.quests(cycleName)) {
            if (data.isCompleted(state.instanceId(), quest.id())) count++;
        }
        return count;
    }

    private String status(PlayerQuestData data, dev.ipseucz.koraquest.cycle.CycleState state,
                          QuestDefinition quest, int progress) {
        if (data == null || state == null) return "UNAVAILABLE";
        if (data.isCompleted(state.instanceId(), quest.id())) return "COMPLETED";
        if (data.isActive(state.instanceId(), quest.id()) && progress >= quest.required()) return "READY";
        if (data.isActive(state.instanceId(), quest.id())) return "ACTIVE";
        return "AVAILABLE";
    }

}
