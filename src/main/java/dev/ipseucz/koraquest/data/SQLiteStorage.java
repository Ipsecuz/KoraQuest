package dev.ipseucz.koraquest.data;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.util.PluginPaths;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class SQLiteStorage {
    private final KoraQuestPlugin plugin;
    private final File file;
    private final ExecutorService executor;
    private Connection connection;

    public SQLiteStorage(KoraQuestPlugin plugin) {
        this.plugin = plugin;
        this.file = PluginPaths.databaseFile(plugin);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "KoraQuest-SQLite");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void initialize() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS daily_quests (difficulty TEXT NOT NULL, position INTEGER NOT NULL, quest_id TEXT NOT NULL, PRIMARY KEY(difficulty, position))");
            statement.execute("CREATE TABLE IF NOT EXISTS player_quests (uuid TEXT NOT NULL, quest_id TEXT NOT NULL, status TEXT NOT NULL, progress INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid, quest_id))");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_quests_uuid ON player_quests(uuid)");
        }
    }

    public StorageSnapshot loadSync() throws SQLException {
        Map<Difficulty, List<String>> daily = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            daily.put(difficulty, new ArrayList<>());
        }
        long cycle = 0L;
        try (PreparedStatement statement = connection.prepareStatement("SELECT value FROM meta WHERE key='cycle_started_at'");
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                try {
                    cycle = Long.parseLong(result.getString(1));
                } catch (NumberFormatException ignored) {
                    cycle = 0L;
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT difficulty, quest_id FROM daily_quests ORDER BY difficulty, position");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                try {
                    Difficulty difficulty = Difficulty.from(result.getString("difficulty"));
                    daily.get(difficulty).add(result.getString("quest_id"));
                } catch (RuntimeException ignored) {
                }
            }
        }
        Map<UUID, PlayerQuestData> players = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, quest_id, status, progress FROM player_quests");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                try {
                    UUID uuid = UUID.fromString(result.getString("uuid"));
                    PlayerQuestData data = players.computeIfAbsent(uuid, ignored -> new PlayerQuestData());
                    String questId = result.getString("quest_id");
                    if ("COMPLETED".equalsIgnoreCase(result.getString("status"))) {
                        data.loadCompleted(questId);
                    } else {
                        data.loadActive(questId, result.getInt("progress"));
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        Map<Difficulty, List<String>> immutableDaily = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            immutableDaily.put(difficulty, List.copyOf(daily.get(difficulty)));
        }
        return new StorageSnapshot(cycle, immutableDaily, players);
    }

    public void replaceAllSync(StorageSnapshot snapshot) throws SQLException {
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM player_quests");
                statement.executeUpdate("DELETE FROM daily_quests");
                statement.executeUpdate("DELETE FROM meta");
            }
            writeCycle(snapshot.cycleStartedAt(), snapshot.dailyQuests(), false);
            for (Map.Entry<UUID, PlayerQuestData> entry : snapshot.players().entrySet()) {
                writePlayer(entry.getKey(), entry.getValue().snapshot(), false);
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    public void persistCycleAsync(long cycleStartedAt, Map<Difficulty, List<String>> daily) {
        submit(() -> {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM player_quests");
                    statement.executeUpdate("DELETE FROM daily_quests");
                }
                writeCycle(cycleStartedAt, daily, false);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        });
    }

    public void persistPlayerAsync(UUID uuid, PlayerQuestSnapshot snapshot) {
        submit(() -> writePlayer(uuid, snapshot, true));
    }

    public void deleteQuestAsync(String questId) {
        submit(() -> {
            try (PreparedStatement daily = connection.prepareStatement("DELETE FROM daily_quests WHERE quest_id=?");
                 PreparedStatement players = connection.prepareStatement("DELETE FROM player_quests WHERE quest_id=?")) {
                daily.setString(1, questId);
                daily.executeUpdate();
                players.setString(1, questId);
                players.executeUpdate();
            }
        });
    }

    public void flush() {
        try {
            Future<?> future = executor.submit(() -> {
            });
            future.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Timed out while flushing SQLite writes", exception);
        }
    }

    public void close() {
        flush();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not close data/data.db", exception);
            }
        }
    }

    private void writeCycle(long cycleStartedAt, Map<Difficulty, List<String>> daily, boolean transaction) throws SQLException {
        try (PreparedStatement meta = connection.prepareStatement("INSERT INTO meta(key, value) VALUES('cycle_started_at', ?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            meta.setString(1, String.valueOf(cycleStartedAt));
            meta.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO daily_quests(difficulty, position, quest_id) VALUES(?, ?, ?)")) {
            for (Difficulty difficulty : Difficulty.values()) {
                List<String> ids = daily.getOrDefault(difficulty, List.of());
                for (int index = 0; index < ids.size(); index++) {
                    insert.setString(1, difficulty.key());
                    insert.setInt(2, index);
                    insert.setString(3, ids.get(index));
                    insert.addBatch();
                }
            }
            insert.executeBatch();
        }
    }

    private void writePlayer(UUID uuid, PlayerQuestSnapshot snapshot, boolean ownTransaction) throws SQLException {
        boolean oldAutoCommit = connection.getAutoCommit();
        if (ownTransaction) {
            connection.setAutoCommit(false);
        }
        try {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM player_quests WHERE uuid=?")) {
                delete.setString(1, uuid.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO player_quests(uuid, quest_id, status, progress) VALUES(?, ?, ?, ?)")) {
                for (String id : snapshot.active()) {
                    insert.setString(1, uuid.toString());
                    insert.setString(2, id);
                    insert.setString(3, "ACTIVE");
                    insert.setInt(4, snapshot.progress().getOrDefault(id, 0));
                    insert.addBatch();
                }
                for (String id : snapshot.completed()) {
                    insert.setString(1, uuid.toString());
                    insert.setString(2, id);
                    insert.setString(3, "COMPLETED");
                    insert.setInt(4, 0);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            if (ownTransaction) {
                connection.commit();
            }
        } catch (SQLException exception) {
            if (ownTransaction) {
                connection.rollback();
            }
            throw exception;
        } finally {
            if (ownTransaction) {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    private void submit(SqlTask task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "SQLite operation failed", exception);
            }
        });
    }

    @FunctionalInterface
    private interface SqlTask {
        void run() throws SQLException;
    }
}
