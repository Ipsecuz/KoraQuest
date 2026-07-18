package dev.ipseucz.koraquest.bedrock;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.cycle.CycleSettings;
import dev.ipseucz.koraquest.cycle.CycleState;
import dev.ipseucz.koraquest.data.PlayerQuestData;
import dev.ipseucz.koraquest.model.QuestDefinition;
import dev.ipseucz.koraquest.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

/** Native Floodgate/Cumulus forms loaded reflectively to keep Floodgate optional. */
public final class BedrockFormService {
    private final KoraQuestPlugin plugin;
    private final QuestManager manager;
    private final QuestConfig config;
    private final MessageService messages;
    private volatile boolean warned;

    public BedrockFormService(KoraQuestPlugin plugin, QuestManager manager, QuestConfig config, MessageService messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
        this.messages = messages;
    }

    public boolean isBedrock(Player player) {
        if (!config.bedrockFormsEnabled() || player == null || !Bukkit.getPluginManager().isPluginEnabled("floodgate")) return false;
        try {
            Object api = floodgateApi();
            Object value = api.getClass().getMethod("isFloodgatePlayer", java.util.UUID.class).invoke(api, player.getUniqueId());
            return Boolean.TRUE.equals(value);
        } catch (Throwable throwable) {
            warnOnce(throwable);
            return false;
        }
    }

    public boolean open(Player player) {
        if (!isBedrock(player)) return false;
        List<CycleSettings> enabled = config.cycles().values().stream().filter(CycleSettings::enabled).toList();
        if (enabled.size() <= 1) {
            openQuestList(player, enabled.isEmpty() ? "daily" : enabled.getFirst().name(), 0, View.ALL);
        } else {
            openCycleList(player, enabled);
        }
        return true;
    }

    private void openCycleList(Player player, List<CycleSettings> cycles) {
        List<Runnable> actions = new ArrayList<>();
        try {
            Object builder = simpleBuilder();
            call(builder, "title", clean(messages.raw("bedrock.cycles.title")));
            StringBuilder content = new StringBuilder(clean(messages.raw("bedrock.cycles.content")));
            for (CycleSettings settings : cycles) {
                call(builder, "button", clean(settings.displayName()) + "\n" + clean(manager.resetTimeText(settings.name())));
                actions.add(() -> openQuestList(player, settings.name(), 0, View.ALL));
            }
            call(builder, "content", content.toString());
            attachHandler(builder, response -> plugin.scheduler().runEntity(player, () -> runSelected(response, actions)));
            send(player, call(builder, "build"));
        } catch (Throwable throwable) {
            warnOnce(throwable);
        }
    }

    private void openQuestList(Player player, String cycle, int requestedPage, View view) {
        try {
            List<QuestDefinition> quests = manager.quests(player, cycle).stream()
                    .filter(quest -> matchesView(player, quest, view))
                    .sorted(Comparator.comparing(QuestDefinition::difficulty).thenComparing(QuestDefinition::id))
                    .toList();
            int pageSize = 12;
            int maxPage = Math.max(0, (quests.size() - 1) / pageSize);
            int page = Math.max(0, Math.min(requestedPage, maxPage));
            int from = page * pageSize;
            int to = Math.min(quests.size(), from + pageSize);
            List<Runnable> actions = new ArrayList<>();
            Object builder = simpleBuilder();
            CycleSettings settings = config.cycle(cycle);
            call(builder, "title", clean(settings == null ? cycle : settings.displayName()));
            call(builder, "content", clean(messages.raw("bedrock.list.content"))
                    .replace("%page%", String.valueOf(page + 1))
                    .replace("%max_page%", String.valueOf(maxPage + 1))
                    .replace("%reset_time%", clean(manager.resetTimeText(cycle))));
            for (int index = from; index < to; index++) {
                QuestDefinition quest = quests.get(index);
                int progress = manager.progress(player.getUniqueId(), quest.id());
                String status = localizedStatus(manager.status(player.getUniqueId(), quest.id()));
                String label = clean(messages.questName(quest)) + "\n" + progress + "/" + quest.required() + " • " + clean(status);
                call(builder, "button", label);
                actions.add(() -> openQuest(player, quest, page, view));
            }
            if (page > 0) {
                call(builder, "button", clean(messages.raw("bedrock.buttons.previous")));
                actions.add(() -> openQuestList(player, cycle, page - 1, view));
            }
            if (page < maxPage) {
                call(builder, "button", clean(messages.raw("bedrock.buttons.next")));
                actions.add(() -> openQuestList(player, cycle, page + 1, view));
            }
            call(builder, "button", clean(messages.raw("bedrock.buttons.filter"))
                    .replace("%filter%", clean(localizedFilter(view))));
            actions.add(() -> openQuestList(player, cycle, 0, view.next()));
            call(builder, "button", clean(messages.raw("bedrock.buttons.claim-all")));
            actions.add(() -> { manager.claimAll(player, cycle); openQuestList(player, cycle, page, view); });
            if (config.cycles().size() > 1) {
                call(builder, "button", clean(messages.raw("bedrock.buttons.cycles")));
                actions.add(() -> openCycleList(player, config.cycles().values().stream().filter(CycleSettings::enabled).toList()));
            }
            attachHandler(builder, response -> plugin.scheduler().runEntity(player, () -> runSelected(response, actions)));
            send(player, call(builder, "build"));
        } catch (Throwable throwable) {
            warnOnce(throwable);
        }
    }

