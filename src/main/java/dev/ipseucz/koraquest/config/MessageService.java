package dev.ipseucz.koraquest.config;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.QuestDefinition;
import dev.ipseucz.koraquest.util.PluginPaths;
import dev.ipseucz.koraquest.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MessageService {
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private YamlConfiguration messages;
    private YamlConfiguration defaults;
    private File activeFile;
    private String prefix;

    public MessageService(KoraQuestPlugin plugin, QuestConfig config) {
        this.plugin = plugin;
        this.config = config;
        reload();
    }

    public synchronized void reload() {
        String name = config.languageFile();
        activeFile = PluginPaths.resolveMessageFile(plugin, name);
        if (!activeFile.exists()) {
            File source = PluginPaths.messageFile(plugin, "messages.yml");
            try {
                Files.copy(source.toPath(), activeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not create language file " + name, exception);
            }
        }
        messages = YamlConfiguration.loadConfiguration(activeFile);
        InputStream bundledStream = plugin.getResource("message/" + name);
        if (bundledStream == null) bundledStream = plugin.getResource("message/messages.yml");
        try (InputStream stream = bundledStream;
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
            messages.setDefaults(defaults);
            if (migrateLegacyCommandHelp()) messages.save(activeFile);
        } catch (Exception exception) {
            defaults = new YamlConfiguration();
            plugin.getLogger().warning("Could not load bundled message defaults: " + exception.getMessage());
        }
        prefix = messages.getString("prefix", "&#FFB84D&lKoraQuest &8» &f");
    }


    /**
     * Upgrades only the command-help lists shipped by older KoraQuest builds.
     * Custom language files are left untouched unless they still contain the
     * exact compact legacy layout that exposed admin commands to players.
     */
    private boolean migrateLegacyCommandHelp() {
        boolean changed = false;
        List<String> help = messages.getStringList("messages.help");
        boolean legacyPlayerHelp = help.stream().anyMatch(line -> line.contains("accept|cancel|claim|reroll"))
                || help.stream().anyMatch(line -> line.contains("/quest editor [id]"));
        if (help.isEmpty() || legacyPlayerHelp) {
            messages.set("messages.help", defaults.getStringList("messages.help"));
            changed = true;
        }

        List<String> adminHelp = messages.getStringList("messages.admin-help");
        boolean legacyAdminHelp = adminHelp.stream()
                .filter(line -> line.contains("/quest admin "))
                .anyMatch(line -> !line.contains("—") && !line.contains(" - "));
        if (adminHelp.isEmpty() || legacyAdminHelp) {
            messages.set("messages.admin-help", defaults.getStringList("messages.admin-help"));
            changed = true;
        }

        if (!messages.isList("messages.help-admin")) {
            messages.set("messages.help-admin", defaults.getStringList("messages.help-admin"));
            changed = true;
        }
        return changed;
    }

    public synchronized String raw(String path) {
        return rawOr(path, path);
    }

    /**
     * Reads a translated value without ever leaking a configuration key into the GUI.
     * This is intentionally used by dynamic enum paths such as difficulty/status/filter.
     */
    public synchronized String rawOr(String path, String fallbackValue) {
        String value = messages.getString(path);
        if (value != null && !value.isBlank()) {
            return value;
        }
        String fallback = defaults.getString(path);
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return fallbackValue == null ? "" : fallbackValue;
    }

    public synchronized List<String> rawList(String path) {
        List<String> values = messages.getStringList(path);
        if (!values.isEmpty() || messages.isList(path)) {
            return values;
        }
        return defaults.getStringList(path);
    }

    public synchronized String format(String path, Map<String, String> placeholders) {
        return Text.color(Text.placeholders(raw(path), merge(placeholders)));
    }

    public synchronized List<String> formatList(String path, Map<String, String> placeholders) {
        Map<String, String> merged = merge(placeholders);
        List<String> result = new ArrayList<>();
        for (String line : rawList(path)) {
            result.add(Text.color(Text.placeholders(line, merged)));
        }
        return result;
    }

    public synchronized List<String> parseRawList(String path, Map<String, String> placeholders) {
        Map<String, String> merged = merge(placeholders);
        List<String> result = new ArrayList<>();
        for (String line : rawList(path)) {
            result.add(Text.placeholders(line, merged));
        }
        return result;
    }

    public synchronized void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public synchronized void send(CommandSender sender, String path, Map<String, String> placeholders) {
        if (sender != null) {
            sender.sendMessage(format("messages." + path, placeholders));
        }
    }

    public synchronized void sendList(CommandSender sender, String path, Map<String, String> placeholders) {
        if (sender == null) {
            return;
        }
        for (String line : formatList("messages." + path, placeholders)) {
            sender.sendMessage(line);
        }
    }

    public synchronized String questName(QuestDefinition quest) {
        return quest.title() == null || quest.title().isBlank() ? quest.id() : quest.title();
    }

    public synchronized List<String> questLore(QuestDefinition quest) {
        return quest.lore();
    }

    public synchronized String difficultyName(Difficulty difficulty) {
        if (difficulty == null) {
            return rawOr("gui.difficulty.unknown.name", "&7Không xác định");
        }
        String fallback = switch (difficulty) {
            case EASY -> "&#75FF75Dễ";
            case MEDIUM -> "&#FFD36ATrung bình";
            case HARD -> "&#FF6B6BKhó";
        };
        return rawOr("gui.difficulty." + difficulty.key() + ".name", fallback);
    }

    public synchronized String guiValue(String path, String fallbackValue) {
        return rawOr(path, fallbackValue);
    }

    private Map<String, String> merge(Map<String, String> placeholders) {
        Map<String, String> merged = new HashMap<>();
        merged.put("%prefix%", prefix);
        if (placeholders != null) {
            merged.putAll(placeholders);
        }
        return merged;
    }

}
