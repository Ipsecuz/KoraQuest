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
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("message/messages.yml"), StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
            messages.setDefaults(defaults);
        } catch (Exception exception) {
            defaults = new YamlConfiguration();
            plugin.getLogger().warning("Could not load bundled message defaults: " + exception.getMessage());
        }
        prefix = messages.getString("prefix", "&#FFB84D&lKoraQuest &8» &f");
    }

    public synchronized String raw(String path) {
        String value = messages.getString(path);
        if (value != null) {
            return value;
        }
        String fallback = defaults.getString(path);
        return fallback == null ? path : fallback;
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
        return raw("gui.difficulty." + difficulty.key() + ".name");
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
