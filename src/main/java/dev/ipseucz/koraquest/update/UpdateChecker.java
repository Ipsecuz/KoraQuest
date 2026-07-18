package dev.ipseucz.koraquest.update;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
<<<<<<< HEAD
import dev.ipseucz.koraquest.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
=======
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
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
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
=======
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

<<<<<<< HEAD
/** Checks the official SpigotMC legacy version endpoint and notifies only authorized administrators. */
public final class UpdateChecker implements Listener {
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final MessageService messages;
    private volatile String latestVersion = "?";
    private volatile boolean updateAvailable;
    private volatile boolean checked;
    private volatile long lastCheckedAt;
    private volatile String announcedVersion = "";
=======
public final class UpdateChecker implements Listener {
    private static final long SIX_HOURS_TICKS = 6L * 60L * 60L * 20L;
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final MessageService messages;
    private volatile String latestVersion;
    private volatile boolean updateAvailable;
    private volatile boolean checked;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b

    public UpdateChecker(KoraQuestPlugin plugin, QuestConfig config, MessageService messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    public void start() {
<<<<<<< HEAD
        if (!config.updateCheckerEnabled()) return;
        plugin.scheduler().runAsyncLater(() -> check(null, false), 5L, TimeUnit.SECONDS);
        long interval = config.updateCheckIntervalTicks();
        plugin.scheduler().runGlobalTimer(() -> plugin.scheduler().runAsync(() -> check(null, false)), interval, interval);
    }

    public void checkNow(CommandSender requester) {
        if (!config.updateCheckerEnabled()) { plugin.sendSafe(requester, "update-disabled", Map.of()); return; }
        if (config.spigotResourceId() <= 0) { plugin.sendSafe(requester, "update-not-configured", Map.of()); return; }
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        plugin.sendSafe(requester, "update-checking", Map.of());
        plugin.scheduler().runAsync(() -> check(requester, true));
    }

    private void check(CommandSender requester, boolean reportCurrent) {
        int resourceId = config.spigotResourceId();
        if (resourceId <= 0) {
<<<<<<< HEAD
            if (requester != null) plugin.sendSafe(requester, "update-not-configured", Map.of());
            else if (config.notifyConsoleUpdate()) plugin.getLogger().info("Update checker awaits update-checker.resource-id after the SpigotMC resource is published.");
=======
            if (requester != null) {
                plugin.sendSafe(requester, "update-not-configured", Map.of());
            } else if (config.notifyConsoleUpdate()) {
                plugin.getLogger().info("Update checker is ready but update-checker.resource-id is 0. Set it after publishing the SpigotMC resource.");
            }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            return;
        }
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
<<<<<<< HEAD
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "text/plain");
            connection.setRequestProperty("User-Agent", "KoraQuest/" + currentVersion() + " (SpigotMC Update Checker)");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("SpigotMC returned HTTP " + status);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String response = reader.readLine();
                if (response == null || response.isBlank() || response.length() > 64 || !response.matches("[0-9A-Za-z._+\\-]+")) {
                    throw new IllegalStateException("SpigotMC returned an invalid version string");
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
                }
                latestVersion = response.trim();
            }
            checked = true;
<<<<<<< HEAD
            lastCheckedAt = System.currentTimeMillis();
            updateAvailable = compareVersions(latestVersion, currentVersion()) > 0;
            if (requester instanceof Player player) {
                plugin.scheduler().runEntity(player, () -> showPlayerResult(player, reportCurrent));
            } else if (requester != null) {
                plugin.sendSafe(requester, updateAvailable ? "update-available-console" : "update-current", versionPlaceholders());
            } else if (updateAvailable) {
                announceUpdateOnce();
=======
            updateAvailable = compareVersions(latestVersion, currentVersion()) > 0;
            Map<String, String> placeholders = versionPlaceholders();
            if (requester != null) {
                plugin.sendSafe(requester, updateAvailable ? "update-available" : "update-current", placeholders);
            } else if (updateAvailable && config.notifyConsoleUpdate()) {
                plugin.scheduler().runGlobal(() -> plugin.getLogger().warning(
                        "A KoraQuest update is available: " + latestVersion + " (current: " + currentVersion() + ")"));
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            } else if (reportCurrent && config.notifyConsoleUpdate()) {
                plugin.scheduler().runGlobal(() -> plugin.getLogger().info("KoraQuest is up to date: " + currentVersion()));
            }
        } catch (Exception exception) {
<<<<<<< HEAD
            if (requester != null) plugin.sendSafe(requester, "update-check-failed", Map.of("%error%", safeError(exception)));
            plugin.getLogger().log(Level.WARNING, "Could not check for a KoraQuest update: " + exception.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
=======
            if (requester != null) {
                plugin.sendSafe(requester, "update-check-failed", Map.of());
            }
            plugin.getLogger().log(Level.WARNING, "Could not check for a KoraQuest update: " + exception.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
<<<<<<< HEAD
        if (!checked || !updateAvailable || !player.hasPermission(config.updateNotifyPermission())) return;
        plugin.scheduler().runEntityLater(player, () -> { if (player.isOnline() && updateAvailable) showUpdateCard(player); }, 50L);
    }

    private void showPlayerResult(Player player, boolean reportCurrent) {
        if (updateAvailable) showUpdateCard(player);
        else if (reportCurrent) messages.send(player, "update-current", versionPlaceholders());
    }

    private void showUpdateCard(Player player) {
        Map<String, String> placeholders = versionPlaceholders();
        for (String line : messages.formatList("messages.update-card", placeholders)) player.sendMessage(line);
        Component button = LegacyComponentSerializer.legacySection().deserialize(messages.format("messages.update-button", placeholders))
                .clickEvent(ClickEvent.openUrl(config.updateDownloadUrl()))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection()
                        .deserialize(messages.format("messages.update-hover", placeholders))));
        player.sendMessage(button);
        player.sendMessage(messages.format("messages.update-border", placeholders));
    }