    private void openQuest(Player player, QuestDefinition quest, int page, View view) {
        try {
            PlayerQuestData data = manager.playerData(player.getUniqueId());
            CycleState cycle = manager.playerDataService().cycle(quest.cycle());
            if (cycle == null) return;
            int progress = data.progressOf(cycle.instanceId(), quest.id());
            boolean active = data.isActive(cycle.instanceId(), quest.id());
            boolean completed = data.isCompleted(cycle.instanceId(), quest.id());
            boolean ready = active && progress >= quest.required();
            Object builder = simpleBuilder();
            call(builder, "title", clean(messages.questName(quest)));
            Map<String, String> placeholders = manager.placeholders(player, quest, progress);
            StringBuilder content = new StringBuilder();
            for (String line : quest.lore()) {
                content.append(clean(plugin.applyExternalPlaceholders(player, Text.placeholders(line, placeholders)))).append('\n');
            }
            content.append("\nLoại nhiệm vụ: ").append(clean(placeholders.get("%cycle_name%")))
                    .append("\nĐộ khó: ").append(clean(placeholders.get("%difficulty_name%")))
                    .append("\nThời gian còn lại: ").append(clean(placeholders.get("%quest_reset_time%")))
                    .append("\nTiến độ: ").append(progress).append('/').append(quest.required()).append('\n')
                    .append(strip(manager.progressBar(progress, quest.required(), 18)));
            call(builder, "content", content.toString());
            List<Runnable> actions = new ArrayList<>();
            if (completed) {
                call(builder, "button", clean(messages.raw("bedrock.buttons.completed")));
                actions.add(() -> openQuestList(player, quest.cycle(), page, view));
            } else if (ready) {
                call(builder, "button", clean(messages.raw("bedrock.buttons.claim")));
                actions.add(() -> { manager.complete(player, quest.id()); openQuestList(player, quest.cycle(), page, view); });
            } else if (active) {
                call(builder, "button", clean(messages.raw("bedrock.buttons.cancel")));
                actions.add(() -> { manager.cancel(player, quest.id()); openQuestList(player, quest.cycle(), page, view); });
            } else {
                call(builder, "button", clean(messages.raw("bedrock.buttons.accept")));
                actions.add(() -> { manager.accept(player, quest.id()); openQuestList(player, quest.cycle(), page, view); });
            }
            if (!completed && quest.rerollable() && config.rerollEnabled()) {
                call(builder, "button", clean(messages.raw("bedrock.buttons.reroll")));
                actions.add(() -> { manager.reroll(player, quest.id()); openQuestList(player, quest.cycle(), page, view); });
            }
            call(builder, "button", clean(messages.raw("bedrock.buttons.back")));
            actions.add(() -> openQuestList(player, quest.cycle(), page, view));
            attachHandler(builder, response -> plugin.scheduler().runEntity(player, () -> runSelected(response, actions)));
            send(player, call(builder, "build"));
        } catch (Throwable throwable) {
            warnOnce(throwable);
        }
    }

