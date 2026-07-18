package dev.ipseucz.koraquest.config;

import dev.ipseucz.koraquest.KoraQuestPlugin;
<<<<<<< HEAD
import dev.ipseucz.koraquest.cycle.CycleSettings;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
import dev.ipseucz.koraquest.model.QuestFilters;
=======
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
import dev.ipseucz.koraquest.util.PluginPaths;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
<<<<<<< HEAD
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
=======
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b

public final class QuestConfig {
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9_-]{2,64}");
    private final KoraQuestPlugin plugin;
    private final Map<String, QuestDefinition> quests = new LinkedHashMap<>();
<<<<<<< HEAD
    private final Map<String, CycleSettings> cycles = new LinkedHashMap<>();
=======
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
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
<<<<<<< HEAD
        applyBundledConfigurationDefaults();
        questsFile = PluginPaths.questsFile(plugin);
        questsConfig = YamlConfiguration.loadConfiguration(questsFile);
        validationErrors.clear();
        installBundledQuestPacks();
        migrateLegacyQuestText();
        migrateLegacyRewardProfiles();
        loadCycles();
        loadQuests();
    }

    /**
     * Migrates only values that exactly match KoraQuest's old bundled defaults.
     * Customized server values are preserved, while existing installations gain
     * the official Spigot resource ID and the new per-cycle quest limits.
     */
    private void applyBundledConfigurationDefaults() {
        boolean changed = false;
        if (!config.contains("update-checker.resource-id") || config.getInt("update-checker.resource-id", 0) <= 0) {
            config.set("update-checker.resource-id", 137091);
            changed = true;
        }

        int schema = config.getInt("settings.config-schema-version", 0);
        if (schema < 110) {
            changed |= replaceIntDefault("settings.max-active-quests-per-player", 6, 45);
            changed |= migrateCycleDefaults("daily", 3, 15, "WEIGHTED", 3, "WEIGHTED", 15);
            changed |= migrateCycleDefaults("weekly", 2, 10, "FIXED", 2, "WEIGHTED", 10);
            changed |= migrateCycleDefaults("monthly", 1, 10, "FIXED", 1, "WEIGHTED", 10);
            changed |= migrateCycleDefaults("seasonal", 1, 10, "ALL", 0, "WEIGHTED", 10);
            config.set("settings.config-schema-version", 110);
            changed = true;
        }

        if (!changed) return;
        try {
            config.save(PluginPaths.configFile(plugin));
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save KoraQuest 1.1 config migration: " + exception.getMessage());
        }
    }

    private boolean replaceIntDefault(String path, int oldValue, int newValue) {
        if (config.getInt(path, oldValue) != oldValue) return false;
        config.set(path, newValue);
        return true;
    }

    private boolean migrateCycleDefaults(String cycle, int oldActiveLimit, int newActiveLimit,
                                         String oldMode, int oldTotal, String newMode, int newTotal) {
        boolean changed = replaceIntDefault("cycles." + cycle + ".active-limit", oldActiveLimit, newActiveLimit);
        String modePath = "cycles." + cycle + ".selection.mode";
        String totalPath = "cycles." + cycle + ".selection.total-quests";
        if (oldMode.equalsIgnoreCase(config.getString(modePath, oldMode))
                && config.getInt(totalPath, oldTotal) == oldTotal) {
            config.set(modePath, newMode);
            config.set(totalPath, newTotal);
            changed = true;
        }
        return changed;
    }

    private void ensureRootResource(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    private void ensureMessageResource(String name) {
        File file = PluginPaths.messageFile(plugin, name);
<<<<<<< HEAD
        if (!file.exists()) plugin.saveResource("message/" + name, false);
    }

    /**
     * Installs only newly bundled quest packs and never overwrites an existing quest.
     * A version marker prevents deliberately deleted bundled quests from being recreated on every reload.
     */
    private void installBundledQuestPacks() {
        if (!config.getBoolean("settings.install-bundled-quest-updates", true)) return;
        int installedVersion = Math.max(0, config.getInt("settings.bundled-quest-pack-version", 0));
        int newestVersion = installedVersion;
        boolean questsChanged = false;
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("quests.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(reader);
            ConfigurationSection packs = bundled.getConfigurationSection("bundled-packs");
            if (packs == null) return;
            List<Integer> versions = packs.getKeys(false).stream().map(value -> {
                try { return Integer.parseInt(value); }
                catch (NumberFormatException ignored) { return -1; }
            }).filter(value -> value > installedVersion).sorted().toList();
            for (int version : versions) {
                for (String questId : bundled.getStringList("bundled-packs." + version + ".quests")) {
                    String sourcePath = "quests." + questId;
                    if (questsConfig.contains(sourcePath)) continue;
                    ConfigurationSection questSection = bundled.getConfigurationSection(sourcePath);
                    if (questSection == null) continue;
                    for (String key : questSection.getKeys(true)) {
                        if (!questSection.isConfigurationSection(key)) {
                            questsConfig.set(sourcePath + "." + key, questSection.get(key));
                        }
                    }
                    questsChanged = true;
                }
                newestVersion = Math.max(newestVersion, version);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not install bundled quest pack: " + exception.getMessage());
            return;
        }
        if (questsChanged) saveQuests();
        if (newestVersion > installedVersion) {
            config.set("settings.bundled-quest-pack-version", newestVersion);
            try {
                config.save(PluginPaths.configFile(plugin));
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not save bundled quest pack version: " + exception.getMessage());
            }
        }
    }

    private void loadCycles() {
        cycles.clear();
        ConfigurationSection section = config.getConfigurationSection("cycles");
        if (section == null || section.getKeys(false).isEmpty()) {
            cycles.put("daily", defaultDailyCycle());
            validationErrors.add(new ValidationIssue("missing-cycles-section", Map.of()));
            return;
        }
        for (String rawName : section.getKeys(false)) {
            String name = CycleSettings.normalize(rawName);
            String base = "cycles." + rawName;
            try {
                boolean enabled = config.getBoolean(base + ".enabled", true);
                String permission = config.getString(base + ".permission", "");
                int activeLimit = Math.max(1, config.getInt(base + ".active-limit", maxActiveQuests()));
                CycleSettings.SelectionMode mode = CycleSettings.SelectionMode.parse(config.getString(base + ".selection.mode", "FIXED"));
                int total = Math.max(0, config.getInt(base + ".selection.total-quests", 0));
                Map<Difficulty, Integer> amounts = new EnumMap<>(Difficulty.class);
                Map<Difficulty, Integer> weights = new EnumMap<>(Difficulty.class);
                for (Difficulty difficulty : Difficulty.values()) {
                    amounts.put(difficulty, Math.max(0, config.getInt(base + ".selection.amounts." + difficulty.key(), 0)));
                    weights.put(difficulty, Math.max(0, config.getInt(base + ".selection.weights." + difficulty.key(), defaultWeight(difficulty))));
                }
                String resetBase = base + ".reset";
                CycleSettings.ResetType resetType = CycleSettings.ResetType.parse(config.getString(resetBase + ".type", "MANUAL"));
                ZoneId zone = safeZone(config.getString(resetBase + ".timezone", "Asia/Ho_Chi_Minh"));
                DayOfWeek day = safeWeekDay(config.getString(resetBase + ".day", "MONDAY"));
                int monthDay = config.getInt(resetBase + ".day-of-month", 1);
                LocalTime time = safeTime(config.getString(resetBase + ".time", "00:00"));
                long intervalMillis = Math.max(1L, config.getLong(resetBase + ".interval-hours", 24L)) * 3_600_000L;
                long retention = Math.max(1L, config.getLong(base + ".history-retention-days", 90L)) * 86_400_000L;
                String displayName = config.getString(base + ".display-name", name);
                CycleSettings settings = new CycleSettings(name, enabled, permission, activeLimit, mode, total,
                        amounts, weights, resetType, config.getString(resetBase + ".expression", ""), zone,
                        day, monthDay, time, intervalMillis, retention, displayName);
                cycles.put(name, settings);
            } catch (RuntimeException exception) {
                validationErrors.add(new ValidationIssue("invalid-cycle", Map.of("%cycle%", rawName)));
                plugin.getLogger().warning("Invalid cycle '" + rawName + "': " + exception.getMessage());
            }
        }
        if (!cycles.containsKey("daily")) cycles.put("daily", defaultDailyCycle());
    }

    private CycleSettings defaultDailyCycle() {
        Map<Difficulty, Integer> amounts = new EnumMap<>(Difficulty.class);
        amounts.put(Difficulty.EASY, 5);
        amounts.put(Difficulty.MEDIUM, 4);
        amounts.put(Difficulty.HARD, 3);
        Map<Difficulty, Integer> weights = new EnumMap<>(Difficulty.class);
        weights.put(Difficulty.EASY, 50);
        weights.put(Difficulty.MEDIUM, 30);
        weights.put(Difficulty.HARD, 20);
        return new CycleSettings("daily", true, "", 3, CycleSettings.SelectionMode.WEIGHTED, 12,
                amounts, weights, CycleSettings.ResetType.CRON, "0 0 * * *", ZoneId.of("Asia/Ho_Chi_Minh"),
                DayOfWeek.MONDAY, 1, LocalTime.MIDNIGHT, 86_400_000L, 90L * 86_400_000L, "Daily");
    }

    private int defaultWeight(Difficulty difficulty) {
        return switch (difficulty) { case EASY -> 50; case MEDIUM -> 30; case HARD -> 20; };
=======
        if (!file.exists()) {
            plugin.saveResource("message/" + name, false);
        }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
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
<<<<<<< HEAD
                String cycle = CycleSettings.normalize(questsConfig.getString(path + ".cycle", "daily"));
                if (!cycles.containsKey(cycle)) throw new IllegalArgumentException("unknown cycle '" + cycle + "'");
                Difficulty difficulty = Difficulty.from(questsConfig.getString(path + ".difficulty"));
                ObjectiveType type = ObjectiveType.from(questsConfig.getString(path + ".type"));
                String target = type.normalizeTarget(questsConfig.getString(path + ".target", "ANY"));
                if (!type.validTarget(target)) throw new IllegalArgumentException("invalid target '" + target + "' for " + type);
                int required = questsConfig.getInt(path + ".required", 1);
                if (required <= 0) throw new IllegalArgumentException("required must be greater than 0");
                if (questsConfig.contains(path + ".rewards") && !questsConfig.isList(path + ".rewards")) {
                    throw new IllegalArgumentException("rewards must be a YAML list");
                }
                String filterPath = path + ".filters";
                QuestFilters filters = QuestFilters.of(
                        questsConfig.getStringList(filterPath + ".worlds"),
                        questsConfig.getInt(filterPath + ".min-y", Integer.MIN_VALUE),
                        questsConfig.getInt(filterPath + ".max-y", Integer.MAX_VALUE),
                        questsConfig.getString(filterPath + ".weapon", "ANY"),
                        questsConfig.getStringList(filterPath + ".spawn-reasons"),
                        questsConfig.getBoolean(filterPath + ".require-critical-hit", false),
                        questsConfig.getStringList(filterPath + ".regions"),
                        questsConfig.getBoolean(filterPath + ".require-own-island", false),
                        questsConfig.getBoolean(filterPath + ".require-own-plot", false),
                        questsConfig.getString(filterPath + ".location", ""),
                        questsConfig.getDouble(filterPath + ".radius", 3.0D)
                );
                QuestDefinition definition = new QuestDefinition(
                        id, cycle, difficulty, type, target, required,
                        material(questsConfig.getString(path + ".material"), Material.BOOK),
                        requireTitle(path), List.copyOf(questsConfig.getStringList(path + ".lore")),
                        List.copyOf(questsConfig.getStringList(path + ".requirements")),
                        List.copyOf(questsConfig.getStringList(path + ".rewards")), filters,
                        questsConfig.getBoolean(path + ".rerollable", true),
                        questsConfig.getString(path + ".permission", ""),
                        Math.max(0L, questsConfig.getLong(path + ".cooldown-seconds", 0L)),
                        Math.max(0, questsConfig.getInt(path + ".season-xp", defaultSeasonXp(difficulty))),
                        questsConfig.getString(path + ".completion.sound", ""),
                        questsConfig.getString(path + ".completion.particle", "")
                );
                quests.put(id, definition);
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            } catch (RuntimeException exception) {
                validationErrors.add(new ValidationIssue("invalid-quest", Map.of("%quest_id%", rawId)));
                plugin.getLogger().warning("Invalid quest '" + rawId + "': " + exception.getMessage());
            }
        }
    }

