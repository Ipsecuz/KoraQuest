package dev.ipseucz.koraquest.data;

public record PlayerProfile(
        int dailyStreak,
        int bestDailyStreak,
        String lastDailyCycleId,
        long lastDailyCompletedAt,
        int perfectWeeks,
        int catchupTokens,
        String seasonCycleId,
        int seasonLevel,
        long seasonXp
) {
    public static final PlayerProfile EMPTY = new PlayerProfile(0, 0, "", 0L, 0, 0, "", 0, 0L);

    public PlayerProfile {
        dailyStreak = Math.max(0, dailyStreak);
        bestDailyStreak = Math.max(dailyStreak, bestDailyStreak);
        lastDailyCycleId = lastDailyCycleId == null ? "" : lastDailyCycleId;
        perfectWeeks = Math.max(0, perfectWeeks);
        catchupTokens = Math.max(0, catchupTokens);
        seasonCycleId = seasonCycleId == null ? "" : seasonCycleId;
        seasonLevel = Math.max(0, seasonLevel);
        seasonXp = Math.max(0L, seasonXp);
    }

    public PlayerProfile withStreak(int streak, String cycleId, long completedAt, int perfectWeekDelta, int tokenDelta) {
        int safeStreak = Math.max(0, streak);
        return new PlayerProfile(safeStreak, Math.max(bestDailyStreak, safeStreak), cycleId, completedAt,
                perfectWeeks + Math.max(0, perfectWeekDelta), Math.max(0, catchupTokens + tokenDelta),
                seasonCycleId, seasonLevel, seasonXp);
    }

    public PlayerProfile alignSeason(String cycleId) {
        String safeCycleId = cycleId == null ? "" : cycleId;
        if (safeCycleId.isBlank() || safeCycleId.equals(seasonCycleId)) return this;
        return new PlayerProfile(dailyStreak, bestDailyStreak, lastDailyCycleId, lastDailyCompletedAt,
                perfectWeeks, catchupTokens, safeCycleId, 0, 0L);
    }

    public PlayerProfile withSeason(String cycleId, long xp, int level) {
        String safeCycleId = cycleId == null ? "" : cycleId;
        return new PlayerProfile(dailyStreak, bestDailyStreak, lastDailyCycleId, lastDailyCompletedAt,
                perfectWeeks, catchupTokens, safeCycleId, Math.max(0, level), Math.max(0L, xp));
    }

    public PlayerProfile useCatchupToken() {
        return new PlayerProfile(dailyStreak, bestDailyStreak, lastDailyCycleId, lastDailyCompletedAt,
                perfectWeeks, Math.max(0, catchupTokens - 1), seasonCycleId, seasonLevel, seasonXp);
    }
}
