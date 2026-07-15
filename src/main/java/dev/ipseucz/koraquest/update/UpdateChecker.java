package dev.ipseucz.koraquest.update;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class UpdateChecker implements Listener {
    private static final long SIX_HOURS_TICKS = 6L * 60L * 60L * 20L;
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final MessageService messages;
    private volatile String latestVersion;
    private volatile boolean updateAvailable;
    private volatile boolean checked;

    public UpdateChecker(KoraQuestPlugin plugin, QuestConfig config, MessageService messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    public void start() {
        if (!config.updateCheckerEnabled()) {
            return;
        }
        plugin.scheduler().runAsyncLater(() -> check(null, false), 5L, TimeUnit.SECONDS);
        plugin.scheduler().runGlobalTimer(() -> plugin.scheduler().runAsync(() -> check(null, false)), SIX_HOURS_TICKS, SIX_HOURS_TICKS);
    }

    public void checkNow(CommandSender requester) {
        if (!config.updateCheckerEnabled()) {
            plugin.sendSafe(requester, "update-disabled", Map.of());
            return;
        }
        if (config.spigotResourceId() <= 0) {
            plugin.sendSafe(requester, "update-not-configured", Map.of());
            return;
        }
        plugin.sendSafe(requester, "update-checking", Map.of());
        plugin.scheduler().runAsync(() -> check(requester, true));
    }

    private void check(CommandSender requester, boolean reportCurrent) {
        int resourceId = config.spigotResourceId();
        if (resourceId <= 0) {
            if (requester != null) {
                plugin.sendSafe(requester, "update-not-configured", Map.of());
            } else if (config.notifyConsoleUpdate()) {
                plugin.getLogger().info("Update checker is ready but update-checker.resource-id is 0. Set it after publishing the SpigotMC resource.");
            }
            return;
        }
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "KoraQuest/" + currentVersion());
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("HTTP " + status);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String response = reader.readLine();
                if (response == null || response.isBlank() || response.length() > 64) {
                    throw new IllegalStateException("Invalid version response");
                }
                latestVersion = response.trim();
            }
            checked = true;
            updateAvailable = compareVersions(latestVersion, currentVersion()) > 0;
            Map<String, String> placeholders = versionPlaceholders();
            if (requester != null) {
                plugin.sendSafe(requester, updateAvailable ? "update-available" : "update-current", placeholders);
            } else if (updateAvailable && config.notifyConsoleUpdate()) {
                plugin.scheduler().runGlobal(() -> plugin.getLogger().warning(
                        "A KoraQuest update is available: " + latestVersion + " (current: " + currentVersion() + ")"));
            } else if (reportCurrent && config.notifyConsoleUpdate()) {
                plugin.scheduler().runGlobal(() -> plugin.getLogger().info("KoraQuest is up to date: " + currentVersion()));
            }
        } catch (Exception exception) {
            if (requester != null) {
                plugin.sendSafe(requester, "update-check-failed", Map.of());
            }
            plugin.getLogger().log(Level.WARNING, "Could not check for a KoraQuest update: " + exception.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!checked || !updateAvailable || !player.hasPermission(config.updateNotifyPermission())) {
            return;
        }
        plugin.scheduler().runEntityLater(player, () -> {
            if (player.isOnline() && updateAvailable) {
                messages.send(player, "update-available", versionPlaceholders());
            }
        }, 40L);
    }

    public Map<String, String> versionPlaceholders() {
        return Map.of(
                "%current%", currentVersion(),
                "%latest%", latestVersion == null ? "?" : latestVersion
        );
    }

    private String currentVersion() {
        return plugin.getDescription().getVersion();
    }

    static int compareVersions(String left, String right) {
        int[] a = numericParts(left);
        int[] b = numericParts(right);
        int length = Math.max(a.length, b.length);
        for (int index = 0; index < length; index++) {
            int av = index < a.length ? a[index] : 0;
            int bv = index < b.length ? b[index] : 0;
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }
        return 0;
    }

    private static int[] numericParts(String version) {
        if (version == null) {
            return new int[0];
        }
        String normalized = version.replaceAll("[^0-9]+", ".");
        String[] split = normalized.split("\\.");
        return java.util.Arrays.stream(split)
                .filter(part -> !part.isBlank())
                .mapToInt(part -> {
                    try {
                        return Integer.parseInt(part);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .toArray();
    }
}
