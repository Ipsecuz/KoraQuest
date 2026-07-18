package dev.ipseucz.koraquest.data;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.cycle.CycleState;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.RewardClaim;
import dev.ipseucz.koraquest.security.BlockKey;
import dev.ipseucz.koraquest.storage.DatabaseStorage;
import dev.ipseucz.koraquest.util.PluginPaths;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public final class PlayerDataService {
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final DatabaseStorage storage;
    private final Map<UUID, CachedPlayer> players = new ConcurrentHashMap<>();
    private final Map<UUID, Object> loadLocks = new ConcurrentHashMap<>();
    private final Map<String, CycleState> cycles = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, CompletableFuture<Void>> saveChains = new ConcurrentHashMap<>();
    private final AtomicLong cacheGeneration = new AtomicLong();
    private volatile boolean legacyDataChecked;
    private volatile BiConsumer<String, String> networkNotifier = (type, payload) -> { };

    public PlayerDataService(KoraQuestPlugin plugin, QuestConfig config) throws Exception {
        this.plugin = plugin;
        this.config = config;
        this.storage = new DatabaseStorage(plugin, config);
        storage.initialize();
        applySnapshot(storage.loadSync());
    }

    private void applySnapshot(StorageSnapshot snapshot) {
        cycles.clear();
        cycles.putAll(snapshot.cycles());
        players.clear();
        dirtyPlayers.clear();
        cacheGeneration.incrementAndGet();
    }

    public PlayerQuestData player(UUID uuid) {
        CachedPlayer cached = players.get(uuid);
        if (cached != null) {
            cached.touch();
            return cached.data();
        }
        Object lock = loadLocks.computeIfAbsent(uuid, ignored -> new Object());
        synchronized (lock) {
            cached = players.get(uuid);
            if (cached == null) {
                try {
                    long generation;
                    PlayerQuestData loaded;
                    do {
                        generation = cacheGeneration.get();
                        loaded = storage.loadPlayerBlocking(uuid, currentCycleIds());
                    } while (generation != cacheGeneration.get());
                    if (alignSeasonProfile(loaded)) dirtyPlayers.add(uuid);
                    CachedPlayer result = new CachedPlayer(loaded, System.currentTimeMillis());
                    CachedPlayer existing = players.putIfAbsent(uuid, result);
                    cached = existing == null ? result : existing;
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.SEVERE, "Could not lazy-load quest data for " + uuid, exception);
                    CachedPlayer fallback = new CachedPlayer(new PlayerQuestData(), System.currentTimeMillis());
                    CachedPlayer existing = players.putIfAbsent(uuid, fallback);
                    cached = existing == null ? fallback : existing;
                }
            }
        }
        loadLocks.remove(uuid, lock);
        return cached.data();
    }

    public CompletableFuture<PlayerQuestData> preloadPlayerAsync(UUID uuid) {
        CachedPlayer cached = players.get(uuid);
        if (cached != null) {
            cached.touch();
            return CompletableFuture.completedFuture(cached.data());
        }
        long generation = cacheGeneration.get();
        Map<String, String> cycleIds = currentCycleIds();
        return storage.loadPlayerAsync(uuid, cycleIds).thenCompose(loaded -> {
            long current = cacheGeneration.get();
            if (current != generation) return preloadPlayerAsync(uuid);
            if (alignSeasonProfile(loaded)) dirtyPlayers.add(uuid);
            CachedPlayer result = players.compute(uuid, (ignored, existing) -> existing == null
                    ? new CachedPlayer(loaded, System.currentTimeMillis()) : existing);
            result.touch();
            return CompletableFuture.completedFuture(result.data());
        });
    }

    public void evictInactive(long idleMillis) {
        long cutoff = System.currentTimeMillis() - Math.max(60_000L, idleMillis);
        for (Map.Entry<UUID, CachedPlayer> entry : players.entrySet()) {
            UUID uuid = entry.getKey();
            if (entry.getValue().lastAccess() > cutoff || dirtyPlayers.contains(uuid) || Bukkit.getPlayer(uuid) != null) continue;
            players.remove(uuid, entry.getValue());
        }
    }

    public int cachedPlayerCount() { return players.size(); }

    /** Returns only an already-loaded player record and never performs blocking database I/O. */
    public PlayerQuestData cachedPlayer(UUID uuid) {
        CachedPlayer cached = uuid == null ? null : players.get(uuid);
        if (cached == null) return null;
        cached.touch();
        return cached.data();
    }

    public Map<String, CycleState> cycles() { return Map.copyOf(cycles); }
    public CycleState cycle(String name) { return name == null ? null : cycles.get(name.toLowerCase(java.util.Locale.ROOT)); }
    public String cycleId(String name) {
        CycleState state = cycle(name);
        return state == null ? "" : state.instanceId();
    }
    public Map<String, String> currentCycleIds() {
        Map<String, String> result = new LinkedHashMap<>();
        cycles.forEach((name, state) -> result.put(name, state.instanceId()));
        return Map.copyOf(result);
    }
    public List<String> questIds(String cycleName, Difficulty difficulty) {
        CycleState state = cycle(cycleName);
        return state == null ? List.of() : state.questIds(difficulty);
    }
    public boolean isSelectedQuest(String cycleName, String questId) {
        CycleState state = cycle(cycleName);
        return state != null && state.contains(questId);
    }

    public CompletableFuture<Void> replaceCycle(CycleState state) {
        String payload = state.name() + "|" + state.instanceId();
        CompletableFuture<Void> persistence = storage.persistCycleAsync(state)
                .thenRun(() -> applyLocalCycle(state))
                .thenCompose(ignored -> storage.publishNetworkEvent("CYCLE_RESET", payload)
                        .exceptionally(throwable -> {
                            plugin.getLogger().log(Level.WARNING, "Cycle was saved but its SQL network event could not be published", throwable);
                            return null;
                        }))
                .thenRun(() -> networkNotifier.accept("CYCLE_RESET", payload))
                .thenRun(() -> migrateLegacyDataOnce(state));
        return persistence.whenComplete((ignored, throwable) -> {
            if (throwable != null) plugin.getLogger().log(Level.SEVERE, "Could not persist cycle " + state.name(), throwable);
        });
    }

    private void applyLocalCycle(CycleState state) {
        cycles.put(state.name(), state);
        cacheGeneration.incrementAndGet();
        for (Map.Entry<UUID, CachedPlayer> entry : players.entrySet()) {
            entry.getValue().data().removeCycle(state.name(), state.instanceId());
            if (state.name().equals("seasonal") && alignSeasonProfile(entry.getValue().data())) dirtyPlayers.add(entry.getKey());
        }
    }

    public CompletableFuture<Boolean> reloadCyclesFromStorage() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StorageSnapshot snapshot = storage.loadSync();
                boolean changed = false;
                for (CycleState remote : snapshot.cycles().values()) {
                    CycleState local = cycles.get(remote.name());
                    if (local == null || !local.instanceId().equals(remote.instanceId())) {
                        cycles.put(remote.name(), remote);
                        for (Map.Entry<UUID, CachedPlayer> entry : players.entrySet()) {
                            entry.getValue().data().removeCycle(remote.name(), remote.instanceId());
                            if (remote.name().equals("seasonal") && alignSeasonProfile(entry.getValue().data())) dirtyPlayers.add(entry.getKey());
                        }
                        changed = true;
                    }
                }
                if (changed) cacheGeneration.incrementAndGet();
                return changed;
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Could not reload shared cycle states", exception);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> acquireCycleLock(String cycleName, long expiresAt) {
        return storage.acquireLock("cycle-reset:" + cycleName, config.serverId(), expiresAt);
    }
    public void releaseCycleLock(String cycleName) { storage.releaseLock("cycle-reset:" + cycleName, config.serverId()); }

    public void markDirty(UUID uuid, boolean immediate) {
        dirtyPlayers.add(uuid);
        CachedPlayer cached = players.get(uuid);
        if (cached != null) cached.touch();
        if (immediate) savePlayerAsync(uuid);
    }

    public void savePlayerAsync(UUID uuid) {
        CachedPlayer cached = players.get(uuid);
        if (cached == null) {
            dirtyPlayers.remove(uuid);
            return;
        }
        dirtyPlayers.remove(uuid);
        PlayerQuestSnapshot snapshot = cached.data().snapshot();
        saveChains.compute(uuid, (ignored, previous) -> {
            CompletableFuture<Void> base = previous == null
                    ? CompletableFuture.completedFuture(null)
                    : previous.handle((result, throwable) -> null);
            CompletableFuture<Void> next = base.thenCompose(result -> storage.persistPlayerAsync(uuid, snapshot));
            next.whenComplete((result, throwable) -> {
                if (throwable != null) dirtyPlayers.add(uuid);
                saveChains.remove(uuid, next);
            });
            return next;
        });
    }

    public void flushDirtyAsync() {
        for (UUID uuid : List.copyOf(dirtyPlayers)) savePlayerAsync(uuid);
    }

    public void markCompletedFromReward(UUID uuid, String cycleName, String cycleId, String questId) {
        CachedPlayer cached = players.get(uuid);
        if (cached == null) return;
        cached.data().markCompleted(cycleName, cycleId, questId);
        cached.touch();
        dirtyPlayers.add(uuid);
        savePlayerAsync(uuid);
    }

    public CompletableFuture<RewardClaim> createRewardClaim(UUID uuid, String questId, String cycleName,
                                                             String cycleId, List<String> commands) {
        return storage.createOrGetRewardClaimAsync(uuid, questId, cycleName, cycleId, commands);
    }
    public CompletableFuture<List<RewardClaim>> pendingRewards(int limit) { return storage.pendingRewardClaimsAsync(limit); }
    public CompletableFuture<List<RewardClaim>> pendingRewards(UUID uuid, int limit) { return storage.pendingRewardClaimsAsync(uuid, limit); }
    public CompletableFuture<Void> markRewardAttempt(String claimId) { return storage.markRewardAttemptAsync(claimId); }
    public CompletableFuture<Void> advanceRewardCommand(String claimId, int nextCommandIndex) { return storage.advanceRewardCommandAsync(claimId, nextCommandIndex); }
    public CompletableFuture<Void> finalizeRewardSuccess(RewardClaim claim, boolean currentCycle) { return storage.finalizeRewardSuccessAsync(claim, currentCycle); }

    public Map<BlockKey, Long> loadPlacedBlocks(long notBefore) throws Exception { return storage.loadPlacedBlocksBlocking(notBefore); }
    public void savePlacedBlock(BlockKey key, long timestamp) { storage.persistPlacedBlockAsync(key, timestamp); }
    public void deletePlacedBlock(BlockKey key) { storage.deletePlacedBlockAsync(key); }
    public void purgePlacedBlocks(long before) { storage.purgePlacedBlocksAsync(before); }
    public void purgeDeliveredRewards(long before) { storage.purgeDeliveredRewardsAsync(before); }
    public void purgeCycleHistory(Map<String, Long> cutoffs) { storage.purgeCycleHistoryAsync(cutoffs); }
    public void purgeNetworkEvents(long before) { storage.purgeNetworkEventsAsync(before); }

    public CompletableFuture<List<DatabaseStorage.NetworkEvent>> networkEventsAfter(long timestamp, String eventId) {
        return storage.networkEventsAfter(timestamp, eventId);
    }
    public void setNetworkNotifier(BiConsumer<String, String> notifier) { networkNotifier = notifier == null ? (type, payload) -> { } : notifier; }
    public CompletableFuture<Void> publishNetworkEvent(String type, String payload) {
        return storage.publishNetworkEvent(type, payload).thenRun(() -> networkNotifier.accept(type, payload));
    }

    public void removeQuestEverywhere(String questId) {
        for (CachedPlayer cached : players.values()) cached.data().removeQuest(questId);
        storage.deleteQuestAsync(questId);
    }

    public void shutdown() {
        flushDirtyAsync();
        try {
            CompletableFuture<?>[] pending = saveChains.values().toArray(CompletableFuture[]::new);
            if (pending.length > 0) CompletableFuture.allOf(pending).get(15, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Timed out while waiting for ordered player saves", exception);
        }
        storage.flush();
        for (UUID uuid : List.copyOf(dirtyPlayers)) {
            CachedPlayer cached = players.get(uuid);
            if (cached == null) continue;
            try {
                storage.persistPlayerSync(uuid, cached.data().snapshot());
                dirtyPlayers.remove(uuid);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not persist final quest snapshot for " + uuid, exception);
            }
        }
        storage.close();
    }

    private boolean alignSeasonProfile(PlayerQuestData playerData) {
        String seasonalCycleId = cycleId("seasonal");
        if (seasonalCycleId.isBlank()) return false;
        PlayerProfile current = playerData.profile();
        PlayerProfile aligned = current.alignSeason(seasonalCycleId);
        if (aligned.equals(current)) return false;
        playerData.setProfile(aligned);
        return true;
    }

    private void migrateLegacyDataOnce(CycleState state) {
        if (legacyDataChecked || !state.name().equals("daily")) return;
        legacyDataChecked = true;
        File file = PluginPaths.legacyDataFile(plugin);
        if (!file.exists()) return;
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = yaml.getConfigurationSection("players");
            if (section != null) {
                for (String uuidText : section.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidText);
                        PlayerQuestData player = new PlayerQuestData();
                        String base = "players." + uuidText;
                        Map<String, Integer> progress = new HashMap<>();
                        ConfigurationSection progressSection = yaml.getConfigurationSection(base + ".progress");
                        if (progressSection != null) for (String id : progressSection.getKeys(false)) progress.put(id, progressSection.getInt(id));
                        long now = System.currentTimeMillis();
                        for (String id : yaml.getStringList(base + ".active")) {
                            player.loadEntry(new QuestProgressEntry("daily", state.instanceId(), id,
                                    QuestProgressEntry.Status.ACTIVE, progress.getOrDefault(id, 0), now, 0L));
                        }
                        for (String id : yaml.getStringList(base + ".completed")) {
                            player.loadEntry(new QuestProgressEntry("daily", state.instanceId(), id,
                                    QuestProgressEntry.Status.COMPLETED, 0, now, now));
                        }
                        storage.persistPlayerSync(uuid, player.snapshot());
                    } catch (IllegalArgumentException ignored) { }
                }
            }
            Files.move(file.toPath(), PluginPaths.migratedLegacyDataFile(plugin).toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Migrated legacy data/data.yml into the multi-cycle database.");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Could not migrate legacy data/data.yml", exception);
        }
    }

    private static final class CachedPlayer {
        private final PlayerQuestData data;
        private volatile long lastAccess;
        private CachedPlayer(PlayerQuestData data, long lastAccess) { this.data = data; this.lastAccess = lastAccess; }
        private PlayerQuestData data() { return data; }
        private long lastAccess() { return lastAccess; }
        private void touch() { lastAccess = System.currentTimeMillis(); }
    }
}
