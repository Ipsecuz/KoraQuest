package dev.ipseucz.koraquest.config;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
import dev.ipseucz.koraquest.util.PluginPaths;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class QuestConfig {
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9_-]{2,64}");
    private final KoraQuestPlugin plugin;
    private final Map<String, QuestDefinition> quests = new LinkedHashMap<>();
    private final List<ValidationIssue> validationErrors = new ArrayList<>();
    private YamlConfiguration config;
    private YamlConfiguration questsConfig;
    private File questsFile;

    public QuestConfig(KoraQuestPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public synchronized void reload() {
        PluginPaths.prepareLayout(plugin);
        ensureRootResource("config.yml");
        ensureRootResource("quests.yml");
        ensureMessageResource("messages.yml");
        ensureMessageResource("messages_en.yml");
        config = YamlConfiguration.loadConfiguration(PluginPaths.configFile(plugin));
        questsFile = PluginPaths.questsFile(plugin);
        questsConfig = YamlConfiguration.loadConfiguration(questsFile);
        migrateLegacyQuestText();
        validationErrors.clear();
        migrateLegacyRewardProfiles();
        loadQuests();
    }

    private void ensureRootResource(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }

    private void ensureMessageResource(String name) {
        File file = PluginPaths.messageFile(plugin, name);
        if (!file.exists()) {
            plugin.saveResource("message/" + name, false);
        }
    }

    private void loadQuests() {
        quests.clear();
        ConfigurationSection root = questsConfig.getConfigurationSection("quests");
        if (root == null) {
            validationErrors.add(new ValidationIssue("missing-quests-section", Map.of()));
            return;
        }
        for (String rawId : root.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            String path = "quests." + rawId;
            if (!SAFE_ID.matcher(id).matches()) {
                validationErrors.add(new ValidationIssue("invalid-quest-id", Map.of("%quest_id%", rawId)));
                continue;
            }
            try {
                Difficulty difficulty = Difficulty.from(questsConfig.getString(path + ".difficulty"));
                ObjectiveType type = ObjectiveType.from(questsConfig.getString(path + ".type"));
                String target = type.normalizeTarget(questsConfig.getString(path + ".target", "ANY"));
                if (!type.validTarget(target)) {
                    throw new IllegalArgumentException("invalid target '" + target + "' for type " + type);
                }
                int required = questsConfig.getInt(path + ".required", 1);
                if (required <= 0) {
                    throw new IllegalArgumentException("required must be greater than 0");
                }
                Material icon = material(questsConfig.getString(path + ".material"), Material.BOOK);
                if (questsConfig.contains(path + ".rewards") && !questsConfig.isList(path + ".rewards")) {
                    throw new IllegalArgumentException("rewards must be a YAML list of console commands");
                }
                quests.put(id, new QuestDefinition(
                        id,
                        difficulty,
                        type,
                        target,
                        required,
                        icon,
                        requireTitle(path, id),
                        List.copyOf(questsConfig.getStringList(path + ".lore")),
                        List.copyOf(questsConfig.getStringList(path + ".rewards"))
                ));
            } catch (RuntimeException exception) {
                validationErrors.add(new ValidationIssue("invalid-quest", Map.of("%quest_id%", rawId)));
                plugin.getLogger().warning("Invalid quest '" + rawId + "': " + exception.getMessage());
            }
        }
    }

    private String requireTitle(String path, String id) {
        String title = questsConfig.getString(path + ".title", "");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is missing or blank");
        }
        return title;
    }

    /**
     * Migrates the old name-key/lore-key layout once so existing installations keep
     * their custom quest text after updating. New versions only read title/lore
     * directly from quests.yml.
     */
    private void migrateLegacyQuestText() {
        ConfigurationSection root = questsConfig.getConfigurationSection("quests");
        if (root == null) {
            return;
        }
        String languageName = languageFile();
        YamlConfiguration activeLanguage = YamlConfiguration.loadConfiguration(
                PluginPaths.resolveMessageFile(plugin, languageName == null ? "messages.yml" : languageName));
        YamlConfiguration defaultLanguage = YamlConfiguration.loadConfiguration(
                PluginPaths.messageFile(plugin, "messages.yml"));
        boolean changed = false;
        for (String id : root.getKeys(false)) {
            String path = "quests." + id;
            if (!questsConfig.contains(path + ".title")) {
                String title = questsConfig.getString(path + ".name");
                String nameKey = questsConfig.getString(path + ".name-key");
                if ((title == null || title.isBlank()) && nameKey != null) {
                    title = activeLanguage.getString(nameKey);
                    if (title == null || title.isBlank()) {
                        title = defaultLanguage.getString(nameKey);
                    }
                }
                questsConfig.set(path + ".title", title == null || title.isBlank() ? "&#FFFFFF" + id : title);
                changed = true;
            }
            if (!questsConfig.contains(path + ".lore")) {
                String loreKey = questsConfig.getString(path + ".lore-key");
                List<String> lore = loreKey == null ? List.of() : activeLanguage.getStringList(loreKey);
                if (lore.isEmpty() && loreKey != null) {
                    lore = defaultLanguage.getStringList(loreKey);
                }
                questsConfig.set(path + ".lore", lore.isEmpty()
                        ? List.of("&#8A8A8AChỉnh sửa mô tả nhiệm vụ tại quests.yml")
                        : lore);
                changed = true;
            }
            if (questsConfig.contains(path + ".name-key")) {
                questsConfig.set(path + ".name-key", null);
                changed = true;
            }
            if (questsConfig.contains(path + ".lore-key")) {
                questsConfig.set(path + ".lore-key", null);
                changed = true;
            }
            if (questsConfig.contains(path + ".name")) {
                questsConfig.set(path + ".name", null);
                changed = true;
            }
        }
        if (changed) {
            saveQuests();
            plugin.getLogger().info("Migrated legacy quest name/lore fields to quests.yml title/lore.");
        }
    }

    /**
     * Migrates the previous reward-profile layout into per-quest console commands.
     * The old structured bonus system is intentionally not retained because rewards
     * are now plain custom commands owned entirely by each quest.
     */
    private void migrateLegacyRewardProfiles() {
        ConfigurationSection root = questsConfig.getConfigurationSection("quests");
        if (root == null) {
            return;
        }
        boolean changed = false;
        for (String id : root.getKeys(false)) {
            String path = "quests." + id;
            if (!questsConfig.contains(path + ".rewards")) {
                String profile = questsConfig.getString(path + ".reward-profile", "").toLowerCase(Locale.ROOT);
                List<String> commands = legacyProfileCommands(profile);
                questsConfig.set(path + ".rewards", commands);
                changed = true;
            }
            if (questsConfig.contains(path + ".reward-profile")) {
                questsConfig.set(path + ".reward-profile", null);
                changed = true;
            }
        }
        if (changed) {
            saveQuests();
            plugin.getLogger().info("Migrated legacy reward-profile fields to per-quest rewards command lists.");
        }
    }

    private List<String> legacyProfileCommands(String profile) {
        String base = "reward-profiles." + profile;
        if (profile.isBlank() || config.getConfigurationSection(base) == null) {
            return List.of();
        }
        long min = config.getLong(base + ".money-min", 0L);
        long max = config.getLong(base + ".money-max", min);
        long mobcoin = config.getLong(base + ".mobcoin", 0L);
        List<String> result = new ArrayList<>();
        for (String command : config.getStringList(base + ".commands")) {
            String migrated = command
                    .replace("%money%", min == max ? String.valueOf(min) : "{random:" + Math.min(min, max) + "-" + Math.max(min, max) + "}")
                    .replace("%mobcoin%", String.valueOf(mobcoin));
            result.add(migrated);
        }
        return List.copyOf(result);
    }

    public synchronized CreateResult createQuest(String idInput, Difficulty difficulty, ObjectiveType type, String target,
                                                  int required, Material icon) {
        String id = idInput == null ? "" : idInput.toLowerCase(Locale.ROOT);
        if (!SAFE_ID.matcher(id).matches()) {
            return CreateResult.INVALID_ID;
        }
        if (questsConfig.contains("quests." + id)) {
            return CreateResult.EXISTS;
        }
        String normalizedTarget = type.normalizeTarget(target);
        if (!type.validTarget(normalizedTarget) || required <= 0) {
            return CreateResult.INVALID_TARGET;
        }
        String path = "quests." + id;
        questsConfig.set(path + ".difficulty", difficulty.key());
        questsConfig.set(path + ".type", type.name());
        questsConfig.set(path + ".target", normalizedTarget);
        questsConfig.set(path + ".required", required);
        questsConfig.set(path + ".material", (icon == null ? Material.BOOK : icon).name());
        questsConfig.set(path + ".title", "&#FFFFFF" + id);
        questsConfig.set(path + ".lore", List.of(
                "&#8A8A8ALoại: &f" + type.name(),
                "&#8A8A8AMục tiêu: &f" + normalizedTarget,
                "&#8A8A8AYêu cầu: &f" + required,
                "&#75FF75Hãy chỉnh phần thưởng tại rewards bên dưới."
        ));
        questsConfig.set(path + ".rewards", List.of());
        saveQuests();
        loadQuests();
        return CreateResult.CREATED;
    }

    public synchronized boolean deleteQuest(String idInput) {
        String id = idInput == null ? "" : idInput.toLowerCase(Locale.ROOT);
        if (!questsConfig.contains("quests." + id)) {
            return false;
        }
        questsConfig.set("quests." + id, null);
        saveQuests();
        loadQuests();
        return true;
    }

    private void saveQuests() {
        try {
            questsConfig.save(questsFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save quests.yml", exception);
        }
    }

    private Material material(String input, Material fallback) {
        Material material = Material.matchMaterial(input == null ? "" : input.trim().toUpperCase(Locale.ROOT));
        return material == null || material == Material.AIR ? fallback : material;
    }

    public synchronized YamlConfiguration raw() {
        return config;
    }

    public synchronized Map<String, QuestDefinition> quests() {
        return Map.copyOf(quests);
    }

    public synchronized QuestDefinition quest(String id) {
        return id == null ? null : quests.get(id.toLowerCase(Locale.ROOT));
    }

    public synchronized List<ValidationIssue> validationErrors() {
        return List.copyOf(validationErrors);
    }

    public synchronized String languageFile() {
        String value = config.getString("language-file", "messages.yml");
        if (value == null || !value.matches("[A-Za-z0-9_.-]+\\.yml")) {
            return "messages.yml";
        }
        return value;
    }

    public synchronized boolean metricsEnabled() {
        return config.contains("metric") ? config.getBoolean("metric", true) : config.getBoolean("metrics", true);
    }

    public synchronized long resetIntervalMillis() {
        return Math.max(1L, config.getLong("settings.reset-interval-hours", 24L)) * 3_600_000L;
    }

    public synchronized int maxActiveQuests() {
        return Math.max(1, config.getInt("settings.max-active-quests-per-player", 3));
    }

    public synchronized boolean broadcastReset() {
        return config.getBoolean("settings.broadcast-daily-reset", true);
    }

    public synchronized boolean sendProgressMessages() {
        return config.getBoolean("settings.send-progress-messages", true);
    }

    public synchronized int progressPercentStep() {
        return Math.max(1, Math.min(100, config.getInt("settings.progress-message-percent-step", 10)));
    }

    public synchronized long databaseFlushTicks() {
        return Math.max(20L, config.getLong("settings.database-flush-ticks", 100L));
    }

    public synchronized boolean weightedDailyEnabled() {
        return config.getBoolean("daily-random.enabled", true);
    }

    public synchronized int dailyTotalQuests() {
        return Math.max(1, config.getInt("daily-random.total-quests", 12));
    }

    public synchronized int dailyWeight(Difficulty difficulty) {
        return Math.max(0, config.getInt("daily-random.weights." + difficulty.key(), switch (difficulty) {
            case EASY -> 50;
            case MEDIUM -> 30;
            case HARD -> 20;
        }));
    }

    public synchronized int dailyChancePercent(Difficulty difficulty) {
        int total = 0;
        for (Difficulty value : Difficulty.values()) {
            total += dailyWeight(value);
        }
        return total <= 0 ? 0 : (int) Math.round(dailyWeight(difficulty) * 100.0D / total);
    }

    public synchronized int dailyAmount(Difficulty difficulty) {
        return Math.max(0, config.getInt("daily-pool." + difficulty.key(), 3));
    }

    public synchronized int guiSize() {
        int size = Math.max(9, Math.min(54, config.getInt("gui.size", 54)));
        return ((size + 8) / 9) * 9;
    }

    public synchronized List<Integer> questSlots() {
        List<Integer> result = config.getIntegerList("gui.quest-slots");
        return result.isEmpty() ? List.of(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43) : result;
    }

    public synchronized int guiSlot(String path, int fallback) {
        return config.getInt(path + ".slot", fallback);
    }

    public synchronized Material guiMaterial(String path, Material fallback) {
        return material(config.getString(path + ".material"), fallback);
    }

    public synchronized Material fillerMaterial() {
        return material(config.getString("gui.filler-material"), Material.BLACK_STAINED_GLASS_PANE);
    }

    public synchronized boolean updateCheckerEnabled() {
        return config.getBoolean("update-checker.enabled", true);
    }

    public synchronized int spigotResourceId() {
        return Math.max(0, config.getInt("update-checker.resource-id", 0));
    }

    public synchronized boolean notifyConsoleUpdate() {
        return config.getBoolean("update-checker.notify-console", true);
    }

    public synchronized String updateNotifyPermission() {
        return config.getString("update-checker.notify-permission", "koraquest.update-notify");
    }

    public record ValidationIssue(String key, Map<String, String> placeholders) {
        public ValidationIssue {
            placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
        }
    }

    public enum CreateResult {
        CREATED,
        EXISTS,
        INVALID_ID,
        INVALID_TARGET
    }
}
