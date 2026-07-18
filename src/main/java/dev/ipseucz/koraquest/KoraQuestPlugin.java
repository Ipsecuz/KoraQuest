package dev.ipseucz.koraquest;

import dev.ipseucz.koraquest.api.KoraQuestAPI;
<<<<<<< HEAD
import dev.ipseucz.koraquest.bedrock.BedrockFormService;
=======
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
import dev.ipseucz.koraquest.command.QuestCommand;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.data.PlayerDataService;
<<<<<<< HEAD
import dev.ipseucz.koraquest.editor.QuestEditor;
import dev.ipseucz.koraquest.gui.QuestGui;
import dev.ipseucz.koraquest.integration.IntegrationManager;
import dev.ipseucz.koraquest.listener.QuestListener;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.network.NetworkSyncService;
import dev.ipseucz.koraquest.security.AntiExploitService;
=======
import dev.ipseucz.koraquest.gui.QuestGui;
import dev.ipseucz.koraquest.listener.QuestListener;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
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

<<<<<<< HEAD
import java.lang.reflect.Method;
=======
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public final class KoraQuestPlugin extends JavaPlugin {
    private SafeScheduler scheduler;
    private QuestConfig questConfig;
    private MessageService messages;
    private PlayerDataService playerData;
    private QuestManager questManager;
<<<<<<< HEAD
    private IntegrationManager integrations;
    private AntiExploitService antiExploit;
    private BedrockFormService bedrockForms;
    private QuestGui questGui;
    private QuestEditor questEditor;
    private UpdateChecker updateChecker;
    private NetworkSyncService networkSync;
    private Metrics metrics;
    private Object placeholderExpansion;
    private Method placeholderApplyMethod;
    private Method placeholderUnregisterMethod;
=======
    private QuestGui questGui;
    private UpdateChecker updateChecker;
    private Metrics metrics;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b

    @Override
    public void onEnable() {
        try {
            PluginPaths.prepareLayout(this);
            scheduler = new SafeScheduler(this);
            questConfig = new QuestConfig(this);
            messages = new MessageService(this, questConfig);
<<<<<<< HEAD
            playerData = new PlayerDataService(this, questConfig);
            integrations = new IntegrationManager(this, questConfig);
            questManager = new QuestManager(this, questConfig, messages, playerData, integrations);
            antiExploit = new AntiExploitService(this, questConfig, playerData, integrations);
            bedrockForms = new BedrockFormService(this, questManager, questConfig, messages);
            questGui = new QuestGui(this, questManager, questConfig, messages, bedrockForms);
            questEditor = new QuestEditor(this, questManager, questConfig, messages);
            updateChecker = new UpdateChecker(this, questConfig, messages);
            networkSync = new NetworkSyncService(this, questConfig, playerData);

            QuestCommand commandHandler = new QuestCommand(this, questManager, questConfig, messages, questGui, questEditor, updateChecker);
=======
            playerData = new PlayerDataService(this);
            questManager = new QuestManager(this, questConfig, messages, playerData);
            questGui = new QuestGui(this, questManager, questConfig, messages);
            updateChecker = new UpdateChecker(this, questConfig, messages);

            QuestCommand commandHandler = new QuestCommand(this, questManager, questConfig, messages, questGui, updateChecker);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            PluginCommand command = Objects.requireNonNull(getCommand("quest"), "quest command is missing from plugin.yml");
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);

            Bukkit.getPluginManager().registerEvents(questGui, this);
<<<<<<< HEAD
            Bukkit.getPluginManager().registerEvents(questEditor, this);
            Bukkit.getPluginManager().registerEvents(integrations, this);
            Bukkit.getPluginManager().registerEvents(new QuestListener(this, questManager, playerData, antiExploit, integrations), this);
            Bukkit.getPluginManager().registerEvents(updateChecker, this);
            integrations.registerExternalObjectiveHooks(questManager);

            KoraQuestAPI.bootstrap(questManager);
            questManager.ensureCycles(false);
            for (Player online : Bukkit.getOnlinePlayers()) playerData.preloadPlayerAsync(online.getUniqueId());

            scheduler.runGlobalTimer(() -> questManager.ensureCycles(true), 1200L, 1200L);
            scheduler.runGlobalTimer(playerData::flushDirtyAsync, questConfig.databaseFlushTicks(), questConfig.databaseFlushTicks());
            scheduler.runGlobalTimer(() -> playerData.evictInactive(questConfig.cacheUnloadMillis()), 1200L, 1200L);
            scheduler.runGlobalTimer(() -> {
                antiExploit.cleanup();
                long now = System.currentTimeMillis();
                playerData.purgeDeliveredRewards(now - questConfig.deliveredRewardRetentionMillis());
                playerData.purgeNetworkEvents(now - 7L * 86_400_000L);
                Map<String, Long> historyCutoffs = new java.util.LinkedHashMap<>();
                questConfig.cycles().forEach((name, cycle) -> historyCutoffs.put(name, now - cycle.historyRetentionMillis()));
                playerData.purgeCycleHistory(historyCutoffs);
            }, 72_000L, 72_000L);
            scheduler.runGlobalTimer(() -> questManager.retryPendingRewards(null), questConfig.rewardRetryTicks(), questConfig.rewardRetryTicks());
            scheduler.runGlobalTimer(this::tickPlaytime, 1200L, 1200L);
            scheduler.runGlobalTimer(() -> integrations.tickEconomyObjectives(questManager), 100L, 100L);

            setupPlaceholderApi();
            setupMetrics();
            networkSync.start();
            updateChecker.start();
            printBanner();
=======
            Bukkit.getPluginManager().registerEvents(new QuestListener(questManager), this);
            Bukkit.getPluginManager().registerEvents(updateChecker, this);

            KoraQuestAPI.bootstrap(questManager);
            questManager.ensureCycle(false);
            scheduler.runGlobalTimer(() -> questManager.ensureCycle(true), 1200L, 1200L);
            scheduler.runGlobalTimer(playerData::flushDirtyAsync, questConfig.databaseFlushTicks(), questConfig.databaseFlushTicks());

            setupMetrics();
            printBanner();
            updateChecker.start();
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "KoraQuest could not be enabled safely. The plugin will be disabled.", throwable);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

<<<<<<< HEAD
    private void tickPlaytime() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduler.runEntity(player, () -> questManager.increment(player, ObjectiveType.PLAYTIME, "ANY", 1));
        }
    }

    private void setupPlaceholderApi() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return;
        try {
            Class<?> bridge = Class.forName("dev.ipseucz.koraquest.placeholder.PlaceholderBridge", true, getClassLoader());
            Method register = bridge.getMethod("register", KoraQuestPlugin.class, QuestManager.class);
            placeholderApplyMethod = bridge.getMethod("apply", Player.class, String.class);
            placeholderUnregisterMethod = bridge.getMethod("unregister", Object.class);
            placeholderExpansion = register.invoke(null, this, questManager);
        } catch (Throwable throwable) {
            placeholderExpansion = null;
            placeholderApplyMethod = null;
            placeholderUnregisterMethod = null;
            getLogger().log(Level.WARNING, "Could not register PlaceholderAPI expansion", throwable);
        }
    }

    @Override
    public void onDisable() {
        KoraQuestAPI.shutdown();
        if (networkSync != null) try { networkSync.close(); } catch (Throwable ignored) { }
        if (placeholderExpansion != null && placeholderUnregisterMethod != null) {
            try { placeholderUnregisterMethod.invoke(null, placeholderExpansion); } catch (Throwable ignored) { }
        }
        if (scheduler != null) scheduler.shutdown();
        if (metrics != null) try { metrics.shutdown(); } catch (Throwable throwable) { getLogger().log(Level.WARNING, "Could not stop bStats cleanly", throwable); }
        if (playerData != null) try { playerData.shutdown(); } catch (Throwable throwable) { getLogger().log(Level.WARNING, "Could not finish database shutdown cleanly", throwable); }
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    public void reloadPlugin(CommandSender sender) {
        try {
<<<<<<< HEAD
            String oldStorage = questConfig.storageType();
            questConfig.reload();
            messages.reload();
            antiExploit.reloadPlacedBlocks();
            questManager.ensureCycles(false);
            if (!oldStorage.equalsIgnoreCase(questConfig.storageType())) getLogger().warning("Storage type changes require a full server restart.");
=======
            questConfig.reload();
            messages.reload();
            questManager.ensureCycle(false);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            sendSafe(sender, "reloaded", Map.of());
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "Could not reload KoraQuest", throwable);
            sendSafe(sender, "reload-failed", Map.of());
        }
    }

