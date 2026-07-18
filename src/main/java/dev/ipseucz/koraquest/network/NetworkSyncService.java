package dev.ipseucz.koraquest.network;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.data.PlayerDataService;
import dev.ipseucz.koraquest.storage.DatabaseStorage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Cross-server invalidation service. SQL remains authoritative. Redis is an
 * optional low-latency signal and is implemented with the RESP protocol so
 * server owners do not need another runtime library.
 */
public final class NetworkSyncService implements AutoCloseable {
    private final KoraQuestPlugin plugin;
    private final QuestConfig config;
    private final PlayerDataService data;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long lastDatabaseEvent = System.currentTimeMillis();
    private volatile String lastDatabaseEventId = "";
    private volatile Socket subscriberSocket;

    public NetworkSyncService(KoraQuestPlugin plugin, QuestConfig config, PlayerDataService data) {
        this.plugin = plugin;
        this.config = config;
        this.data = data;
        data.setNetworkNotifier(this::publishRedis);
    }

    public void start() {
        if (!config.networkSyncEnabled() || !running.compareAndSet(false, true)) return;
        plugin.scheduler().runGlobalTimer(this::pollDatabase, config.networkPollTicks(), config.networkPollTicks());
        if (config.redisEnabled()) plugin.scheduler().runAsync(this::subscribeRedis);
    }

    private void pollDatabase() {
        long from = lastDatabaseEvent;
        String fromId = lastDatabaseEventId;
        data.networkEventsAfter(from, fromId).whenComplete((events, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "Could not poll KoraQuest network events", error);
                return;
            }
            process(events);
        });
    }

    private void process(List<DatabaseStorage.NetworkEvent> events) {
        boolean reloadCycles = false;
        for (DatabaseStorage.NetworkEvent event : events) {
            if (event.createdAt() > lastDatabaseEvent
                    || event.createdAt() == lastDatabaseEvent && event.eventId().compareTo(lastDatabaseEventId) > 0) {
                lastDatabaseEvent = event.createdAt();
                lastDatabaseEventId = event.eventId();
            }
            if ("CYCLE_RESET".equalsIgnoreCase(event.type())) reloadCycles = true;
        }
        if (reloadCycles) data.reloadCyclesFromStorage();
    }

    private void publishRedis(String type, String payload) {
        if (!running.get() || !config.redisEnabled()) return;
        plugin.scheduler().runAsync(() -> {
            RedisAddress address = RedisAddress.parse(config.redisUri());
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address.host(), address.port()), 3000);
                socket.setSoTimeout(3000);
                BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
                BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
                if (address.password() != null) {
                    writeCommand(output, "AUTH", address.password());
                    readResp(input);
                }
                writeCommand(output, "PUBLISH", config.redisChannel(), type + "|" + payload);
                readResp(input);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.FINE, "Redis publish failed; SQL polling remains active", exception);
            }
        });
    }

    private void subscribeRedis() {
        RedisAddress address = RedisAddress.parse(config.redisUri());
        while (running.get()) {
            try (Socket socket = new Socket()) {
                subscriberSocket = socket;
                socket.connect(new InetSocketAddress(address.host(), address.port()), 5000);
                socket.setSoTimeout(30_000);
                BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
                BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
                if (address.password() != null) {
                    writeCommand(output, "AUTH", address.password());
                    readResp(input);
                }
                writeCommand(output, "SUBSCRIBE", config.redisChannel());
                while (running.get()) {
                    Object response = readResp(input);
                    if (!(response instanceof List<?> values) || values.size() < 3) continue;
                    if (!"message".equalsIgnoreCase(String.valueOf(values.get(0)))) continue;
                    String message = String.valueOf(values.get(2));
                    if (message.startsWith("CYCLE_RESET|")) data.reloadCyclesFromStorage();
                }
            } catch (Exception exception) {
                if (running.get()) {
                    plugin.getLogger().log(Level.FINE, "Redis subscriber reconnecting; SQL polling remains active", exception);
                    try { Thread.sleep(5000L); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
                }
            } finally {
                subscriberSocket = null;
            }
        }
    }

    private static void writeCommand(BufferedOutputStream output, String... parts) throws IOException {
        output.write(("*" + parts.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String part : parts) {
            byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
            output.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        output.flush();
    }

    private static Object readResp(BufferedInputStream input) throws IOException {
        int marker = input.read();
        if (marker < 0) throw new EOFException();
        String line = readLine(input);
        return switch (marker) {
            case '+' -> line;
            case '-' -> throw new IOException("Redis: " + line);
            case ':' -> Long.parseLong(line);
            case '$' -> {
                int length = Integer.parseInt(line);
                if (length < 0) yield null;
                byte[] bytes = input.readNBytes(length);
                readLine(input);
                yield new String(bytes, StandardCharsets.UTF_8);
            }
            case '*' -> {
                int length = Integer.parseInt(line);
                java.util.ArrayList<Object> values = new java.util.ArrayList<>(Math.max(0, length));
                for (int index = 0; index < length; index++) values.add(readResp(input));
                yield values;
            }
            default -> throw new IOException("Unsupported Redis response");
        };
    }

    private static String readLine(BufferedInputStream input) throws IOException {
        StringBuilder line = new StringBuilder();
        int previous = -1;
        while (true) {
            int current = input.read();
            if (current < 0) throw new EOFException();
            if (previous == '\r' && current == '\n') {
                line.setLength(Math.max(0, line.length() - 1));
                return line.toString();
            }
            line.append((char) current);
            previous = current;
        }
    }

    @Override
    public void close() {
        running.set(false);
        data.setNetworkNotifier(null);
        Socket socket = subscriberSocket;
        if (socket != null) try { socket.close(); } catch (IOException ignored) { }
    }

    private record RedisAddress(String host, int port, String password) {
        static RedisAddress parse(String raw) {
            URI uri = URI.create(raw == null || raw.isBlank() ? "redis://127.0.0.1:6379" : raw);
            String password = null;
            if (uri.getUserInfo() != null) {
                int colon = uri.getUserInfo().indexOf(':');
                password = colon >= 0 ? uri.getUserInfo().substring(colon + 1) : uri.getUserInfo();
            }
            return new RedisAddress(uri.getHost() == null ? "127.0.0.1" : uri.getHost(), uri.getPort() < 0 ? 6379 : uri.getPort(), password);
        }
    }
}
