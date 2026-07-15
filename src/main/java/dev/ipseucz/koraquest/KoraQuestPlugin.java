package dev.ipseucz.koraquest;

import dev.ipseucz.koraquest.api.KoraQuestAPI;
import dev.ipseucz.koraquest.command.QuestCommand;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.data.PlayerDataService;
import dev.ipseucz.koraquest.gui.QuestGui;
import dev.ipseucz.koraquest.listener.QuestListener;
import dev.ipseucz.koraquest.update.UpdateChecker;
import dev.ipseucz.koraquest.util.PlatformDetector;
import dev.ipseucz.koraquest.util.PluginPaths;
import dev.ipseucz.koraquest.util.SafeScheduler;
import dev.ipseucz.koraquest.util.Text;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public final class KoraQuestPlugin extends JavaPlugin {
    private SafeScheduler scheduler;
    private QuestConfig questConfig;
    private MessageService messages;
    private PlayerDataService playerData;
    private QuestManager questManager;
    private QuestGui questGui;
    private UpdateChecker updateChecker;
    private Metrics metrics;

    @Override
    public void onEnable() {
        try {
            PluginPaths.prepareLayout(this);
            scheduler = new SafeScheduler(this);
            questConfig = new QuestConfig(this);
            messages = new MessageService(this, questConfig);
            playerData = new PlayerDataService(this);
            questManager = new QuestManager(this, questConfig, messages, playerData);
            questGui = new QuestGui(this, questManager, questConfig, messages);
            updateChecker = new UpdateChecker(this, questConfig, messages);

            QuestCommand commandHandler = new QuestCommand(this, questManager, questConfig, messages, questGui, updateChecker);
            PluginCommand command = Objects.requireNonNull(getCommand("quest"), "quest command is missing from plugin.yml");
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);

            Bukkit.getPluginManager().registerEvents(questGui, this);
            Bukkit.getPluginManager().registerEvents(new QuestListener(questManager), this);
            Bukkit.getPluginManager().registerEvents(updateChecker, this);

            KoraQuestAPI.bootstrap(questManager);
            questManager.ensureCycle(false);
            scheduler.runGlobalTimer(() -> questManager.ensureCycle(true), 1200L, 1200L);
            scheduler.runGlobalTimer(playerData::flushDirtyAsync, questConfig.databaseFlushTicks(), questConfig.databaseFlushTicks());

            setupMetrics();
            printBanner();
            updateChecker.start();
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "KoraQuest could not be enabled safely. The plugin will be disabled.", throwable);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        KoraQuestAPI.shutdown();
        // Stop producers before flushing/closing SQLite. This prevents a repeating Folia task
        // from enqueueing another database write while shutdown is in progress.
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (metrics != null) {
            try {
                metrics.shutdown();
            } catch (Throwable throwable) {
                getLogger().log(Level.WARNING, "Could not stop bStats cleanly", throwable);
            }
        }
        if (playerData != null) {
            try {
                playerData.shutdown();
            } catch (Throwable throwable) {
                getLogger().log(Level.WARNING, "Could not finish the SQLite shutdown cleanly", throwable);
            }
        }
    }

    public void reloadPlugin(CommandSender sender) {
        try {
            questConfig.reload();
            messages.reload();
            questManager.ensureCycle(false);
            sendSafe(sender, "reloaded", Map.of());
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "Could not reload KoraQuest", throwable);
            sendSafe(sender, "reload-failed", Map.of());
        }
    }

    public void sendSafe(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null || messages == null) {
            return;
        }
        if (sender instanceof Player player) {
            scheduler.runEntity(player, () -> messages.send(player, key, placeholders));
        } else {
            scheduler.runGlobal(() -> messages.send(sender, key, placeholders));
        }
    }

    private void setupMetrics() {
        if (!questConfig.metricsEnabled()) {
            return;
        }
        try {
            metrics = new Metrics(this, 32623);
            metrics.addCustomChart(new SimplePie("platform", PlatformDetector::name));
            metrics.addCustomChart(new SingleLineChart("loaded_quests", () -> questConfig.quests().size()));
            metrics.addCustomChart(new SimplePie("storage", () -> "SQLite"));
        } catch (Throwable throwable) {
            getLogger().log(Level.WARNING, "Could not start bStats metrics", throwable);
        }
    }

    private void printBanner() {
        String[] lines = {
                "                               ",
                "  ▄▄▄▄   ▄▄▄                   ",
                " █▀ ██  ██                     ",
                "    ██ ██          ▄           ",
                "    █████    ▄███▄ ████▄▄▀▀█▄  ",
                "    ██ ██▄   ██ ██ ██   ▄█▀██  ",
                "  ▀██▀  ▀██▄▄▀███▀▄█▀  ▄▀█▄██  ",
                "                               ",
                "   ▄▄▄▄                        ",
                " ▄█▀▀███▄▄                  █▄ ",
                " ██    ██                  ▄██▄",
                " ██    ██ ██ ██ ▄█▀█▄ ▄██▀█ ██ ",
                " ██  ▄ ██ ██ ██ ██▄█▀ ▀███▄ ██ ",
                "  ▀█████▄▄▀██▀█▄▀█▄▄▄█▄▄██▀▄██ ",
                "       ▀█                      ",
                "                               "
        };
        for (String line : lines) {
            Bukkit.getConsoleSender().sendMessage(Text.color("&e" + line));
        }
        Bukkit.getConsoleSender().sendMessage(Text.color("&ePlatform: Paper/Folia (detected: "
                + PlatformDetector.name() + ") | Version: " + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(Text.color("&eLoaded quests: " + questConfig.quests().size()));
        if (!questConfig.validationErrors().isEmpty()) {
            getLogger().warning("Quest validation issues: " + questConfig.validationErrors().size() + " (use /quest admin validate)");
        }
    }

    public SafeScheduler scheduler() {
        return scheduler;
    }

    public QuestConfig questConfig() {
        return questConfig;
    }

    public MessageService messages() {
        return messages;
    }

    public QuestManager questManager() {
        return questManager;
    }
}
