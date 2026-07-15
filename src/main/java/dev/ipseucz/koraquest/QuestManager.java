package dev.ipseucz.koraquest;

import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.data.PlayerDataService;
import dev.ipseucz.koraquest.data.PlayerQuestData;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class QuestManager {
    private static final Pattern RANDOM_RANGE = Pattern.compile("\\{random:(-?\\d+)-(-?\\d+)\\}", Pattern.CASE_INSENSITIVE);
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final MessageService messages;
    private final PlayerDataService data;
    private final Object cycleLock = new Object();

    public QuestManager(KoraQuestPlugin plugin, QuestConfig config, MessageService messages, PlayerDataService data) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.data = data;
    }

    public void ensureCycle(boolean broadcast) {
        synchronized (cycleLock) {
            long now = System.currentTimeMillis();
            boolean empty = dailyQuests().isEmpty();
            boolean expired = data.cycleStartedAt() <= 0L || now - data.cycleStartedAt() >= config.resetIntervalMillis();
            if (empty || expired) {
                rotateDaily(now, broadcast);
            }
        }
    }

    public void forceRotate(CommandSender sender) {
        synchronized (cycleLock) {
            rotateDaily(System.currentTimeMillis(), true);
        }
        plugin.sendSafe(sender, "forced-reset", Map.of());
    }

    private void rotateDaily(long now, boolean broadcast) {
        Map<Difficulty, List<String>> selected = config.weightedDailyEnabled() ? selectWeighted() : selectFixed();
        data.replaceCycle(now, selected);
        if (broadcast && config.broadcastReset()) {
            plugin.scheduler().runGlobal(() -> Bukkit.broadcastMessage(messages.format("messages.daily-reset", Map.of())));
        }
    }

    private Map<Difficulty, List<String>> selectFixed() {
        Map<Difficulty, List<String>> selected = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            List<String> ids = questsByDifficulty(difficulty);
            Collections.shuffle(ids);
            int amount = Math.min(ids.size(), config.dailyAmount(difficulty));
            selected.put(difficulty, List.copyOf(ids.subList(0, amount)));
        }
        return selected;
    }

    private Map<Difficulty, List<String>> selectWeighted() {
        Map<Difficulty, List<String>> remaining = new EnumMap<>(Difficulty.class);
        Map<Difficulty, List<String>> selected = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            List<String> ids = questsByDifficulty(difficulty);
            Collections.shuffle(ids);
            remaining.put(difficulty, ids);
            selected.put(difficulty, new ArrayList<>());
        }
        int total = Math.min(config.dailyTotalQuests(), config.quests().size());
        for (int index = 0; index < total; index++) {
            Difficulty rolled = rollAvailableDifficulty(remaining);
            if (rolled == null) {
                break;
            }
            List<String> pool = remaining.get(rolled);
            selected.get(rolled).add(pool.remove(0));
        }
        Map<Difficulty, List<String>> immutable = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            immutable.put(difficulty, List.copyOf(selected.get(difficulty)));
        }
        return immutable;
    }

    private Difficulty rollAvailableDifficulty(Map<Difficulty, List<String>> remaining) {
        int totalWeight = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            if (!remaining.getOrDefault(difficulty, List.of()).isEmpty()) {
                totalWeight += config.dailyWeight(difficulty);
            }
        }
        if (totalWeight <= 0) {
            return remaining.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).map(Map.Entry::getKey).findFirst().orElse(null);
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cursor = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            if (remaining.getOrDefault(difficulty, List.of()).isEmpty()) {
                continue;
            }
            cursor += config.dailyWeight(difficulty);
            if (roll < cursor) {
                return difficulty;
            }
        }
        return null;
    }

    private List<String> questsByDifficulty(Difficulty difficulty) {
        return config.quests().values().stream()
                .filter(quest -> quest.difficulty() == difficulty)
                .map(QuestDefinition::id)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Collection<QuestDefinition> dailyQuests() {
        List<QuestDefinition> result = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            result.addAll(dailyQuests(difficulty));
        }
        result.sort(Comparator.comparing((QuestDefinition quest) -> quest.difficulty().ordinal()).thenComparing(QuestDefinition::id));
        return result;
    }

    public Collection<QuestDefinition> dailyQuests(Difficulty difficulty) {
        List<QuestDefinition> result = new ArrayList<>();
        for (String id : data.dailyQuestIds(difficulty)) {
            QuestDefinition quest = config.quest(id);
            if (quest != null) {
                result.add(quest);
            }
        }
        return result;
    }

    public PlayerQuestData playerData(UUID uuid) {
        return data.player(uuid);
    }

    public long millisUntilReset() {
        long started = data.cycleStartedAt();
        if (started <= 0L) {
            return 0L;
        }
        return Math.max(0L, started + config.resetIntervalMillis() - System.currentTimeMillis());
    }

    public boolean accept(Player player, String questIdInput) {
        QuestDefinition quest = config.quest(questIdInput);
        if (quest == null) {
            messages.send(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
            return false;
        }
        if (!data.isDailyQuest(quest.id())) {
            messages.send(player, "quest-not-daily");
            return false;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        PlayerQuestData.AcceptResult result = playerData.accept(quest.id(), config.maxActiveQuests());
        switch (result) {
            case ALREADY_ACTIVE -> messages.send(player, "quest-already-active");
            case COMPLETED -> messages.send(player, "quest-already-completed");
            case LIMIT -> messages.send(player, "quest-active-limit", Map.of("%limit%", String.valueOf(config.maxActiveQuests())));
            case ACCEPTED -> {
                data.markDirty(player.getUniqueId(), true);
                messages.send(player, "quest-accepted", placeholders(player, quest, 0));
                return true;
            }
        }
        return false;
    }

    public boolean cancel(Player player, String questIdInput) {
        QuestDefinition quest = config.quest(questIdInput);
        if (quest == null) {
            messages.send(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
            return false;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        if (!playerData.cancel(quest.id())) {
            messages.send(player, "quest-not-active");
            return false;
        }
        data.markDirty(player.getUniqueId(), true);
        messages.send(player, "quest-cancelled", placeholders(player, quest, 0));
        return true;
    }

    public boolean complete(Player player, String questIdInput) {
        QuestDefinition quest = config.quest(questIdInput);
        if (quest == null) {
            messages.send(player, "quest-not-found", Map.of("%quest_id%", String.valueOf(questIdInput)));
            return false;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        int progress = playerData.progressOf(quest.id());
        PlayerQuestData.CompleteResult result = playerData.complete(quest.id(), quest.required());
        switch (result) {
            case ALREADY_COMPLETED -> messages.send(player, "quest-already-completed");
            case NOT_ACTIVE -> messages.send(player, "quest-not-active");
            case NOT_READY -> messages.send(player, "quest-not-ready", placeholders(player, quest, progress));
            case COMPLETED -> {
                Map<String, String> completion = placeholders(player, quest, quest.required());
                data.markDirty(player.getUniqueId(), true);
                dispatchRewards(player, quest, completion);
                messages.send(player, "quest-completed", completion);
                return true;
            }
        }
        return false;
    }

    private void dispatchRewards(Player player, QuestDefinition quest, Map<String, String> placeholders) {
        List<String> commands = quest.rewards();
        if (commands.isEmpty()) {
            return;
        }
        plugin.scheduler().runGlobal(() -> {
            boolean failed = false;
            for (String raw : commands) {
                String command = prepareRewardCommand(raw, placeholders);
                if (command.isBlank() || command.startsWith("#")) {
                    continue;
                }
                if (command.startsWith("/")) {
                    command = command.substring(1).trim();
                }
                try {
                    if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                        failed = true;
                        plugin.getLogger().warning("Reward command returned false for quest '" + quest.id() + "': " + command);
                    }
                } catch (RuntimeException exception) {
                    failed = true;
                    plugin.getLogger().warning("Reward command failed for quest '" + quest.id() + "': " + exception.getMessage());
                }
            }
            if (failed && player.isOnline()) {
                plugin.scheduler().runEntity(player, () -> messages.send(player, "reward-command-failed"));
            }
        });
    }

    private String prepareRewardCommand(String raw, Map<String, String> placeholders) {
        String command = raw == null ? "" : raw.trim();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            command = command.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        Matcher matcher = RANDOM_RANGE.matcher(command);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            long first;
            long second;
            try {
                first = Long.parseLong(matcher.group(1));
                second = Long.parseLong(matcher.group(2));
            } catch (NumberFormatException exception) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            long min = Math.min(first, second);
            long max = Math.max(first, second);
            long rolled = rollInclusive(min, max);
            matcher.appendReplacement(output, Matcher.quoteReplacement(String.valueOf(rolled)));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private long rollInclusive(long min, long max) {
        if (min == max) {
            return min;
        }
        long range = max - min + 1L;
        if (range > 0L) {
            return min + ThreadLocalRandom.current().nextLong(range);
        }
        // Overflow means the requested interval is larger than Long.MAX_VALUE.
        // Rejection sampling remains unbiased for that uncommon configuration.
        long value;
        do {
            value = ThreadLocalRandom.current().nextLong();
        } while (value < min || value > max);
        return value;
    }

    public void increment(Player player, ObjectiveType type, String target, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        boolean changed = false;
        for (String questId : playerData.activeIds()) {
            QuestDefinition quest = config.quest(questId);
            if (quest == null || quest.type() != type || !matches(quest.target(), target)) {
                continue;
            }
            PlayerQuestData.ProgressUpdate update = playerData.increment(quest.id(), amount, quest.required());
            if (!update.changed()) {
                continue;
            }
            changed = true;
            if (update.becameReady()) {
                messages.send(player, "quest-ready", placeholders(player, quest, update.progress()));
            } else if (config.sendProgressMessages() && shouldSendProgress(update.oldProgress(), update.progress(), quest.required())) {
                messages.send(player, "quest-progress", placeholders(player, quest, update.progress()));
            }
        }
        if (changed) {
            data.markDirty(player.getUniqueId(), false);
        }
    }

    public boolean progressById(Player player, String questIdInput, int amount) {
        QuestDefinition quest = config.quest(questIdInput);
        if (quest == null || amount <= 0) {
            return false;
        }
        PlayerQuestData playerData = data.player(player.getUniqueId());
        PlayerQuestData.ProgressUpdate update = playerData.increment(quest.id(), amount, quest.required());
        if (!update.changed()) {
            return false;
        }
        data.markDirty(player.getUniqueId(), false);
        if (update.becameReady()) {
            messages.send(player, "quest-ready", placeholders(player, quest, update.progress()));
        } else if (config.sendProgressMessages() && shouldSendProgress(update.oldProgress(), update.progress(), quest.required())) {
            messages.send(player, "quest-progress", placeholders(player, quest, update.progress()));
        }
        return true;
    }

    private boolean shouldSendProgress(int oldValue, int newValue, int required) {
        if (required <= 0) {
            return false;
        }
        int step = config.progressPercentStep();
        int oldPercent = (int) Math.floor(oldValue * 100.0D / required);
        int newPercent = (int) Math.floor(newValue * 100.0D / required);
        return oldPercent / step < newPercent / step;
    }

    private boolean matches(String configured, String actual) {
        if (configured == null || configured.equalsIgnoreCase("ANY")) {
            return true;
        }
        return actual != null && configured.equalsIgnoreCase(actual);
    }

    public Map<String, String> placeholders(Player player, QuestDefinition quest, int progress) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("%player%", player.getName());
        map.put("%uuid%", player.getUniqueId().toString());
        map.put("%quest_id%", quest.id());
        map.put("%quest_name%", messages.questName(quest));
        map.put("%difficulty%", quest.difficulty().key());
        map.put("%difficulty_name%", messages.difficultyName(quest.difficulty()));
        map.put("%progress%", String.valueOf(progress));
        map.put("%required%", String.valueOf(quest.required()));
        return map;
    }

    public void removeQuest(String id) {
        data.removeQuestEverywhere(id.toLowerCase(Locale.ROOT));
    }
}
