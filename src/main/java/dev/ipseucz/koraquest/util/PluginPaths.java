package dev.ipseucz.koraquest.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Centralizes KoraQuest's on-disk layout so configuration, language and storage
 * services cannot accidentally write duplicate files to different locations.
 */
public final class PluginPaths {
    private static final List<String> MESSAGE_FILES = List.of("messages.yml", "messages_en.yml");
    private static final List<String> DATA_FILES = List.of("data.db", "data.db-wal", "data.db-shm", "data.yml", "data.yml.migrated");

    private PluginPaths() {
    }

    public static void prepareLayout(JavaPlugin plugin) {
        File root = plugin.getDataFolder();
        createDirectory(root, "plugin data");
        createDirectory(dataDirectory(plugin), "data");
        createDirectory(messageDirectory(plugin), "message");

        for (String name : MESSAGE_FILES) {
            migrateFile(plugin, new File(root, name), new File(messageDirectory(plugin), name));
        }
        for (String name : DATA_FILES) {
            migrateFile(plugin, new File(root, name), new File(dataDirectory(plugin), name));
        }
    }

    public static File configFile(JavaPlugin plugin) {
        return new File(plugin.getDataFolder(), "config.yml");
    }

    public static File questsFile(JavaPlugin plugin) {
        return new File(plugin.getDataFolder(), "quests.yml");
    }

    public static File dataDirectory(JavaPlugin plugin) {
        return new File(plugin.getDataFolder(), "data");
    }

    public static File messageDirectory(JavaPlugin plugin) {
        return new File(plugin.getDataFolder(), "message");
    }

    public static File databaseFile(JavaPlugin plugin) {
        return new File(dataDirectory(plugin), "data.db");
    }

    public static File legacyDataFile(JavaPlugin plugin) {
        return new File(dataDirectory(plugin), "data.yml");
    }

    public static File migratedLegacyDataFile(JavaPlugin plugin) {
        return new File(dataDirectory(plugin), "data.yml.migrated");
    }

    public static File messageFile(JavaPlugin plugin, String fileName) {
        return new File(messageDirectory(plugin), fileName);
    }

    public static File resolveMessageFile(JavaPlugin plugin, String fileName) {
        File target = messageFile(plugin, fileName);
        migrateFile(plugin, new File(plugin.getDataFolder(), fileName), target);
        return target;
    }

    private static void migrateFile(JavaPlugin plugin, File source, File target) {
        if (!source.isFile() || target.exists()) {
            return;
        }
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Moved legacy file " + source.getName() + " to "
                    + plugin.getDataFolder().toPath().relativize(target.toPath()));
        } catch (IOException moveFailure) {
            try {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().warning("Copied legacy file " + source.getName()
                        + " to the new folder, but could not remove the old file: " + moveFailure.getMessage());
            } catch (IOException copyFailure) {
                throw new IllegalStateException("Could not migrate " + source.getName() + " to " + target, copyFailure);
            }
        }
    }

    private static void createDirectory(File directory, String label) {
        if (directory.isDirectory()) {
            return;
        }
        if (directory.exists() || !directory.mkdirs()) {
            throw new IllegalStateException("Could not create KoraQuest " + label + " directory: " + directory);
        }
    }
}
