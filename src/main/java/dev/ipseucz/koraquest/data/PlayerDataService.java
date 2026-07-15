package dev.ipseucz.koraquest.data;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.util.PluginPaths;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PlayerDataService {
    private final KoraQuestPlugin plugin;
    private final SQLiteStorage storage;
    private final Map<UUID, PlayerQuestData> players = new ConcurrentHashMap<>();
    private final Map<Difficulty, List<String>> dailyQuests = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private final AtomicLong cycleStartedAt = new AtomicLong();

    public PlayerDataService(KoraQuestPlugin plugin) throws Exception {
        this.plugin = plugin;
        this.storage = new SQLiteStorage(plugin);
        storage.initialize();
        StorageSnapshot snapshot = storage.loadSync();
        if (snapshot.isEmpty()) {
            StorageSnapshot legacy = readLegacyData();
            if (legacy != null && !legacy.isEmpty()) {
                storage.replaceAllSync(legacy);
                snapshot = legacy;
                archiveLegacyData();
            }
        }
        applySnapshot(snapshot);
    }

    private void applySnapshot(StorageSnapshot snapshot) {
        cycleStartedAt.set(snapshot.cycleStartedAt());
        players.clear();
        players.putAll(snapshot.players());
        for (Difficulty difficulty : Difficulty.values()) {
            dailyQuests.put(difficulty, List.copyOf(snapshot.dailyQuests().getOrDefault(difficulty, List.of())));
        }
    }

    public PlayerQuestData player(UUID uuid) {
        return players.computeIfAbsent(uuid, ignored -> new PlayerQuestData());
    }

    public long cycleStartedAt() {
        return cycleStartedAt.get();
    }

    public List<String> dailyQuestIds(Difficulty difficulty) {
        return dailyQuests.getOrDefault(difficulty, List.of());
    }

    public boolean isDailyQuest(String questId) {
        for (List<String> ids : dailyQuests.values()) {
            if (ids.contains(questId)) {
                return true;
            }
        }
        return false;
    }

    public void replaceCycle(long startedAt, Map<Difficulty, List<String>> selected) {
        cycleStartedAt.set(startedAt);
        players.clear();
        dirtyPlayers.clear();
        Map<Difficulty, List<String>> copy = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            List<String> ids = List.copyOf(selected.getOrDefault(difficulty, List.of()));
            dailyQuests.put(difficulty, ids);
            copy.put(difficulty, ids);
        }
        storage.persistCycleAsync(startedAt, copy);
    }

    public void markDirty(UUID uuid, boolean immediate) {
        dirtyPlayers.add(uuid);
        if (immediate) {
            savePlayerAsync(uuid);
        }
    }

    public void savePlayerAsync(UUID uuid) {
        PlayerQuestData data = players.get(uuid);
        if (data == null) {
            dirtyPlayers.remove(uuid);
            return;
        }
        dirtyPlayers.remove(uuid);
        storage.persistPlayerAsync(uuid, data.snapshot());
    }

    public void flushDirtyAsync() {
        for (UUID uuid : List.copyOf(dirtyPlayers)) {
            savePlayerAsync(uuid);
        }
    }

    public void removeQuestEverywhere(String questId) {
        for (PlayerQuestData data : players.values()) {
            data.removeQuest(questId);
        }
        for (Difficulty difficulty : Difficulty.values()) {
            List<String> ids = new ArrayList<>(dailyQuestIds(difficulty));
            ids.removeIf(questId::equalsIgnoreCase);
            dailyQuests.put(difficulty, List.copyOf(ids));
        }
        storage.deleteQuestAsync(questId);
    }

    public void shutdown() {
        flushDirtyAsync();
        storage.close();
    }

    private StorageSnapshot readLegacyData() {
        File file = PluginPaths.legacyDataFile(plugin);
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        long cycle = yaml.getLong("cycle-started-at", 0L);
        Map<Difficulty, List<String>> daily = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            daily.put(difficulty, List.copyOf(yaml.getStringList("daily." + difficulty.key())));
        }
        Map<UUID, PlayerQuestData> migratedPlayers = new HashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section != null) {
            for (String uuidText : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    PlayerQuestData data = new PlayerQuestData();
                    String base = "players." + uuidText;
                    Map<String, Integer> progress = new HashMap<>();
                    ConfigurationSection progressSection = yaml.getConfigurationSection(base + ".progress");
                    if (progressSection != null) {
                        for (String questId : progressSection.getKeys(false)) {
                            progress.put(questId, progressSection.getInt(questId));
                        }
                    }
                    for (String questId : yaml.getStringList(base + ".active")) {
                        data.loadActive(questId, progress.getOrDefault(questId, 0));
                    }
                    for (String questId : yaml.getStringList(base + ".completed")) {
                        data.loadCompleted(questId);
                    }
                    migratedPlayers.put(uuid, data);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return new StorageSnapshot(cycle, daily, migratedPlayers);
    }

    private void archiveLegacyData() {
        File source = PluginPaths.legacyDataFile(plugin);
        File target = PluginPaths.migratedLegacyDataFile(plugin);
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Migrated data/data.yml to data/data.db. The old file was renamed to data/data.yml.migrated.");
        } catch (Exception exception) {
            plugin.getLogger().warning("Data migration succeeded, but data/data.yml could not be archived: " + exception.getMessage());
        }
    }
}