<<<<<<< HEAD
    private int defaultSeasonXp(Difficulty difficulty) {
        return switch (difficulty) { case EASY -> 10; case MEDIUM -> 25; case HARD -> 50; };
    }

    private String requireTitle(String path) {
        String title = questsConfig.getString(path + ".title", "");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is missing or blank");
        return title;
    }

    private void migrateLegacyQuestText() {
        ConfigurationSection root = questsConfig.getConfigurationSection("quests");
        if (root == null) return;
        YamlConfiguration active = YamlConfiguration.loadConfiguration(PluginPaths.resolveMessageFile(plugin, languageFile()));
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(PluginPaths.messageFile(plugin, "messages.yml"));
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        boolean changed = false;
        for (String id : root.getKeys(false)) {
            String path = "quests." + id;
            if (!questsConfig.contains(path + ".title")) {
                String title = questsConfig.getString(path + ".name");
<<<<<<< HEAD
                String key = questsConfig.getString(path + ".name-key");
                if ((title == null || title.isBlank()) && key != null) title = active.getString(key, defaults.getString(key, "&f" + id));
                questsConfig.set(path + ".title", title == null || title.isBlank() ? "&f" + id : title);
                changed = true;
            }
            if (!questsConfig.contains(path + ".lore")) {
                String key = questsConfig.getString(path + ".lore-key");
                List<String> lore = key == null ? List.of() : active.getStringList(key);
                if (lore.isEmpty() && key != null) lore = defaults.getStringList(key);
                questsConfig.set(path + ".lore", lore);
                changed = true;
            }
            if (!questsConfig.contains(path + ".cycle")) {
                questsConfig.set(path + ".cycle", "daily");
                changed = true;
            }
        }
        if (changed) saveQuests();
    }

    private void migrateLegacyRewardProfiles() {
        ConfigurationSection root = questsConfig.getConfigurationSection("quests");
        if (root == null) return;
        boolean changed = false;
        for (String id : root.getKeys(false)) {
            String path = "quests." + id;
            if (questsConfig.contains(path + ".rewards")) continue;
            String profile = questsConfig.getString(path + ".reward-profile");
            if (profile == null || profile.isBlank()) continue;
            List<String> commands = config.getStringList("legacy-reward-profiles." + profile + ".commands");
            if (!commands.isEmpty()) {
                questsConfig.set(path + ".rewards", commands);
                changed = true;
            }
        }
        if (changed) saveQuests();
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    public synchronized CreateResult createQuest(String idInput, Difficulty difficulty, ObjectiveType type, String target,
                                                  int required, Material icon) {
<<<<<<< HEAD
        return createQuest(idInput, "daily", difficulty, type, target, required, icon);
    }

    public synchronized CreateResult createQuest(String idInput, String cycle, Difficulty difficulty, ObjectiveType type,
                                                  String target, int required, Material icon) {
        String id = idInput == null ? "" : idInput.toLowerCase(Locale.ROOT);
        String normalizedCycle = CycleSettings.normalize(cycle);
        if (!SAFE_ID.matcher(id).matches()) return CreateResult.INVALID_ID;
        if (questsConfig.contains("quests." + id)) return CreateResult.EXISTS;
        if (!cycles.containsKey(normalizedCycle)) return CreateResult.INVALID_CYCLE;
        String normalizedTarget = type.normalizeTarget(target);
        if (!type.validTarget(normalizedTarget) || required <= 0) return CreateResult.INVALID_TARGET;
        String base = "quests." + id;
        questsConfig.set(base + ".cycle", normalizedCycle);
        questsConfig.set(base + ".difficulty", difficulty.key());
        questsConfig.set(base + ".type", type.name());
        questsConfig.set(base + ".target", normalizedTarget);
        questsConfig.set(base + ".required", required);
        questsConfig.set(base + ".material", (icon == null ? Material.BOOK : icon).name());
        questsConfig.set(base + ".title", "&#7DE2FF" + id);
        questsConfig.set(base + ".lore", List.of("&7Configure this quest with &f/quest editor " + id));
        questsConfig.set(base + ".requirements", List.of());
        questsConfig.set(base + ".rewards", List.of());
        questsConfig.set(base + ".rerollable", true);
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        saveQuests();
        loadQuests();
        return CreateResult.CREATED;
    }

<<<<<<< HEAD
    public synchronized boolean cloneQuest(String sourceId, String newIdInput) {
        String source = sourceId == null ? "" : sourceId.toLowerCase(Locale.ROOT);
        String newId = newIdInput == null ? "" : newIdInput.toLowerCase(Locale.ROOT);
        if (!SAFE_ID.matcher(newId).matches() || questsConfig.contains("quests." + newId)) return false;
        ConfigurationSection sourceSection = questsConfig.getConfigurationSection("quests." + source);
        if (sourceSection == null) return false;
        questsConfig.set("quests." + newId, deepMap(sourceSection));
        questsConfig.set("quests." + newId + ".title", "&#7DE2FF" + newId);
        saveQuests();
        loadQuests();
        return true;
    }

    private Map<String, Object> deepMap(ConfigurationSection section) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nested) copy.put(key, deepMap(nested));
            else copy.put(key, value);
        }
        return copy;
    }

    public synchronized boolean updateQuestField(String questIdInput, String field, Object value) {
        String id = questIdInput == null ? "" : questIdInput.toLowerCase(Locale.ROOT);
        if (!questsConfig.contains("quests." + id) || field == null || field.isBlank()) return false;
        Set<String> allowed = Set.of("cycle", "difficulty", "type", "target", "required", "material", "title", "lore",
                "requirements", "rewards", "rerollable", "permission", "cooldown-seconds", "season-xp",
                "completion.sound", "completion.particle", "filters.worlds", "filters.min-y", "filters.max-y",
                "filters.weapon", "filters.spawn-reasons", "filters.require-critical-hit", "filters.regions",
                "filters.require-own-island", "filters.require-own-plot", "filters.location", "filters.radius");
        if (!allowed.contains(field)) return false;
        questsConfig.set("quests." + id + "." + field, value);
        saveQuests();
        loadQuests();
        return quests.containsKey(id);
    }

    public synchronized boolean deleteQuest(String idInput) {
        String id = idInput == null ? "" : idInput.toLowerCase(Locale.ROOT);
        if (!questsConfig.contains("quests." + id)) return false;
=======
    public synchronized boolean deleteQuest(String idInput) {
        String id = idInput == null ? "" : idInput.toLowerCase(Locale.ROOT);
        if (!questsConfig.contains("quests." + id)) {
            return false;
        }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
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
<<<<<<< HEAD
        Material parsed = input == null ? null : Material.matchMaterial(input);
        return parsed == null || parsed.isAir() ? fallback : parsed;
    }

    public synchronized YamlConfiguration raw() { return config; }
    public synchronized Map<String, QuestDefinition> quests() { return Map.copyOf(quests); }
    public synchronized QuestDefinition quest(String id) { return id == null ? null : quests.get(id.toLowerCase(Locale.ROOT)); }
    public synchronized Map<String, CycleSettings> cycles() { return Map.copyOf(cycles); }
    public synchronized CycleSettings cycle(String name) { return cycles.get(CycleSettings.normalize(name)); }
    public synchronized List<ValidationIssue> validationErrors() { return List.copyOf(validationErrors); }
    public synchronized String languageFile() { return config == null ? "messages.yml" : config.getString("language-file", "messages.yml"); }
    public synchronized boolean metricsEnabled() { return config.getBoolean("metric", true); }
    public synchronized int maxActiveQuests() { return Math.max(1, config.getInt("settings.max-active-quests-per-player", 6)); }
    public synchronized boolean broadcastReset() { return config.getBoolean("settings.broadcast-cycle-reset", config.getBoolean("settings.broadcast-daily-reset", true)); }
    public synchronized boolean sendProgressMessages() { return config.getBoolean("settings.send-progress-messages", true); }
    public synchronized int progressPercentStep() { return Math.max(1, Math.min(100, config.getInt("settings.progress-message-percent-step", 10))); }
    public synchronized long databaseFlushTicks() { return Math.max(20L, config.getLong("settings.database-flush-ticks", 100L)); }
    public synchronized long cacheUnloadMillis() { return Math.max(1L, config.getLong("settings.player-cache-unload-minutes", 10L)) * 60_000L; }

    public synchronized long nextReset(CycleSettings settings, long referenceMillis) {
        if (settings == null) return Long.MAX_VALUE;
        ZonedDateTime reference = Instant.ofEpochMilli(Math.max(referenceMillis, System.currentTimeMillis()))
                .atZone(settings.timezone()).withSecond(0).withNano(0);
        return switch (settings.resetType()) {
            case MANUAL -> Long.MAX_VALUE;
            case INTERVAL -> referenceMillis + settings.intervalMillis();
            case DAILY -> nextDaily(reference, settings.time()).toInstant().toEpochMilli();
            case WEEKLY -> {
                ZonedDateTime candidate = reference.with(settings.weekDay()).with(settings.time());
                if (!candidate.isAfter(reference)) candidate = candidate.plusWeeks(1);
                yield candidate.toInstant().toEpochMilli();
            }
            case MONTHLY -> nextMonthly(reference, settings.monthDay(), settings.time()).toInstant().toEpochMilli();
            case CRON -> nextCron(reference, settings.cron()).toInstant().toEpochMilli();
        };
    }

    private ZonedDateTime nextDaily(ZonedDateTime reference, LocalTime time) {
        ZonedDateTime candidate = reference.with(time);
        return candidate.isAfter(reference) ? candidate : candidate.plusDays(1);
    }

    private ZonedDateTime nextMonthly(ZonedDateTime reference, int requestedDay, LocalTime time) {
        ZonedDateTime cursor = reference;
        for (int i = 0; i < 24; i++) {
            int day = Math.min(requestedDay, cursor.toLocalDate().lengthOfMonth());
            ZonedDateTime candidate = cursor.withDayOfMonth(day).with(time);
            if (candidate.isAfter(reference)) return candidate;
            cursor = cursor.plusMonths(1).withDayOfMonth(1);
        }
        return reference.plusMonths(1);
    }

    private ZonedDateTime nextCron(ZonedDateTime reference, String expression) {
        Cron cron = Cron.parse(expression);
        ZonedDateTime candidate = reference.plusMinutes(1).withSecond(0).withNano(0);
        for (int i = 0; i < 2_700_000; i++) {
            if (cron.matches(candidate)) return candidate;
            candidate = candidate.plusMinutes(1);
        }
        throw new IllegalArgumentException("cron expression has no match in five years: " + expression);
    }

    private ZoneId safeZone(String input) {
        try { return ZoneId.of(input == null ? "Asia/Ho_Chi_Minh" : input); }
        catch (RuntimeException ignored) { return ZoneId.of("Asia/Ho_Chi_Minh"); }
    }

    private DayOfWeek safeWeekDay(String input) {
        try { return DayOfWeek.valueOf(input == null ? "MONDAY" : input.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return DayOfWeek.MONDAY; }
    }

    private LocalTime safeTime(String input) {
        try { return LocalTime.parse(input == null ? "00:00" : input); }
        catch (DateTimeParseException ignored) { return LocalTime.MIDNIGHT; }
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    public synchronized int guiSize() {
        int size = Math.max(9, Math.min(54, config.getInt("gui.size", 54)));
        return ((size + 8) / 9) * 9;
    }
<<<<<<< HEAD
    public synchronized List<Integer> questSlots() {
        List<Integer> configured = config.getIntegerList("gui.quest-slots");
        boolean lockRows = config.getBoolean("gui.lock-quest-rows", true);
        int size = guiSize();
        List<Integer> result = new ArrayList<>();
        for (Integer slot : configured) {
            if (slot == null || slot < 0 || slot >= size || result.contains(slot)) continue;
            // In a six-row menu, Minecraft row 3 and row 4 are slots 18..35.
            if (lockRows && (slot < 18 || slot > 35)) continue;
            result.add(slot);
        }
        if (result.isEmpty()) {
            // Seven centered quest icons on each row leave clean borders at both sides.
            result.addAll(List.of(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34));
        }
        return List.copyOf(result);
    }
    public synchronized int guiSlot(String path, int fallback) { return config.getInt(path + ".slot", fallback); }
    public synchronized Material guiMaterial(String path, Material fallback) { return material(config.getString(path + ".material"), fallback); }
    public synchronized Material fillerMaterial() { return material(config.getString("gui.filler-material"), Material.BLACK_STAINED_GLASS_PANE); }
    public synchronized boolean guiCompletionAnimation() { return config.getBoolean("gui.completion-animation", true); }
    public synchronized boolean actionBarTracker() { return config.getBoolean("gui.action-bar-tracker", true); }
    public synchronized boolean bedrockFormsEnabled() { return config.getBoolean("bedrock.enabled", true); }

    public synchronized boolean rerollEnabled() { return config.getBoolean("reroll.enabled", true); }
    public synchronized int freeRerolls() { return Math.max(0, config.getInt("reroll.free-per-cycle", 1)); }
    public synchronized String rerollCostType() { return config.getString("reroll.cost.type", "COMMAND").toUpperCase(Locale.ROOT); }
    public synchronized double rerollCostAmount() { return Math.max(0D, config.getDouble("reroll.cost.amount", 0D)); }
    public synchronized String rerollCheckCommand() { return config.getString("reroll.cost.check-command", ""); }
    public synchronized String rerollTakeCommand() { return config.getString("reroll.cost.take-command", ""); }
    public synchronized Set<String> rerollBlockedQuests() { return normalizedLowerSet(config.getStringList("reroll.blocked-quests")); }

    public synchronized boolean streakEnabled() { return config.getBoolean("streak.enabled", true); }
    public synchronized boolean catchupEnabled() { return config.getBoolean("streak.catch-up.enabled", true); }
    public synchronized int seasonXpPerLevel() { return Math.max(1, config.getInt("season.xp-per-level", 100)); }
    public synchronized List<String> streakRewardCommands(int streak) { return config.getStringList("streak.milestones." + streak + ".commands"); }
    public synchronized int streakTokenReward(int streak) { return Math.max(0, config.getInt("streak.milestones." + streak + ".catchup-tokens", 0)); }
    public synchronized List<String> perfectWeekCommands() { return config.getStringList("streak.perfect-week.commands"); }
    public synchronized List<String> seasonLevelCommands(int level) { return config.getStringList("season.level-rewards." + level + ".commands"); }

    public synchronized String storageType() { return config.getString("storage.type", "SQLITE").toUpperCase(Locale.ROOT); }
    public synchronized String storageHost() { return config.getString("storage.host", "127.0.0.1"); }
    public synchronized int storagePort() { return config.getInt("storage.port", storageType().equals("MARIADB") ? 3306 : 3306); }
    public synchronized String storageDatabase() { return config.getString("storage.database", "koraquest"); }
    public synchronized String storageUsername() { return config.getString("storage.username", "root"); }
    public synchronized String storagePassword() { return config.getString("storage.password", ""); }
    public synchronized int storagePoolSize() { return Math.max(1, config.getInt("storage.pool-size", storageType().equals("SQLITE") ? 1 : 10)); }
    public synchronized String storageTablePrefix() {
        String value = config.getString("storage.table-prefix", "kq_").replaceAll("[^A-Za-z0-9_]", "");
        return value.isBlank() ? "kq_" : value;
    }
    public synchronized String serverId() { return config.getString("network.server-id", "server-1"); }
    public synchronized boolean networkSyncEnabled() { return config.getBoolean("network.enabled", !storageType().equals("SQLITE")); }
    public synchronized long networkPollTicks() { return Math.max(100L, config.getLong("network.poll-ticks", 200L)); }
    public synchronized long distributedLockMillis() { return Math.max(10_000L, config.getLong("network.lock-timeout-seconds", 30L) * 1000L); }
    public synchronized boolean redisEnabled() { return config.getBoolean("network.redis.enabled", false); }
    public synchronized String redisUri() { return config.getString("network.redis.uri", "redis://127.0.0.1:6379"); }
    public synchronized String redisChannel() { return config.getString("network.redis.channel", "koraquest:sync"); }

    public synchronized boolean updateCheckerEnabled() { return config.getBoolean("update-checker.enabled", true); }
    public synchronized int spigotResourceId() { return Math.max(0, config.getInt("update-checker.resource-id", 137091)); }
    public synchronized boolean notifyConsoleUpdate() { return config.getBoolean("update-checker.notify-console", true); }
    public synchronized String updateNotifyPermission() { return config.getString("update-checker.notify-permission", "koraquest.update-notify"); }
    public synchronized String updateDownloadUrl() {
        String configured = config.getString("update-checker.download-url", "");
        if (configured != null && !configured.isBlank()) return configured;
        return spigotResourceId() > 0 ? "https://www.spigotmc.org/resources/" + spigotResourceId() + "/" : "";
    }
    public synchronized long updateCheckIntervalTicks() { return Math.max(72_000L, config.getLong("update-checker.interval-hours", 6L) * 72_000L); }

    public synchronized boolean placedBlockTracking() { return config.getBoolean("anti-exploit.placed-block-tracking", true); }
    public synchronized long placedBlockExpireMillis() { return Math.max(1L, config.getLong("anti-exploit.placed-block-expire-days", 7L)) * 86_400_000L; }
    public synchronized boolean countPlayerPlacedBlocks() { return config.getBoolean("anti-exploit.break.count-player-placed-blocks", false); }
    public synchronized boolean countCreativeBreak() { return config.getBoolean("anti-exploit.break.count-creative-mode", false); }
    public synchronized boolean countSilkTouchBreak() { return config.getBoolean("anti-exploit.break.count-silk-touch", true); }
    public synchronized boolean countCustomNamedMobs() { return config.getBoolean("anti-exploit.kill.count-custom-named-mobs", false); }
    public synchronized boolean countMythicMobs() { return config.getBoolean("anti-exploit.kill.count-mythic-mobs", true); }
    public synchronized boolean countHookedEntities() { return config.getBoolean("anti-exploit.fish.count-hooked-entities", false); }
    public synchronized Set<String> allowedSpawnReasons() { return normalizedSet(config.getStringList("anti-exploit.kill.allowed-spawn-reasons")); }
    public synchronized Set<String> worldBlacklist() { return normalizedLowerSet(config.getStringList("anti-exploit.worlds.blacklist")); }
    public synchronized Set<String> worldWhitelist() { return normalizedLowerSet(config.getStringList("anti-exploit.worlds.whitelist")); }
    public synchronized Set<String> allowedGameModes() {
        List<String> values = config.getStringList("anti-exploit.gamemode-whitelist");
        return values.isEmpty() ? Set.of("SURVIVAL", "ADVENTURE") : normalizedSet(values);
    }
    public synchronized String antiExploitBypassPermission() { return config.getString("anti-exploit.bypass-permission", "koraquest.bypass.antiexploit"); }
    public synchronized Set<String> blockedRegions() { return normalizedLowerSet(config.getStringList("anti-exploit.integrations.worldguard.blacklist")); }
    public synchronized Set<String> allowedRegions() { return normalizedLowerSet(config.getStringList("anti-exploit.integrations.worldguard.whitelist")); }
    public synchronized boolean requireOwnIslandGlobally() { return config.getBoolean("anti-exploit.integrations.islands.require-member", false); }
    public synchronized boolean requireOwnPlotGlobally() { return config.getBoolean("anti-exploit.integrations.plots.require-member", false); }
    public synchronized boolean countAutomation() { return config.getBoolean("anti-exploit.count-automation", false); }

    public synchronized int rewardRetryBatchSize() { return Math.max(1, config.getInt("rewards.retry-batch-size", 50)); }
    public synchronized long rewardRetryTicks() { return Math.max(1200L, config.getLong("rewards.auto-retry-ticks", 6000L)); }
    public synchronized long deliveredRewardRetentionMillis() { return Math.max(1L, config.getLong("rewards.delivered-retention-days", 30L)) * 86_400_000L; }

    private Set<String> normalizedSet(List<String> input) {
        return input == null ? Set.of() : input.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> normalizedLowerSet(List<String> input) {
        return input == null ? Set.of() : input.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    public record ValidationIssue(String key, Map<String, String> placeholders) {
        public ValidationIssue { placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders); }
    }

    public enum CreateResult { CREATED, EXISTS, INVALID_ID, INVALID_TARGET, INVALID_CYCLE }

    private record Cron(Set<Integer> minutes, Set<Integer> hours, Set<Integer> days, Set<Integer> months, Set<Integer> weekDays) {
        static Cron parse(String expression) {
            String[] fields = expression == null ? new String[0] : expression.trim().split("\\s+");
            if (fields.length != 5) throw new IllegalArgumentException("cron must contain five fields");
            return new Cron(parseField(fields[0], 0, 59), parseField(fields[1], 0, 23),
                    parseField(fields[2], 1, 31), parseField(fields[3], 1, 12), parseField(fields[4], 0, 7));
        }

        boolean matches(ZonedDateTime value) {
            int weekDay = value.getDayOfWeek().getValue() % 7;
            return minutes.contains(value.getMinute()) && hours.contains(value.getHour())
                    && days.contains(value.getDayOfMonth()) && months.contains(value.getMonthValue())
                    && (weekDays.contains(weekDay) || (weekDay == 0 && weekDays.contains(7)));
        }

        private static Set<Integer> parseField(String raw, int min, int max) {
            Set<Integer> result = new LinkedHashSet<>();
            for (String token : raw.split(",")) {
                String value = token.trim();
                int step = 1;
                if (value.contains("/")) {
                    String[] stepParts = value.split("/", 2);
                    value = stepParts[0];
                    step = Math.max(1, Integer.parseInt(stepParts[1]));
                }
                int start = min;
                int end = max;
                if (!value.equals("*")) {
                    if (value.contains("-")) {
                        String[] range = value.split("-", 2);
                        start = Integer.parseInt(range[0]);
                        end = Integer.parseInt(range[1]);
                    } else {
                        start = end = Integer.parseInt(value);
                    }
                }
                if (start < min || end > max || start > end) throw new IllegalArgumentException("invalid cron field: " + raw);
                for (int current = start; current <= end; current += step) result.add(current);
            }
            return Set.copyOf(result);
        }
=======

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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }
}
