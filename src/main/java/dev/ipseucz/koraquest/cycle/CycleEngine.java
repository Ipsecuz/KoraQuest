package dev.ipseucz.koraquest.cycle;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.data.PlayerDataService;
import dev.ipseucz.koraquest.event.QuestCycleResetEvent;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.QuestDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class CycleEngine {
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final MessageService messages;
    private final PlayerDataService data;
    private final Set<String> rotationsInFlight = ConcurrentHashMap.newKeySet();

    public CycleEngine(KoraQuestPlugin plugin, QuestConfig config, MessageService messages, PlayerDataService data) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.data = data;
    }

    public void ensureAll(boolean broadcast) {
        long now = System.currentTimeMillis();
        for (CycleSettings settings : config.cycles().values()) {
            if (!settings.enabled()) continue;
            CycleState state = data.cycle(settings.name());
            boolean hasConfiguredQuests = config.quests().values().stream()
                    .anyMatch(quest -> quest.cycle().equalsIgnoreCase(settings.name()));
            if (state == null || (hasConfiguredQuests && state.selectedQuests().values().stream().allMatch(List::isEmpty))
                    || state.nextResetAt() <= now) {
                requestRotation(settings, broadcast, false, null);
            }
        }
    }

    public void forceRotate(String cycleName, CommandSender sender) {
        CycleSettings settings = config.cycle(cycleName);
        if (settings == null || !settings.enabled()) {
            plugin.sendSafe(sender, "cycle-not-found", Map.of("%cycle%", String.valueOf(cycleName)));
            return;
        }
        requestRotation(settings, true, true, sender);
    }

    private void requestRotation(CycleSettings settings, boolean broadcast, boolean forced, CommandSender sender) {
        if (!rotationsInFlight.add(settings.name())) {
            if (sender != null) plugin.sendSafe(sender, "cycle-reset-processing", Map.of("%cycle%", settings.name()));
            return;
        }
        if (!config.networkSyncEnabled()) {
            plugin.scheduler().runGlobal(() -> rotate(settings, broadcast, forced, sender));
            return;
        }
        long expiresAt = System.currentTimeMillis() + config.distributedLockMillis();
        data.acquireCycleLock(settings.name(), expiresAt).whenComplete((acquired, throwable) -> {
            if (throwable != null || !Boolean.TRUE.equals(acquired)) {
                rotationsInFlight.remove(settings.name());
                data.reloadCyclesFromStorage();
                if (sender != null) plugin.sendSafe(sender, "cycle-reset-locked", Map.of("%cycle%", settings.name()));
                return;
            }
            data.reloadCyclesFromStorage().whenComplete((ignored, reloadError) -> plugin.scheduler().runGlobal(() -> {
                try {
                    CycleState current = data.cycle(settings.name());
                    if (forced || current == null || current.nextResetAt() <= System.currentTimeMillis()) {
                        rotate(settings, broadcast, forced, sender);
                    } else {
                        rotationsInFlight.remove(settings.name());
                        data.releaseCycleLock(settings.name());
                    }
                } catch (Throwable throwable1) {
                    rotationsInFlight.remove(settings.name());
                    data.releaseCycleLock(settings.name());
                    plugin.getLogger().warning("Could not rotate cycle " + settings.name() + ": " + throwable1.getMessage());
                }
            }));
        });
    }

    private void rotate(CycleSettings settings, boolean broadcast, boolean forced, CommandSender sender) {
        long now = System.currentTimeMillis();
        Map<Difficulty, List<String>> selected;
        CycleState state;
        try {
            selected = select(settings);
            long nextReset = config.nextReset(settings, now);
            state = new CycleState(settings.name(), settings.name() + ":" + now, now, nextReset, selected);
        } catch (Throwable throwable) {
            finishRotation(settings);
            plugin.getLogger().warning("Could not prepare cycle " + settings.name() + ": " + throwable.getMessage());
            if (sender != null) plugin.sendSafe(sender, "cycle-reset-failed", Map.of("%cycle%", settings.displayName()));
            return;
        }

        data.replaceCycle(state).whenComplete((ignored, throwable) -> plugin.scheduler().runGlobal(() -> {
            try {
                if (throwable != null) {
                    plugin.getLogger().warning("Could not persist cycle " + settings.name() + ": " + throwable.getMessage());
                    if (sender != null) plugin.sendSafe(sender, "cycle-reset-failed", Map.of("%cycle%", settings.displayName()));
                    return;
                }
                Bukkit.getPluginManager().callEvent(new QuestCycleResetEvent(settings.name(), now));
                if (broadcast && config.broadcastReset()) {
                    Bukkit.broadcastMessage(messages.format("messages.cycle-reset", Map.of(
                            "%cycle%", settings.displayName(),
                            "%amount%", String.valueOf(selected.values().stream().mapToInt(List::size).sum())
                    )));
                }
                if (sender != null) plugin.sendSafe(sender, "forced-reset", Map.of("%cycle%", settings.displayName()));
            } finally {
                finishRotation(settings);
            }
        }));
    }

    private void finishRotation(CycleSettings settings) {
        rotationsInFlight.remove(settings.name());
        if (config.networkSyncEnabled()) data.releaseCycleLock(settings.name());
    }

    private Map<Difficulty, List<String>> select(CycleSettings settings) {
        Map<Difficulty, List<String>> candidates = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            List<String> ids = config.quests().values().stream()
                    .filter(quest -> quest.cycle().equalsIgnoreCase(settings.name()))
                    .filter(quest -> quest.difficulty() == difficulty)
                    .map(QuestDefinition::id)
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(ids);
            candidates.put(difficulty, ids);
        }
        if (settings.selectionMode() == CycleSettings.SelectionMode.ALL) {
            Map<Difficulty, List<String>> result = new EnumMap<>(Difficulty.class);
            for (Difficulty difficulty : Difficulty.values()) result.put(difficulty, List.copyOf(candidates.get(difficulty)));
            return Map.copyOf(result);
        }
        if (settings.selectionMode() == CycleSettings.SelectionMode.WEIGHTED) return selectWeighted(settings, candidates);
        Map<Difficulty, List<String>> result = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            List<String> ids = candidates.get(difficulty);
            result.put(difficulty, List.copyOf(ids.subList(0, Math.min(ids.size(), settings.amount(difficulty)))));
        }
        return Map.copyOf(result);
    }

    private Map<Difficulty, List<String>> selectWeighted(CycleSettings settings, Map<Difficulty, List<String>> remaining) {
        Map<Difficulty, List<String>> selected = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) selected.put(difficulty, new ArrayList<>());
        int available = remaining.values().stream().mapToInt(List::size).sum();
        int total = settings.totalQuests() <= 0 ? available : Math.min(available, settings.totalQuests());
        for (int index = 0; index < total; index++) {
            Difficulty difficulty = roll(remaining, settings);
            if (difficulty == null) break;
            selected.get(difficulty).add(remaining.get(difficulty).remove(0));
        }
        Map<Difficulty, List<String>> result = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) result.put(difficulty, List.copyOf(selected.get(difficulty)));
        return Map.copyOf(result);
    }

    private Difficulty roll(Map<Difficulty, List<String>> remaining, CycleSettings settings) {
        int totalWeight = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            if (!remaining.getOrDefault(difficulty, List.of()).isEmpty()) totalWeight += settings.weight(difficulty);
        }
        if (totalWeight <= 0) {
            return remaining.entrySet().stream().filter(entry -> !entry.getValue().isEmpty())
                    .sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
        }
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cursor = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            if (remaining.getOrDefault(difficulty, List.of()).isEmpty()) continue;
            cursor += settings.weight(difficulty);
            if (random < cursor) return difficulty;
        }
        return null;
    }

    public long millisUntilReset(String cycleName) {
        CycleState state = data.cycle(cycleName);
        if (state == null || state.nextResetAt() == Long.MAX_VALUE) return 0L;
        return Math.max(0L, state.nextResetAt() - System.currentTimeMillis());
    }

    public CycleState state(String cycleName) { return data.cycle(cycleName); }
}
