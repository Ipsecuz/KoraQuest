package dev.ipseucz.koraquest.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.cycle.CycleState;
import dev.ipseucz.koraquest.data.PlayerProfile;
import dev.ipseucz.koraquest.data.PlayerQuestData;
import dev.ipseucz.koraquest.data.PlayerQuestSnapshot;
import dev.ipseucz.koraquest.data.QuestProgressEntry;
import dev.ipseucz.koraquest.data.RerollRecord;
import dev.ipseucz.koraquest.data.StorageSnapshot;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.RewardClaim;
import dev.ipseucz.koraquest.security.BlockKey;
import dev.ipseucz.koraquest.util.PluginPaths;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class DatabaseStorage {
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final ExecutorService executor;
    private final Dialect dialect;
    private final String prefix;
    private HikariDataSource dataSource;

    public DatabaseStorage(KoraQuestPlugin plugin, QuestConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.dialect = Dialect.parse(config.storageType());
        this.prefix = config.storageTablePrefix();
        int threads = dialect == Dialect.SQLITE ? 1 : Math.max(2, Math.min(8, config.storagePoolSize()));
        this.executor = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "KoraQuest-Database");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void initialize() throws SQLException {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("KoraQuest-Hikari");
        hikari.setJdbcUrl(jdbcUrl());
        int poolSize = dialect == Dialect.SQLITE ? 1 : Math.max(2, config.storagePoolSize());
        hikari.setMaximumPoolSize(poolSize);
        hikari.setMinimumIdle(dialect == Dialect.SQLITE ? 1 : Math.min(2, poolSize));
        hikari.setConnectionTimeout(10_000L);
        hikari.setValidationTimeout(5_000L);
        hikari.setIdleTimeout(600_000L);
        hikari.setMaxLifetime(1_800_000L);
        hikari.setAutoCommit(true);
        if (dialect == Dialect.SQLITE) {
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.addDataSourceProperty("busy_timeout", "5000");
        } else if (dialect == Dialect.MARIADB) {
            hikari.setDriverClassName("org.mariadb.jdbc.Driver");
            hikari.setUsername(config.storageUsername());
            hikari.setPassword(config.storagePassword());
        } else {
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikari.setUsername(config.storageUsername());
            hikari.setPassword(config.storagePassword());
        }
        dataSource = new HikariDataSource(hikari);
        try (Connection connection = dataSource.getConnection()) {
            if (dialect == Dialect.SQLITE) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA journal_mode=WAL");
                    statement.execute("PRAGMA synchronous=NORMAL");
                    statement.execute("PRAGMA foreign_keys=ON");
                    statement.execute("PRAGMA busy_timeout=5000");
                }
            }
            createSchema(connection);
            ensureSchemaColumns(connection);
            migrateV2(connection);
        }
    }

    private String jdbcUrl() {
        return switch (dialect) {
            case SQLITE -> "jdbc:sqlite:" + PluginPaths.databaseFile(plugin).getAbsolutePath();
            case MYSQL -> "jdbc:mysql://" + config.storageHost() + ":" + config.storagePort() + "/" + config.storageDatabase()
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
            case MARIADB -> "jdbc:mariadb://" + config.storageHost() + ":" + config.storagePort() + "/" + config.storageDatabase()
                    + "?useUnicode=true&characterEncoding=utf8";
        };
    }

    private void createSchema(Connection connection) throws SQLException {
        String text = dialect == Dialect.SQLITE ? "TEXT" : "VARCHAR(255)";
        String longText = dialect == Dialect.SQLITE ? "TEXT" : "LONGTEXT";
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("meta") + " (meta_key " + text + " PRIMARY KEY, meta_value " + longText + " NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("cycle_states") + " (cycle_name " + text + " PRIMARY KEY, instance_id " + text + " NOT NULL, started_at BIGINT NOT NULL, next_reset_at BIGINT NOT NULL, updated_at BIGINT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("cycle_quests") + " (cycle_name " + text + " NOT NULL, difficulty " + text + " NOT NULL, position INTEGER NOT NULL, quest_id " + text + " NOT NULL, PRIMARY KEY(cycle_name,difficulty,position))");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("player_quests") + " (uuid " + text + " NOT NULL, cycle_name " + text + " NOT NULL, cycle_id " + text + " NOT NULL, quest_id " + text + " NOT NULL, status " + text + " NOT NULL, progress INTEGER NOT NULL DEFAULT 0, accepted_at BIGINT NOT NULL DEFAULT 0, completed_at BIGINT NOT NULL DEFAULT 0, PRIMARY KEY(uuid,cycle_id,quest_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("player_profiles") + " (uuid " + text + " PRIMARY KEY, daily_streak INTEGER NOT NULL DEFAULT 0, best_daily_streak INTEGER NOT NULL DEFAULT 0, last_daily_cycle_id " + text + " NOT NULL, last_daily_completed_at BIGINT NOT NULL DEFAULT 0, perfect_weeks INTEGER NOT NULL DEFAULT 0, catchup_tokens INTEGER NOT NULL DEFAULT 0, season_cycle_id " + text + " NOT NULL DEFAULT '', season_level INTEGER NOT NULL DEFAULT 0, season_xp BIGINT NOT NULL DEFAULT 0, updated_at BIGINT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("player_rerolls") + " (uuid " + text + " NOT NULL, cycle_id " + text + " NOT NULL, original_quest_id " + text + " NOT NULL, replacement_quest_id " + text + " NOT NULL, created_at BIGINT NOT NULL, PRIMARY KEY(uuid,cycle_id,original_quest_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("reroll_uses") + " (uuid " + text + " NOT NULL, cycle_id " + text + " NOT NULL, uses INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid,cycle_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("pending_rewards") + " (claim_id " + text + " PRIMARY KEY, uuid " + text + " NOT NULL, quest_id " + text + " NOT NULL, cycle_name " + text + " NOT NULL, cycle_id " + text + " NOT NULL, reward_data " + longText + " NOT NULL, status " + text + " NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, next_command_index INTEGER NOT NULL DEFAULT 0, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, UNIQUE(uuid,quest_id,cycle_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("placed_blocks") + " (world_uuid " + text + " NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, placed_at BIGINT NOT NULL, PRIMARY KEY(world_uuid,x,y,z))");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("distributed_locks") + " (lock_key " + text + " PRIMARY KEY, owner_id " + text + " NOT NULL, expires_at BIGINT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("network_events") + " (event_id " + text + " PRIMARY KEY, server_id " + text + " NOT NULL, event_type " + text + " NOT NULL, payload " + longText + " NOT NULL, created_at BIGINT NOT NULL)");
        }
        createIndex(connection, "idx_kq_player_uuid", table("player_quests"), "uuid");
        createIndex(connection, "idx_kq_reward_status", table("pending_rewards"), "status,created_at");
        createIndex(connection, "idx_kq_placed_time", table("placed_blocks"), "placed_at");
        createIndex(connection, "idx_kq_network_time", table("network_events"), "created_at");
    }

    private void ensureSchemaColumns(Connection connection) throws SQLException {
        String text = dialect == Dialect.SQLITE ? "TEXT" : "VARCHAR(255)";
        ensureColumn(connection, table("player_profiles"), "season_cycle_id", text + " NOT NULL DEFAULT ''");
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, null)) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void createIndex(Connection connection, String name, String table, String columns) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX " + name + " ON " + table + "(" + columns + ")");
        } catch (SQLException ignored) {
            // Existing indexes and database-specific duplicate-index codes are harmless.
        }
    }

    private void migrateV2(Connection connection) {
        if (dialect != Dialect.SQLITE) return;
        try {
            if (!tableExists(connection, "daily_quests") || !tableExists(connection, "player_quests")) return;
            long startedAt = readLegacyCycleStartedAt(connection);
            if (startedAt <= 0L) startedAt = System.currentTimeMillis();
            long nextReset = startedAt + 86_400_000L;
            String cycleId = "daily:" + startedAt;
            int migrated = 0;

            try (PreparedStatement cycle = connection.prepareStatement(insertIgnore(table("cycle_states"),
                    "cycle_name,instance_id,started_at,next_reset_at,updated_at", 5))) {
                cycle.setString(1, "daily");
                cycle.setString(2, cycleId);
                cycle.setLong(3, startedAt);
                cycle.setLong(4, nextReset);
                cycle.setLong(5, System.currentTimeMillis());
                migrated += cycle.executeUpdate();
            }
            try (Statement query = connection.createStatement();
                 ResultSet result = query.executeQuery("SELECT difficulty,position,quest_id FROM daily_quests");
                 PreparedStatement insert = connection.prepareStatement(insertIgnore(table("cycle_quests"),
                         "cycle_name,difficulty,position,quest_id", 4))) {
                while (result.next()) {
                    insert.setString(1, "daily");
                    insert.setString(2, result.getString("difficulty"));
                    insert.setInt(3, result.getInt("position"));
                    insert.setString(4, result.getString("quest_id"));
                    insert.addBatch();
                }
                for (int value : insert.executeBatch()) if (value > 0) migrated += value;
            }
            try (Statement query = connection.createStatement();
                 ResultSet result = query.executeQuery("SELECT uuid,quest_id,status,progress FROM player_quests");
                 PreparedStatement insert = connection.prepareStatement(insertIgnore(table("player_quests"),
                         "uuid,cycle_name,cycle_id,quest_id,status,progress,accepted_at,completed_at", 8))) {
                long now = System.currentTimeMillis();
                while (result.next()) {
                    insert.setString(1, result.getString("uuid"));
                    insert.setString(2, "daily");
                    insert.setString(3, cycleId);
                    insert.setString(4, result.getString("quest_id"));
                    insert.setString(5, result.getString("status"));
                    insert.setInt(6, result.getInt("progress"));
                    insert.setLong(7, now);
                    insert.setLong(8, "COMPLETED".equalsIgnoreCase(result.getString("status")) ? now : 0L);
                    insert.addBatch();
                }
                for (int value : insert.executeBatch()) if (value > 0) migrated += value;
            }

            if (tableExists(connection, "pending_rewards")) {
                try (Statement query = connection.createStatement();
                     ResultSet result = query.executeQuery("SELECT claim_id,uuid,quest_id,cycle_id,reward_data,status,attempts,next_command_index,created_at,updated_at FROM pending_rewards");
                     PreparedStatement insert = connection.prepareStatement(insertIgnore(table("pending_rewards"),
                             "claim_id,uuid,quest_id,cycle_name,cycle_id,reward_data,status,attempts,next_command_index,created_at,updated_at", 11))) {
                    while (result.next()) {
                        insert.setString(1, result.getString("claim_id"));
                        insert.setString(2, result.getString("uuid"));
                        insert.setString(3, result.getString("quest_id"));
                        insert.setString(4, "daily");
                        insert.setString(5, result.getString("cycle_id"));
                        insert.setString(6, result.getString("reward_data"));
                        String status = result.getString("status");
                        insert.setString(7, "EXPIRED".equalsIgnoreCase(status) ? "PENDING" : status);
                        insert.setInt(8, result.getInt("attempts"));
                        insert.setInt(9, result.getInt("next_command_index"));
                        insert.setLong(10, result.getLong("created_at"));
                        insert.setLong(11, result.getLong("updated_at"));
                        insert.addBatch();
                    }
                    for (int value : insert.executeBatch()) if (value > 0) migrated += value;
                } catch (SQLException exception) {
                    plugin.getLogger().log(Level.WARNING, "Could not migrate legacy pending rewards; quest progress migration will continue", exception);
                }
            }

            if (tableExists(connection, "placed_blocks")) {
                try (Statement query = connection.createStatement();
                     ResultSet result = query.executeQuery("SELECT world_uuid,x,y,z,placed_at FROM placed_blocks");
                     PreparedStatement insert = connection.prepareStatement(insertIgnore(table("placed_blocks"),
                             "world_uuid,x,y,z,placed_at", 5))) {
                    while (result.next()) {
                        insert.setString(1, result.getString("world_uuid"));
                        insert.setInt(2, result.getInt("x"));
                        insert.setInt(3, result.getInt("y"));
                        insert.setInt(4, result.getInt("z"));
                        insert.setLong(5, result.getLong("placed_at"));
                        insert.addBatch();
                    }
                    for (int value : insert.executeBatch()) if (value > 0) migrated += value;
                }
            }
            if (migrated > 0) plugin.getLogger().info("Migrated " + migrated + " KoraQuest 2.x rows into the multi-cycle schema.");
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not migrate the old KoraQuest SQLite schema", exception);
        }
    }

    private boolean tableExists(Connection connection, String name) throws SQLException {
        try (ResultSet result = connection.getMetaData().getTables(null, null, name, null)) {
            return result.next();
        }
    }

    private long rowCount(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.next() ? result.getLong(1) : 0L;
        }
    }

    private long readLegacyCycleStartedAt(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT value FROM meta WHERE key='cycle_started_at'");
             ResultSet result = statement.executeQuery()) {
            return result.next() ? Long.parseLong(result.getString(1)) : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public StorageSnapshot loadSync() throws SQLException {
        Map<String, CycleState> cycles = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement states = connection.prepareStatement("SELECT cycle_name,instance_id,started_at,next_reset_at FROM " + table("cycle_states"));
             ResultSet result = states.executeQuery()) {
            while (result.next()) {
                String name = result.getString("cycle_name");
                cycles.put(name, new CycleState(name, result.getString("instance_id"), result.getLong("started_at"),
                        result.getLong("next_reset_at"), loadCycleQuests(connection, name)));
            }
        }
        return new StorageSnapshot(cycles);
    }

    private Map<Difficulty, List<String>> loadCycleQuests(Connection connection, String cycleName) throws SQLException {
        Map<Difficulty, List<String>> result = emptyDifficultyMap();
        try (PreparedStatement statement = connection.prepareStatement("SELECT difficulty,quest_id FROM " + table("cycle_quests") + " WHERE cycle_name=? ORDER BY difficulty,position")) {
            statement.setString(1, cycleName);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    try {
                        result.get(Difficulty.from(rows.getString("difficulty"))).add(rows.getString("quest_id"));
                    } catch (RuntimeException ignored) { }
                }
            }
        }
        return immutableDifficultyMap(result);
    }

    public CompletableFuture<PlayerQuestData> loadPlayerAsync(UUID uuid, Map<String, String> currentCycleIds) {
        return supply(() -> loadPlayerSync(uuid, currentCycleIds));
    }

    public PlayerQuestData loadPlayerBlocking(UUID uuid, Map<String, String> currentCycleIds) throws Exception {
        return loadPlayerAsync(uuid, currentCycleIds).get(10, TimeUnit.SECONDS);
    }

    private PlayerQuestData loadPlayerSync(UUID uuid, Map<String, String> currentCycleIds) throws SQLException {
        PlayerQuestData data = new PlayerQuestData();
        try (Connection connection = dataSource.getConnection()) {
            if (!currentCycleIds.isEmpty()) {
                String placeholders = String.join(",", currentCycleIds.values().stream().map(ignored -> "?").toList());
                try (PreparedStatement statement = connection.prepareStatement("SELECT cycle_name,cycle_id,quest_id,status,progress,accepted_at,completed_at FROM " + table("player_quests") + " WHERE uuid=? AND cycle_id IN (" + placeholders + ")")) {
                    statement.setString(1, uuid.toString());
                    int index = 2;
                    for (String cycleId : currentCycleIds.values()) statement.setString(index++, cycleId);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            QuestProgressEntry.Status status = "COMPLETED".equalsIgnoreCase(result.getString("status"))
                                    ? QuestProgressEntry.Status.COMPLETED : QuestProgressEntry.Status.ACTIVE;
                            data.loadEntry(new QuestProgressEntry(result.getString("cycle_name"), result.getString("cycle_id"),
                                    result.getString("quest_id"), status, result.getInt("progress"),
                                    result.getLong("accepted_at"), result.getLong("completed_at")));
                        }
                    }
                }
                loadRerolls(connection, uuid, currentCycleIds.values(), data);
            }
            loadProfile(connection, uuid, data);
        }
        return data;
    }

    private void loadProfile(Connection connection, UUID uuid, PlayerQuestData data) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT daily_streak,best_daily_streak,last_daily_cycle_id,last_daily_completed_at,perfect_weeks,catchup_tokens,season_cycle_id,season_level,season_xp FROM " + table("player_profiles") + " WHERE uuid=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) data.loadProfile(new PlayerProfile(result.getInt(1), result.getInt(2), result.getString(3),
                        result.getLong(4), result.getInt(5), result.getInt(6), result.getString(7), result.getInt(8), result.getLong(9)));
            }
        }
    }

    private void loadRerolls(Connection connection, UUID uuid, java.util.Collection<String> cycleIds, PlayerQuestData data) throws SQLException {
        if (cycleIds.isEmpty()) return;
        String placeholders = String.join(",", cycleIds.stream().map(ignored -> "?").toList());
        try (PreparedStatement statement = connection.prepareStatement("SELECT cycle_id,original_quest_id,replacement_quest_id,created_at FROM " + table("player_rerolls") + " WHERE uuid=? AND cycle_id IN (" + placeholders + ")")) {
            statement.setString(1, uuid.toString());
            int index = 2;
            for (String cycleId : cycleIds) statement.setString(index++, cycleId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) data.loadReroll(new RerollRecord(result.getString(1), result.getString(2), result.getString(3), result.getLong(4)));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT cycle_id,uses FROM " + table("reroll_uses") + " WHERE uuid=? AND cycle_id IN (" + placeholders + ")")) {
            statement.setString(1, uuid.toString());
            int index = 2;
            for (String cycleId : cycleIds) statement.setString(index++, cycleId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) data.loadRerollUse(result.getString(1), result.getInt(2));
            }
        }
    }

    public CompletableFuture<Void> persistCycleAsync(CycleState state) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    try (PreparedStatement upsert = connection.prepareStatement(upsertCycleSql())) {
                        upsert.setString(1, state.name());
                        upsert.setString(2, state.instanceId());
                        upsert.setLong(3, state.startedAt());
                        upsert.setLong(4, state.nextResetAt());
                        upsert.setLong(5, System.currentTimeMillis());
                        upsert.executeUpdate();
                    }
                    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table("cycle_quests") + " WHERE cycle_name=?")) {
                        delete.setString(1, state.name());
                        delete.executeUpdate();
                    }
                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table("cycle_quests") + "(cycle_name,difficulty,position,quest_id) VALUES(?,?,?,?)")) {
                        for (Difficulty difficulty : Difficulty.values()) {
                            List<String> ids = state.questIds(difficulty);
                            for (int i = 0; i < ids.size(); i++) {
                                insert.setString(1, state.name());
                                insert.setString(2, difficulty.key());
                                insert.setInt(3, i);
                                insert.setString(4, ids.get(i));
                                insert.addBatch();
                            }
                        }
                        insert.executeBatch();
                    }
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        });
    }

    public CompletableFuture<Void> persistPlayerAsync(UUID uuid, PlayerQuestSnapshot snapshot) {
        return run(() -> writePlayer(uuid, snapshot));
    }

    public void persistPlayerSync(UUID uuid, PlayerQuestSnapshot snapshot) throws SQLException {
        writePlayer(uuid, snapshot);
    }

    private void writePlayer(UUID uuid, PlayerQuestSnapshot snapshot) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Set<String> currentCycleIds = new java.util.HashSet<>();
                try (Statement cycles = connection.createStatement();
                     ResultSet result = cycles.executeQuery("SELECT instance_id FROM " + table("cycle_states"))) {
                    while (result.next()) currentCycleIds.add(result.getString(1));
                }
                // Only replace mutable ACTIVE rows. COMPLETED rows are durable reward outcomes and must
                // never be downgraded by an older async player snapshot racing the reward transaction.
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table("player_quests") + " WHERE uuid=? AND status='ACTIVE' AND cycle_id IN (SELECT instance_id FROM " + table("cycle_states") + ")")) {
                    delete.setString(1, uuid.toString());
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(insertIgnore(table("player_quests"),
                        "uuid,cycle_name,cycle_id,quest_id,status,progress,accepted_at,completed_at", 8))) {
                    for (QuestProgressEntry entry : snapshot.entries()) {
                        if (!currentCycleIds.contains(entry.cycleId())) continue;
                        insert.setString(1, uuid.toString());
                        insert.setString(2, entry.cycleName());
                        insert.setString(3, entry.cycleId());
                        insert.setString(4, entry.questId());
                        insert.setString(5, entry.status().name());
                        insert.setInt(6, entry.progress());
                        insert.setLong(7, entry.acceptedAt());
                        insert.setLong(8, entry.completedAt());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                writeProfile(connection, uuid, snapshot.profile());
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table("player_rerolls") + " WHERE uuid=? AND cycle_id IN (SELECT instance_id FROM " + table("cycle_states") + ")")) {
                    delete.setString(1, uuid.toString());
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table("player_rerolls") + "(uuid,cycle_id,original_quest_id,replacement_quest_id,created_at) VALUES(?,?,?,?,?)")) {
                    for (RerollRecord reroll : snapshot.rerolls()) {
                        if (!currentCycleIds.contains(reroll.cycleId())) continue;
                        insert.setString(1, uuid.toString());
                        insert.setString(2, reroll.cycleId());
                        insert.setString(3, reroll.originalQuestId());
                        insert.setString(4, reroll.replacementQuestId());
                        insert.setLong(5, reroll.createdAt());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table("reroll_uses") + " WHERE uuid=? AND cycle_id IN (SELECT instance_id FROM " + table("cycle_states") + ")")) {
                    delete.setString(1, uuid.toString());
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table("reroll_uses") + "(uuid,cycle_id,uses) VALUES(?,?,?)")) {
                    for (Map.Entry<String, Integer> use : snapshot.rerollUses().entrySet()) {
                        if (!currentCycleIds.contains(use.getKey())) continue;
                        insert.setString(1, uuid.toString());
                        insert.setString(2, use.getKey());
                        insert.setInt(3, use.getValue());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void writeProfile(Connection connection, UUID uuid, PlayerProfile profile) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(upsertProfileSql())) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, profile.dailyStreak());
            statement.setInt(3, profile.bestDailyStreak());
            statement.setString(4, profile.lastDailyCycleId());
            statement.setLong(5, profile.lastDailyCompletedAt());
            statement.setInt(6, profile.perfectWeeks());
            statement.setInt(7, profile.catchupTokens());
            statement.setString(8, profile.seasonCycleId());
            statement.setInt(9, profile.seasonLevel());
            statement.setLong(10, profile.seasonXp());
            statement.setLong(11, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    public void deleteQuestAsync(String questId) {
        submit(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String[] tables = {"cycle_quests", "player_quests"};
                for (String name : tables) {
                    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table(name) + " WHERE quest_id=?")) {
                        statement.setString(1, questId);
                        statement.executeUpdate();
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table("player_rerolls") + " WHERE original_quest_id=? OR replacement_quest_id=?")) {
                    statement.setString(1, questId);
                    statement.setString(2, questId);
                    statement.executeUpdate();
                }
            }
        });
    }

    public CompletableFuture<RewardClaim> createOrGetRewardClaimAsync(UUID uuid, String questId, String cycleName,
                                                                      String cycleId, List<String> commands) {
        return supply(() -> {
            long now = System.currentTimeMillis();
            String claimId = UUID.randomUUID().toString();
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement insert = connection.prepareStatement(insertIgnore(table("pending_rewards"),
                        "claim_id,uuid,quest_id,cycle_name,cycle_id,reward_data,status,attempts,next_command_index,created_at,updated_at", 11))) {
                    insert.setString(1, claimId);
                    insert.setString(2, uuid.toString());
                    insert.setString(3, questId);
                    insert.setString(4, cycleName);
                    insert.setString(5, cycleId);
                    insert.setString(6, encodeCommands(commands));
                    insert.setString(7, "PENDING");
                    insert.setInt(8, 0);
                    insert.setInt(9, 0);
                    insert.setLong(10, now);
                    insert.setLong(11, now);
                    insert.executeUpdate();
                }
                try (PreparedStatement select = connection.prepareStatement("SELECT claim_id,uuid,quest_id,cycle_name,cycle_id,reward_data,status,attempts,next_command_index,created_at FROM " + table("pending_rewards") + " WHERE uuid=? AND quest_id=? AND cycle_id=?")) {
                    select.setString(1, uuid.toString());
                    select.setString(2, questId);
                    select.setString(3, cycleId);
                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next()) throw new SQLException("Could not create or load reward claim");
                        return readClaim(result);
                    }
                }
            }
        });
    }

    public CompletableFuture<List<RewardClaim>> pendingRewardClaimsAsync(int limit) {
        return queryPendingRewardClaims(null, limit);
    }

    public CompletableFuture<List<RewardClaim>> pendingRewardClaimsAsync(UUID uuid, int limit) {
        return queryPendingRewardClaims(uuid, limit);
    }

    private CompletableFuture<List<RewardClaim>> queryPendingRewardClaims(UUID uuid, int limit) {
        return supply(() -> {
            List<RewardClaim> claims = new ArrayList<>();
            String sql = "SELECT claim_id,uuid,quest_id,cycle_name,cycle_id,reward_data,status,attempts,next_command_index,created_at FROM "
                    + table("pending_rewards") + " WHERE status='PENDING'"
                    + (uuid == null ? "" : " AND uuid=?") + " ORDER BY created_at LIMIT ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                if (uuid != null) statement.setString(index++, uuid.toString());
                statement.setInt(index, Math.max(1, limit));
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) claims.add(readClaim(result));
                }
            }
            return List.copyOf(claims);
        });
    }

    public CompletableFuture<Void> markRewardAttemptAsync(String claimId) {
        return updateReward("UPDATE " + table("pending_rewards") + " SET attempts=attempts+1,updated_at=? WHERE claim_id=? AND status='PENDING'", claimId, -1);
    }

    public CompletableFuture<Void> advanceRewardCommandAsync(String claimId, int nextCommandIndex) {
        return updateReward("UPDATE " + table("pending_rewards") + " SET next_command_index=?,updated_at=? WHERE claim_id=? AND status='PENDING'", claimId, nextCommandIndex);
    }

    private CompletableFuture<Void> updateReward(String sql, String claimId, int index) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                if (index < 0) {
                    statement.setLong(1, System.currentTimeMillis());
                    statement.setString(2, claimId);
                } else {
                    statement.setInt(1, index);
                    statement.setLong(2, System.currentTimeMillis());
                    statement.setString(3, claimId);
                }
                if (statement.executeUpdate() != 1) throw new SQLException("Reward claim is no longer pending: " + claimId);
            }
        });
    }

    public CompletableFuture<Void> finalizeRewardSuccessAsync(RewardClaim claim, boolean markQuestCompleted) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    try (PreparedStatement reward = connection.prepareStatement("UPDATE " + table("pending_rewards") + " SET status='DELIVERED',updated_at=? WHERE claim_id=? AND status<>'DELIVERED'")) {
                        reward.setLong(1, System.currentTimeMillis());
                        reward.setString(2, claim.claimId());
                        if (reward.executeUpdate() != 1) throw new SQLException("Reward claim is no longer deliverable: " + claim.claimId());
                    }
                    if (markQuestCompleted) {
                        upsertCompletedQuest(connection, claim);
                    }
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        });
    }

    private void upsertCompletedQuest(Connection connection, RewardClaim claim) throws SQLException {
        String sql = dialect == Dialect.SQLITE
                ? "INSERT INTO " + table("player_quests") + "(uuid,cycle_name,cycle_id,quest_id,status,progress,accepted_at,completed_at) VALUES(?,?,?,?, 'COMPLETED',0,?,?) ON CONFLICT(uuid,cycle_id,quest_id) DO UPDATE SET status='COMPLETED',progress=0,completed_at=excluded.completed_at"
                : "INSERT INTO " + table("player_quests") + "(uuid,cycle_name,cycle_id,quest_id,status,progress,accepted_at,completed_at) VALUES(?,?,?,?, 'COMPLETED',0,?,?) ON DUPLICATE KEY UPDATE status='COMPLETED',progress=0,completed_at=VALUES(completed_at)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            statement.setString(1, claim.uuid().toString());
            statement.setString(2, claim.cycleName());
            statement.setString(3, claim.cycleId());
            statement.setString(4, claim.questId());
            statement.setLong(5, now);
            statement.setLong(6, now);
            statement.executeUpdate();
        }
    }

    public CompletableFuture<Map<BlockKey, Long>> loadPlacedBlocksAsync(long notBefore) {
        return supply(() -> {
            Map<BlockKey, Long> blocks = new HashMap<>();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT world_uuid,x,y,z,placed_at FROM " + table("placed_blocks") + " WHERE placed_at>=?")) {
                statement.setLong(1, notBefore);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        try {
                            blocks.put(new BlockKey(UUID.fromString(result.getString(1)), result.getInt(2), result.getInt(3), result.getInt(4)), result.getLong(5));
                        } catch (IllegalArgumentException ignored) { }
                    }
                }
            }
            return blocks;
        });
    }

    public Map<BlockKey, Long> loadPlacedBlocksBlocking(long notBefore) throws Exception {
        return loadPlacedBlocksAsync(notBefore).get(10, TimeUnit.SECONDS);
    }

    public void persistPlacedBlockAsync(BlockKey key, long placedAt) {
        submit(() -> {
            String sql = dialect == Dialect.SQLITE
                    ? "INSERT INTO " + table("placed_blocks") + "(world_uuid,x,y,z,placed_at) VALUES(?,?,?,?,?) ON CONFLICT(world_uuid,x,y,z) DO UPDATE SET placed_at=excluded.placed_at"
                    : "INSERT INTO " + table("placed_blocks") + "(world_uuid,x,y,z,placed_at) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE placed_at=VALUES(placed_at)";
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key.worldId().toString());
                statement.setInt(2, key.x());
                statement.setInt(3, key.y());
                statement.setInt(4, key.z());
                statement.setLong(5, placedAt);
                statement.executeUpdate();
            }
        });
    }

    public void deletePlacedBlockAsync(BlockKey key) {
        submit(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table("placed_blocks") + " WHERE world_uuid=? AND x=? AND y=? AND z=?")) {
                statement.setString(1, key.worldId().toString());
                statement.setInt(2, key.x());
                statement.setInt(3, key.y());
                statement.setInt(4, key.z());
                statement.executeUpdate();
            }
        });
    }

    public void purgePlacedBlocksAsync(long before) { purge("placed_blocks", "placed_at", before); }
    public void purgeDeliveredRewardsAsync(long before) { purgeWhere("pending_rewards", "status='DELIVERED' AND updated_at<?", before); }

    public void purgeCycleHistoryAsync(Map<String, Long> cutoffs) {
        if (cutoffs == null || cutoffs.isEmpty()) return;
        submit(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    String questsSql = "DELETE FROM " + table("player_quests")
                            + " WHERE cycle_name=? AND cycle_id NOT IN (SELECT instance_id FROM " + table("cycle_states") + " WHERE cycle_name=?)"
                            + " AND ((completed_at>0 AND completed_at<?) OR (completed_at=0 AND accepted_at<?))";
                    String rerollsSql = "DELETE FROM " + table("player_rerolls")
                            + " WHERE cycle_id LIKE ? ESCAPE '!' AND cycle_id NOT IN (SELECT instance_id FROM " + table("cycle_states") + ") AND created_at<?";
                    try (PreparedStatement quests = connection.prepareStatement(questsSql);
                         PreparedStatement rerolls = connection.prepareStatement(rerollsSql)) {
                        for (Map.Entry<String, Long> entry : cutoffs.entrySet()) {
                            String cycleName = entry.getKey();
                            long before = entry.getValue();
                            quests.setString(1, cycleName);
                            quests.setString(2, cycleName);
                            quests.setLong(3, before);
                            quests.setLong(4, before);
                            quests.addBatch();

                            rerolls.setString(1, escapeLike(cycleName) + ":%");
                            rerolls.setLong(2, before);
                            rerolls.addBatch();
                        }
                        quests.executeBatch();
                        rerolls.executeBatch();
                    }
                    try (Statement staleUses = connection.createStatement()) {
                        staleUses.executeUpdate("DELETE FROM " + table("reroll_uses")
                                + " WHERE cycle_id NOT IN (SELECT instance_id FROM " + table("cycle_states") + ")");
                    }
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        });
    }

    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    public void purgeNetworkEventsAsync(long before) { purge("network_events", "created_at", before); }

    private void purge(String table, String column, long before) { purgeWhere(table, column + ">0 AND " + column + "<?", before); }
    private void purgeWhere(String name, String where, long before) {
        submit(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table(name) + " WHERE " + where)) {
                statement.setLong(1, before);
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Boolean> acquireLock(String lockKey, String owner, long expiresAt) {
        return supply(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table("distributed_locks") + " WHERE lock_key=? AND expires_at<?")) {
                        delete.setString(1, lockKey);
                        delete.setLong(2, System.currentTimeMillis());
                        delete.executeUpdate();
                    }
                    boolean acquired;
                    try (PreparedStatement insert = connection.prepareStatement(insertIgnore(table("distributed_locks"), "lock_key,owner_id,expires_at", 3))) {
                        insert.setString(1, lockKey);
                        insert.setString(2, owner);
                        insert.setLong(3, expiresAt);
                        acquired = insert.executeUpdate() == 1;
                    }
                    connection.commit();
                    return acquired;
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        });
    }

    public void releaseLock(String lockKey, String owner) {
        submit(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table("distributed_locks") + " WHERE lock_key=? AND owner_id=?")) {
                statement.setString(1, lockKey);
                statement.setString(2, owner);
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> publishNetworkEvent(String type, String payload) {
        return run(() -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table("network_events") + "(event_id,server_id,event_type,payload,created_at) VALUES(?,?,?,?,?)")) {
                statement.setString(1, UUID.randomUUID().toString());
                statement.setString(2, config.serverId());
                statement.setString(3, type);
                statement.setString(4, payload == null ? "" : payload);
                statement.setLong(5, System.currentTimeMillis());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<List<NetworkEvent>> networkEventsAfter(long timestamp, String eventId) {
        return supply(() -> {
            List<NetworkEvent> result = new ArrayList<>();
            String cursorId = eventId == null ? "" : eventId;
            String sql = "SELECT event_id,server_id,event_type,payload,created_at FROM " + table("network_events")
                    + " WHERE (created_at>? OR (created_at=? AND event_id>?)) AND server_id<>?"
                    + " ORDER BY created_at,event_id LIMIT 200";
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, timestamp);
                statement.setLong(2, timestamp);
                statement.setString(3, cursorId);
                statement.setString(4, config.serverId());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) result.add(new NetworkEvent(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4), rows.getLong(5)));
                }
            }
            return List.copyOf(result);
        });
    }

    public void flush() {
        try {
            Future<?> future = executor.submit(() -> { });
            future.get(15, TimeUnit.SECONDS);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Timed out while flushing database writes", exception);
        }
    }

    public void close() {
        flush();
        executor.shutdown();
        try { executor.awaitTermination(10, TimeUnit.SECONDS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
        if (dataSource != null) dataSource.close();
    }

    private RewardClaim readClaim(ResultSet result) throws SQLException {
        return new RewardClaim(result.getString("claim_id"), UUID.fromString(result.getString("uuid")),
                result.getString("quest_id"), result.getString("cycle_name"), result.getString("cycle_id"),
                decodeCommands(result.getString("reward_data")), result.getString("status"), result.getInt("attempts"),
                result.getInt("next_command_index"), result.getLong("created_at"));
    }

    private String encodeCommands(List<String> commands) {
        Base64.Encoder encoder = Base64.getEncoder();
        return commands.stream().map(command -> encoder.encodeToString(command.getBytes(StandardCharsets.UTF_8)))
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private List<String> decodeCommands(String encoded) {
        if (encoded == null || encoded.isBlank()) return List.of();
        Base64.Decoder decoder = Base64.getDecoder();
        List<String> commands = new ArrayList<>();
        for (String line : encoded.split("\\R")) {
            try { commands.add(new String(decoder.decode(line), StandardCharsets.UTF_8)); }
            catch (IllegalArgumentException ignored) { }
        }
        return List.copyOf(commands);
    }

    private String upsertCycleSql() {
        if (dialect == Dialect.SQLITE) {
            return "INSERT INTO " + table("cycle_states") + "(cycle_name,instance_id,started_at,next_reset_at,updated_at) VALUES(?,?,?,?,?) ON CONFLICT(cycle_name) DO UPDATE SET instance_id=excluded.instance_id,started_at=excluded.started_at,next_reset_at=excluded.next_reset_at,updated_at=excluded.updated_at";
        }
        return "INSERT INTO " + table("cycle_states") + "(cycle_name,instance_id,started_at,next_reset_at,updated_at) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE instance_id=VALUES(instance_id),started_at=VALUES(started_at),next_reset_at=VALUES(next_reset_at),updated_at=VALUES(updated_at)";
    }

    private String upsertProfileSql() {
        if (dialect == Dialect.SQLITE) {
            return "INSERT INTO " + table("player_profiles") + "(uuid,daily_streak,best_daily_streak,last_daily_cycle_id,last_daily_completed_at,perfect_weeks,catchup_tokens,season_cycle_id,season_level,season_xp,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET daily_streak=excluded.daily_streak,best_daily_streak=excluded.best_daily_streak,last_daily_cycle_id=excluded.last_daily_cycle_id,last_daily_completed_at=excluded.last_daily_completed_at,perfect_weeks=excluded.perfect_weeks,catchup_tokens=excluded.catchup_tokens,season_cycle_id=excluded.season_cycle_id,season_level=excluded.season_level,season_xp=excluded.season_xp,updated_at=excluded.updated_at";
        }
        return "INSERT INTO " + table("player_profiles") + "(uuid,daily_streak,best_daily_streak,last_daily_cycle_id,last_daily_completed_at,perfect_weeks,catchup_tokens,season_cycle_id,season_level,season_xp,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE daily_streak=VALUES(daily_streak),best_daily_streak=VALUES(best_daily_streak),last_daily_cycle_id=VALUES(last_daily_cycle_id),last_daily_completed_at=VALUES(last_daily_completed_at),perfect_weeks=VALUES(perfect_weeks),catchup_tokens=VALUES(catchup_tokens),season_cycle_id=VALUES(season_cycle_id),season_level=VALUES(season_level),season_xp=VALUES(season_xp),updated_at=VALUES(updated_at)";
    }

    private String insertIgnore(String table, String columns, int values) {
        String placeholders = String.join(",", java.util.Collections.nCopies(values, "?"));
        return (dialect == Dialect.SQLITE ? "INSERT OR IGNORE INTO " : "INSERT IGNORE INTO ") + table + "(" + columns + ") VALUES(" + placeholders + ")";
    }

    private Map<Difficulty, List<String>> emptyDifficultyMap() {
        Map<Difficulty, List<String>> result = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) result.put(difficulty, new ArrayList<>());
        return result;
    }

    private Map<Difficulty, List<String>> immutableDifficultyMap(Map<Difficulty, List<String>> source) {
        Map<Difficulty, List<String>> result = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) result.put(difficulty, List.copyOf(source.getOrDefault(difficulty, List.of())));
        return Map.copyOf(result);
    }

    private String table(String suffix) { return prefix + suffix; }

    private void submit(SqlTask task) {
        executor.execute(() -> {
            try { task.run(); }
            catch (Throwable throwable) { plugin.getLogger().log(Level.SEVERE, "Database operation failed", throwable); }
        });
    }

    private <T> CompletableFuture<T> supply(SqlSupplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try { future.complete(task.get()); }
            catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "Database operation failed", throwable);
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private CompletableFuture<Void> run(SqlTask task) {
        return supply(() -> { task.run(); return null; });
    }

    public record NetworkEvent(String eventId, String serverId, String type, String payload, long createdAt) { }
    @FunctionalInterface private interface SqlTask { void run() throws Exception; }
    @FunctionalInterface private interface SqlSupplier<T> { T get() throws Exception; }

    private enum Dialect {
        SQLITE, MYSQL, MARIADB;
        static Dialect parse(String input) {
            try { return valueOf(input == null ? "SQLITE" : input.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return SQLITE; }
        }
    }
}