<<<<<<< HEAD
    public String applyExternalPlaceholders(Player player, String input) {
        if (input == null) return "";
        if (player == null || placeholderApplyMethod == null) return input;
        try {
            Object result = placeholderApplyMethod.invoke(null, player, input);
            return result instanceof String value ? value : input;
        } catch (Throwable ignored) { return input; }
    }

    public void sendSafe(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null || messages == null) return;
        if (sender instanceof Player player) scheduler.runEntity(player, () -> messages.send(player, key, placeholders));
        else scheduler.runGlobal(() -> messages.send(sender, key, placeholders));
    }

    private void setupMetrics() {
        if (!questConfig.metricsEnabled()) return;
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        try {
            metrics = new Metrics(this, 32623);
            metrics.addCustomChart(new SimplePie("platform", PlatformDetector::name));
            metrics.addCustomChart(new SingleLineChart("loaded_quests", () -> questConfig.quests().size()));
<<<<<<< HEAD
            metrics.addCustomChart(new SingleLineChart("cached_players", playerData::cachedPlayerCount));
            metrics.addCustomChart(new SimplePie("storage", questConfig::storageType));
            metrics.addCustomChart(new SimplePie("bedrock_forms", () -> questConfig.bedrockFormsEnabled() ? "enabled" : "disabled"));
=======
            metrics.addCustomChart(new SimplePie("storage", () -> "SQLite"));
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        } catch (Throwable throwable) {
            getLogger().log(Level.WARNING, "Could not start bStats metrics", throwable);
        }
    }

    private void printBanner() {
        String[] lines = {
<<<<<<< HEAD
                "  ██╗  ██╗ ██████╗ ██████╗  █████╗ ",
                "  ██║ ██╔╝██╔═══██╗██╔══██╗██╔══██╗",
                "  █████╔╝ ██║   ██║██████╔╝███████║",
                "  ██╔═██╗ ██║   ██║██╔══██╗██╔══██║",
                "  ██║  ██╗╚██████╔╝██║  ██║██║  ██║",
                "  ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝"
        };
        for (String line : lines) Bukkit.getConsoleSender().sendMessage(Text.color("&#FFB84D" + line));
        Bukkit.getConsoleSender().sendMessage(Text.color("&#FFB84D&lKoraQuest &8• &f" + getDescription().getVersion()
                + " &8• &#7DE2FF" + PlatformDetector.name() + " &8• &#75FF75" + questConfig.storageType()));
        Bukkit.getConsoleSender().sendMessage(Text.color("&7Quests: &f" + questConfig.quests().size() + " &8• &7Cycles: &f" + questConfig.cycles().size() + " &8• &7Cache: &fLazy"));
        if (!questConfig.validationErrors().isEmpty()) getLogger().warning("Quest validation issues: " + questConfig.validationErrors().size() + " (use /quest admin validate)");
    }

    public SafeScheduler scheduler() { return scheduler; }
    public QuestConfig questConfig() { return questConfig; }
    public MessageService messages() { return messages; }
    public QuestManager questManager() { return questManager; }
    public PlayerDataService playerData() { return playerData; }
    public IntegrationManager integrations() { return integrations; }
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
}