    private String localizedStatus(String status) {
        String key = status == null ? "unavailable" : status.toLowerCase(Locale.ROOT);
        String fallback = switch (key) {
            case "completed" -> "Đã hoàn thành";
            case "ready" -> "Sẵn sàng nhận";
            case "active" -> "Đang làm";
            case "available" -> "Khả dụng";
            default -> "Không khả dụng";
        };
        return messages.rawOr("gui.status." + key, fallback);
    }

    private String localizedFilter(View view) {
        String key = view.name().toLowerCase(Locale.ROOT);
        String fallback = switch (view) {
            case ALL -> "Tất cả";
            case AVAILABLE -> "Khả dụng";
            case ACTIVE -> "Đang làm";
            case READY -> "Sẵn sàng";
            case COMPLETED -> "Hoàn thành";
        };
        return messages.rawOr("gui.filters." + key, fallback);
    }

    private boolean matchesView(Player player, QuestDefinition quest, View view) {
        CycleState state = manager.playerDataService().cycle(quest.cycle());
        if (state == null) return false;
        PlayerQuestData data = manager.playerData(player.getUniqueId());
        boolean active = data.isActive(state.instanceId(), quest.id());
        boolean completed = data.isCompleted(state.instanceId(), quest.id());
        boolean ready = active && data.progressOf(state.instanceId(), quest.id()) >= quest.required();
        return switch (view) {
            case ALL -> true;
            case AVAILABLE -> !active && !completed;
            case ACTIVE -> active && !ready;
            case READY -> ready;
            case COMPLETED -> completed;
        };
    }

    private Object floodgateApi() throws Exception {
        Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
        return apiClass.getMethod("getInstance").invoke(null);
    }

    private Object simpleBuilder() throws Exception {
        Class<?> simpleForm = Class.forName("org.geysermc.cumulus.form.SimpleForm");
        return simpleForm.getMethod("builder").invoke(null);
    }

    private void attachHandler(Object builder, Consumer<Object> handler) throws Exception {
        Method target = null;
        for (Method method : builder.getClass().getMethods()) {
            if ((method.getName().equals("validResultHandler") || method.getName().equals("responseHandler"))
                    && method.getParameterCount() == 1 && Consumer.class.isAssignableFrom(method.getParameterTypes()[0])) {
                target = method; break;
            }
        }
        if (target == null) throw new NoSuchMethodException("Cumulus validResultHandler");
        target.invoke(builder, handler);
    }

    private void runSelected(Object response, List<Runnable> actions) {
        int selected = clickedId(response);
        if (selected >= 0 && selected < actions.size()) actions.get(selected).run();
    }

    private int clickedId(Object response) {
        if (response == null) return -1;
        for (String name : List.of("clickedButtonId", "clickedButtonIndex")) {
            try { return ((Number) response.getClass().getMethod(name).invoke(response)).intValue(); }
            catch (Throwable ignored) { }
        }
        try {
            Object button = response.getClass().getMethod("clickedButton").invoke(response);
            return ((Number) button.getClass().getMethod("index").invoke(button)).intValue();
        } catch (Throwable ignored) { return -1; }
    }

    private void send(Player player, Object form) throws Exception {
        Object api = floodgateApi();
        Method selected = null;
        for (Method method : api.getClass().getMethods()) {
            if (!method.getName().equals("sendForm") || method.getParameterCount() != 2) continue;
            if (method.getParameterTypes()[0].isAssignableFrom(java.util.UUID.class)
                    || method.getParameterTypes()[0] == java.util.UUID.class) { selected = method; break; }
        }
        if (selected == null) throw new NoSuchMethodException("FloodgateApi.sendForm(UUID, Form)");
        selected.invoke(api, player.getUniqueId(), form);
    }

    private Object call(Object target, String name, Object... args) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            try { return method.invoke(target, args); } catch (IllegalArgumentException ignored) { }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name);
    }

    private String clean(String input) { return strip(Text.color(input == null ? "" : input)); }
    private String strip(String input) { return input == null ? "" : input.replaceAll("(?i)§[0-9A-FK-ORX]", ""); }

    private void warnOnce(Throwable throwable) {
        if (warned) return;
        warned = true;
        plugin.getLogger().log(Level.WARNING, "Could not open the native Bedrock quest form; Java GUI fallback remains available", throwable);
    }

    private enum View {
        ALL, AVAILABLE, ACTIVE, READY, COMPLETED;
        private View next() { return values()[(ordinal() + 1) % values().length]; }
    }
}