    private synchronized void announceUpdateOnce() {
        if (!updateAvailable || latestVersion.equalsIgnoreCase(announcedVersion)) return;
        announcedVersion = latestVersion;
        plugin.scheduler().runGlobal(() -> {
            if (config.notifyConsoleUpdate()) printConsoleUpdate();
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.hasPermission(config.updateNotifyPermission())) continue;
                plugin.scheduler().runEntity(online, () -> {
                    if (online.isOnline() && updateAvailable) showUpdateCard(online);
                });
            }
        });
    }

    private void printConsoleUpdate() {
        plugin.getLogger().warning("================================================");
        plugin.getLogger().warning("KoraQuest update available: " + latestVersion);
        plugin.getLogger().warning("Installed version: " + currentVersion());
        plugin.getLogger().warning("Download: " + config.updateDownloadUrl());
        plugin.getLogger().warning("================================================");
    }

    public Map<String, String> versionPlaceholders() {
        return Map.of("%current%", currentVersion(), "%latest%", latestVersion,
                "%download_url%", config.updateDownloadUrl(), "%last_checked%", String.valueOf(lastCheckedAt));
    }

    private String currentVersion() { return plugin.getDescription().getVersion(); }
    private String safeError(Exception exception) { return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(); }

    static int compareVersions(String left, String right) {
        ParsedVersion a = ParsedVersion.parse(left);
        ParsedVersion b = ParsedVersion.parse(right);
        int length = Math.max(a.numbers().size(), b.numbers().size());
        for (int index = 0; index < length; index++) {
            int av = index < a.numbers().size() ? a.numbers().get(index) : 0;
            int bv = index < b.numbers().size() ? b.numbers().get(index) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        if (a.preRelease().isBlank() && !b.preRelease().isBlank()) return 1;
        if (!a.preRelease().isBlank() && b.preRelease().isBlank()) return -1;
        return comparePreRelease(a.preRelease(), b.preRelease());
    }

    private static int comparePreRelease(String left, String right) {
        String[] a = left.split("[._-]");
        String[] b = right.split("[._-]");
        int length = Math.max(a.length, b.length);
        for (int index = 0; index < length; index++) {
            if (index >= a.length) return -1;
            if (index >= b.length) return 1;
            String av = a[index];
            String bv = b[index];
            boolean an = av.matches("\\d+");
            boolean bn = bv.matches("\\d+");
            int compared;
            if (an && bn) {
                try { compared = Integer.compare(Integer.parseInt(av), Integer.parseInt(bv)); }
                catch (NumberFormatException ignored) { compared = av.length() != bv.length() ? Integer.compare(av.length(), bv.length()) : av.compareTo(bv); }
            } else if (an != bn) {
                compared = an ? -1 : 1;
            } else {
                compared = av.compareToIgnoreCase(bv);
            }
            if (compared != 0) return compared;
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        }
        return 0;
    }

<<<<<<< HEAD
    private record ParsedVersion(List<Integer> numbers, String preRelease) {
        static ParsedVersion parse(String raw) {
            String value = raw == null ? "0" : raw.trim().toLowerCase(Locale.ROOT).replaceFirst("^v", "");
            int dash = value.indexOf('-');
            String stable = dash < 0 ? value : value.substring(0, dash);
            String pre = dash < 0 ? "" : value.substring(dash + 1);
            List<Integer> numbers = new ArrayList<>();
            for (String part : stable.split("[^0-9]+")) if (!part.isBlank()) {
                try { numbers.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) { numbers.add(0); }
            }
            return new ParsedVersion(List.copyOf(numbers), pre);
        }
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }
}
