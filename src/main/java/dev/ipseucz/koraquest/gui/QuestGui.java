package dev.ipseucz.koraquest.gui;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.bedrock.BedrockFormService;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.cycle.CycleSettings;
import dev.ipseucz.koraquest.cycle.CycleState;
import dev.ipseucz.koraquest.data.PlayerQuestData;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
import dev.ipseucz.koraquest.util.ItemBuilder;
import dev.ipseucz.koraquest.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestGui implements Listener {
    private static final long CLICK_DEBOUNCE_NANOS = 150_000_000L;
    private final KoraQuestPlugin plugin;
    private final QuestManager manager;
    private final QuestConfig config;
    private final MessageService messages;
    private final BedrockFormService bedrock;
    private final Map<UUID, Long> lastClicks = new ConcurrentHashMap<>();
    private final Map<UUID, ViewState> states = new ConcurrentHashMap<>();

    public QuestGui(KoraQuestPlugin plugin, QuestManager manager, QuestConfig config, MessageService messages,
                    BedrockFormService bedrock) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
        this.messages = messages;
        this.bedrock = bedrock;
    }

    public void open(Player player) {
        if (player == null || !player.hasPermission("koraquest.command.open")) {
            if (player != null) messages.send(player, "no-permission");
            return;
        }
        withLoadedData(player, () -> {
            if (bedrock != null && bedrock.open(player)) return;
            openJava(player, states.getOrDefault(player.getUniqueId(), ViewState.initial(defaultCycle(player))));
        });
    }

    public void open(Player player, String cycle, String search) {
        if (player == null || !player.hasPermission("koraquest.command.open")) return;
        withLoadedData(player, () -> {
            ViewState current = states.getOrDefault(player.getUniqueId(), ViewState.initial(defaultCycle(player)));
            openJava(player, new ViewState(validCycle(cycle, player), 0, current.view(), current.sort(), search == null ? "" : search.trim()));
        });
    }

    private void withLoadedData(Player player, Runnable action) {
        manager.playerDataService().preloadPlayerAsync(player.getUniqueId()).whenComplete((ignored, throwable) ->
                plugin.scheduler().runEntity(player, () -> {
                    if (!player.isOnline()) return;
                    if (throwable != null) {
                        messages.send(player, "data-load-failed");
                        return;
                    }
                    action.run();
                }));
    }

    private String defaultCycle(Player player) {
        return config.cycles().values().stream()
                .filter(CycleSettings::enabled)
                .filter(cycle -> cycle.permission().isBlank() || player.hasPermission(cycle.permission()))
                .map(CycleSettings::name).findFirst().orElse("daily");
    }

    private String validCycle(String requested, Player player) {
        CycleSettings settings = config.cycle(requested);
        if (settings == null || !settings.enabled() || (!settings.permission().isBlank() && !player.hasPermission(settings.permission()))) return defaultCycle(player);
        return settings.name();
    }

    private void openJava(Player player, ViewState requested) {
        if (player == null || !player.isOnline()) return;
        String cycle = validCycle(requested.cycle(), player);
        List<QuestDefinition> quests = filtered(player, cycle, requested.view(), requested.sort(), requested.search());
        List<Integer> questSlots = config.questSlots();
        int pageSize = Math.max(1, questSlots.size());
        int maxPage = Math.max(0, (quests.size() - 1) / pageSize);
        int page = Math.max(0, Math.min(requested.page(), maxPage));
        ViewState state = new ViewState(cycle, page, requested.view(), requested.sort(), requested.search());
        states.put(player.getUniqueId(), state);

        QuestMenuHolder holder = new QuestMenuHolder(player.getUniqueId(), state, maxPage);
        String title = apply(player, messages.raw("gui.title"), placeholders(player, state, maxPage));
        Inventory inventory = Bukkit.createInventory(holder, config.guiSize(), Text.color(title));
        holder.inventory = inventory;
        ItemStack filler = new ItemBuilder(config.fillerMaterial()).name(messages.rawOr("gui.filler-name", " ")).build();
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);

        Map<String, String> values = placeholders(player, state, maxPage);
        renderCycles(player, inventory, holder, cycle, values);
        setAction(inventory, holder, config.guiSlot("gui.filter", 46), Action.FILTER,
                item(player, "gui.filter", Material.HOPPER, values));
        setAction(inventory, holder, config.guiSlot("gui.sort", 47), Action.SORT,
                item(player, "gui.sort", Material.COMPARATOR, values));
        setAction(inventory, holder, config.guiSlot("gui.search", 48), Action.SEARCH,
                item(player, "gui.search", Material.OAK_SIGN, values));
        setAction(inventory, holder, config.guiSlot("gui.claim-all", 49), Action.CLAIM_ALL,
                item(player, "gui.claim-all", Material.CHEST_MINECART, values));
        setAction(inventory, holder, config.guiSlot("gui.info", 50), Action.INFO,
                item(player, "gui.info", Material.CLOCK, values));
        if (page > 0) setAction(inventory, holder, config.guiSlot("gui.previous-page", 45), Action.PREVIOUS,
                item(player, "gui.previous-page", Material.ARROW, values));
        if (page < maxPage) setAction(inventory, holder, config.guiSlot("gui.next-page", 53), Action.NEXT,
                item(player, "gui.next-page", Material.ARROW, values));

        int from = page * pageSize;
        int to = Math.min(quests.size(), from + pageSize);
        for (int index = from; index < to; index++) {
            int slot = questSlots.get(index - from);
            if (slot < 0 || slot >= inventory.getSize()) continue;
            QuestDefinition quest = quests.get(index);
            inventory.setItem(slot, questItem(player, quest));
            holder.questSlots.put(slot, quest.id());
        }
        player.openInventory(inventory);
    }

    private void renderCycles(Player player, Inventory inventory, QuestMenuHolder holder, String current, Map<String, String> values) {
        List<CycleSettings> visibleCycles = config.cycles().values().stream()
                .filter(CycleSettings::enabled)
                .filter(settings -> settings.permission().isBlank() || player.hasPermission(settings.permission()))
                .limit(8)
                .toList();
        int[] slots = cycleSlotsFor(visibleCycles.size());
        for (int index = 0; index < visibleCycles.size(); index++) {
            CycleSettings settings = visibleCycles.get(index);
            int slot = slots[index];
            Map<String, String> map = new HashMap<>(values);
            map.put("%cycle%", settings.name());
            map.put("%cycle_name%", settings.displayName());
            String cycleResetTime = manager.resetTimeText(settings.name());
            map.put("%reset_time%", cycleResetTime);
            map.put("%cycle_reset_time%", cycleResetTime);
            map.put("%cycle_selected%", settings.name().equals(current)
                    ? messages.rawOr("gui.cycle-selected", "&#75FF75Đang chọn") : "");
            inventory.setItem(slot, new ItemBuilder(settings.name().equals(current) ? Material.ENDER_EYE : Material.ENDER_PEARL)
                    .name(apply(player, messages.raw("gui.cycle.name"), map))
                    .lore(parse(player, "gui.cycle.lore", map)).build());
            holder.cycleSlots.put(slot, settings.name());
        }
    }

    private int[] cycleSlotsFor(int count) {
        return switch (Math.max(0, Math.min(8, count))) {
            case 1 -> new int[]{4};
            case 2 -> new int[]{3, 5};
            case 3 -> new int[]{2, 4, 6};
            case 4 -> new int[]{1, 3, 5, 7};
            case 5 -> new int[]{0, 2, 4, 6, 8};
            case 6 -> new int[]{0, 1, 3, 5, 7, 8};
            case 7 -> new int[]{0, 1, 2, 4, 6, 7, 8};
            case 8 -> new int[]{0, 1, 2, 3, 5, 6, 7, 8};
            default -> new int[0];
        };
    }

    private List<QuestDefinition> filtered(Player player, String cycle, View view, Sort sort, String search) {
        CycleState state = manager.playerDataService().cycle(cycle);
        if (state == null) return List.of();
        PlayerQuestData data = manager.playerData(player.getUniqueId());
        String term = search == null ? "" : search.toLowerCase(Locale.ROOT);
        Comparator<QuestDefinition> comparator = switch (sort) {
            case DIFFICULTY -> Comparator.comparing(QuestDefinition::difficulty).thenComparing(QuestDefinition::id);
            case PROGRESS -> Comparator.<QuestDefinition>comparingDouble(q -> ratio(data.progressOf(state.instanceId(), q.id()), q.required())).reversed();
            case NAME -> Comparator.comparing(q -> messages.questName(q).toLowerCase(Locale.ROOT));
        };
        return manager.quests(player, cycle).stream().filter(quest -> {
            boolean active = data.isActive(state.instanceId(), quest.id());
            boolean completed = data.isCompleted(state.instanceId(), quest.id());
            boolean ready = active && data.progressOf(state.instanceId(), quest.id()) >= quest.required();
            boolean status = switch (view) {
                case ALL -> true;
                case AVAILABLE -> !active && !completed;
                case ACTIVE -> active && !ready;
                case READY -> ready;
                case COMPLETED -> completed;
            };
            boolean text = term.isBlank() || quest.id().contains(term)
                    || messages.questName(quest).toLowerCase(Locale.ROOT).contains(term)
                    || quest.type().name().toLowerCase(Locale.ROOT).contains(term);
            return status && text;
        }).sorted(comparator).toList();
    }

    private double ratio(int current, int required) { return required <= 0 ? 0D : current / (double) required; }

    private ItemStack questItem(Player player, QuestDefinition quest) {
        CycleState cycle = manager.playerDataService().cycle(quest.cycle());
        PlayerQuestData data = manager.playerData(player.getUniqueId());
        int progress = cycle == null ? 0 : data.progressOf(cycle.instanceId(), quest.id());
        boolean active = cycle != null && data.isActive(cycle.instanceId(), quest.id());
        boolean completed = cycle != null && data.isCompleted(cycle.instanceId(), quest.id());
        boolean ready = active && progress >= quest.required();
        Map<String, String> map = new HashMap<>(manager.placeholders(player, quest, progress));
        String statusKey = completed ? "completed" : ready ? "ready" : active ? "active" : "available";
        map.put("%status%", messages.rawOr("gui.status." + statusKey, fallbackStatus(statusKey)));
        map.put("%progress_bar%", manager.progressBar(progress, quest.required(), 20));
        List<String> lore = new ArrayList<>();
        for (String line : quest.lore()) lore.add(apply(player, line, map));
        lore.addAll(parse(player, "gui.quest.details", map));
        lore.addAll(parse(player, "gui.quest.actions." + (completed ? "completed" : ready ? "ready" : active ? "active" : "available"), map));
        return new ItemBuilder(quest.material()).name(apply(player, messages.questName(quest), map)).lore(lore).build();
    }

    private ItemStack item(Player player, String path, Material fallback, Map<String, String> map) {
        return new ItemBuilder(config.guiMaterial(path, fallback))
                .name(apply(player, messages.raw(path + ".name"), map))
                .lore(parse(player, path + ".lore", map)).build();
    }

    private List<String> parse(Player player, String path, Map<String, String> map) {
        List<String> lines = new ArrayList<>();
        for (String line : messages.rawList(path)) lines.add(apply(player, line, map));
        return lines;
    }

    private String apply(Player player, String line, Map<String, String> map) {
        return plugin.applyExternalPlaceholders(player, Text.placeholders(line == null ? "" : line, map));
    }

    private Map<String, String> placeholders(Player player, ViewState state, int maxPage) {
        CycleSettings settings = config.cycle(state.cycle());
        Map<String, String> map = new LinkedHashMap<>();
        map.put("%cycle%", state.cycle());
        map.put("%cycle_name%", settings == null ? state.cycle() : settings.displayName());
        map.put("%page%", String.valueOf(state.page() + 1));
        map.put("%max_page%", String.valueOf(maxPage + 1));
        String viewKey = state.view().name().toLowerCase(Locale.ROOT);
        String sortKey = state.sort().name().toLowerCase(Locale.ROOT);
        map.put("%filter%", messages.rawOr("gui.filters." + viewKey, fallbackFilter(state.view())));
        map.put("%sort%", messages.rawOr("gui.sorts." + sortKey, fallbackSort(state.sort())));
        map.put("%search%", state.search().isBlank() ? messages.rawOr("gui.search-none", "Không có") : state.search());
        String cycleResetTime = manager.resetTimeText(state.cycle());
        map.put("%reset_time%", cycleResetTime);
        map.put("%cycle_reset_time%", cycleResetTime);
        map.put("%total%", String.valueOf(manager.quests(player, state.cycle()).size()));
        map.put("%completed%", String.valueOf(manager.completed(state.cycle(), player.getUniqueId())));
        map.put("%remaining%", String.valueOf(manager.remaining(state.cycle(), player.getUniqueId())));
        map.put("%active_count%", String.valueOf(manager.playerData(player.getUniqueId()).activeCount(state.cycle())));
        map.put("%streak%", String.valueOf(manager.playerData(player.getUniqueId()).profile().dailyStreak()));
        map.put("%season_level%", String.valueOf(manager.playerData(player.getUniqueId()).profile().seasonLevel()));
        return map;
    }


    private String fallbackStatus(String key) {
        return switch (key) {
            case "completed" -> "&#8A8A8AĐã hoàn thành";
            case "ready" -> "&#FFD36ASẵn sàng nhận";
            case "active" -> "&#7DE2FFĐang làm";
            default -> "&#75FF75Khả dụng";
        };
    }

    private String fallbackFilter(View view) {
        return switch (view) {
            case ALL -> "Tất cả";
            case AVAILABLE -> "Khả dụng";
            case ACTIVE -> "Đang làm";
            case READY -> "Sẵn sàng";
            case COMPLETED -> "Hoàn thành";
        };
    }

    private String fallbackSort(Sort sort) {
        return switch (sort) {
            case DIFFICULTY -> "Độ khó";
            case PROGRESS -> "Tiến độ";
            case NAME -> "Tên";
        };
    }

    private void setAction(Inventory inventory, QuestMenuHolder holder, int slot, Action action, ItemStack item) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        inventory.setItem(slot, item);
        holder.actions.put(slot, action);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof QuestMenuHolder holder)) return;
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        if (!holder.owner.equals(player.getUniqueId()) || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize() || !allowClick(player)) return;
        int slot = event.getRawSlot();
        if (holder.confirmQuest != null) {
            if (slot == 11) manager.cancel(player, holder.confirmQuest);
            openJava(player, holder.state);
            return;
        }
        String cycle = holder.cycleSlots.get(slot);
        if (cycle != null) {
            openJava(player, new ViewState(cycle, 0, holder.state.view(), holder.state.sort(), holder.state.search()));
            return;
        }
        Action action = holder.actions.get(slot);
        if (action != null) {
            switch (action) {
                case PREVIOUS -> openJava(player, holder.state.withPage(holder.state.page() - 1));
                case NEXT -> openJava(player, holder.state.withPage(holder.state.page() + 1));
                case FILTER -> openJava(player, holder.state.withView(holder.state.view().next()).withPage(0));
                case SORT -> openJava(player, holder.state.withSort(holder.state.sort().next()).withPage(0));
                case SEARCH -> {
                    player.closeInventory();
                    messages.send(player, "search-instruction", Map.of("%cycle%", holder.state.cycle()));
                }
                case CLAIM_ALL -> { manager.claimAll(player, holder.state.cycle()); refresh(player, holder.state); }
                case INFO -> { }
            }
            return;
        }
        String questId = holder.questSlots.get(slot);
        if (questId == null) return;
        QuestDefinition quest = config.quest(questId);
        if (quest == null) return;
        ClickType click = event.getClick();
        CycleState state = manager.playerDataService().cycle(quest.cycle());
        PlayerQuestData data = manager.playerData(player.getUniqueId());
        boolean active = state != null && data.isActive(state.instanceId(), quest.id());
        if (click == ClickType.MIDDLE || click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            if (player.hasPermission("koraquest.command.reroll")) manager.reroll(player, quest.id()); else messages.send(player, "no-permission");
        } else if (click.isShiftClick() && quest.type() == ObjectiveType.ITEM_SUBMIT) {
            manager.submitItems(player, quest.id(), Integer.MAX_VALUE);
        } else if (click == ClickType.RIGHT && active) {
            openConfirm(player, holder.state, quest);
            return;
        } else if (click == ClickType.LEFT) {
            if (active) {
                if (player.hasPermission("koraquest.command.claim")) manager.complete(player, quest.id()); else messages.send(player, "no-permission");
            } else {
                if (player.hasPermission("koraquest.command.accept")) manager.accept(player, quest.id()); else messages.send(player, "no-permission");
            }
        }
        refresh(player, holder.state);
    }

    private void openConfirm(Player player, ViewState state, QuestDefinition quest) {
        QuestMenuHolder holder = new QuestMenuHolder(player.getUniqueId(), state, 0);
        holder.confirmQuest = quest.id();
        Inventory inventory = Bukkit.createInventory(holder, 27, Text.color(messages.raw("gui.confirm.title")));
        holder.inventory = inventory;
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int slot = 0; slot < 27; slot++) inventory.setItem(slot, filler);
        inventory.setItem(11, item(player, "gui.confirm.yes", Material.LIME_CONCRETE, Map.of("%quest_name%", messages.questName(quest))));
        inventory.setItem(15, item(player, "gui.confirm.no", Material.RED_CONCRETE, Map.of("%quest_name%", messages.questName(quest))));
        player.openInventory(inventory);
    }

    private void refresh(Player player, ViewState state) {
        plugin.scheduler().runEntityLater(player, () -> { if (player.isOnline()) openJava(player, state); }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof QuestMenuHolder) {
            event.setCancelled(true); event.setResult(Event.Result.DENY);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        lastClicks.remove(event.getPlayer().getUniqueId());
        states.remove(event.getPlayer().getUniqueId());
    }

    private boolean allowClick(Player player) {
        long now = System.nanoTime();
        Long prior = lastClicks.put(player.getUniqueId(), now);
        return prior == null || now - prior >= CLICK_DEBOUNCE_NANOS;
    }

    public boolean isQuestMenu(Inventory inventory) { return inventory != null && inventory.getHolder() instanceof QuestMenuHolder; }

    private enum Action { PREVIOUS, NEXT, FILTER, SORT, SEARCH, CLAIM_ALL, INFO }
    private enum View { ALL, AVAILABLE, ACTIVE, READY, COMPLETED; View next() { return values()[(ordinal() + 1) % values().length]; } }
    private enum Sort { DIFFICULTY, PROGRESS, NAME; Sort next() { return values()[(ordinal() + 1) % values().length]; } }

    private record ViewState(String cycle, int page, View view, Sort sort, String search) {
        static ViewState initial(String cycle) { return new ViewState(cycle, 0, View.ALL, Sort.DIFFICULTY, ""); }
        ViewState withPage(int value) { return new ViewState(cycle, value, view, sort, search); }
        ViewState withView(View value) { return new ViewState(cycle, page, value, sort, search); }
        ViewState withSort(Sort value) { return new ViewState(cycle, page, view, value, search); }
    }

    private static final class QuestMenuHolder implements InventoryHolder {
        private final UUID owner;
        private final ViewState state;
        private final int maxPage;
        private final Map<Integer, String> questSlots = new HashMap<>();
        private final Map<Integer, String> cycleSlots = new HashMap<>();
        private final Map<Integer, Action> actions = new HashMap<>();
        private String confirmQuest;
        private Inventory inventory;
        private QuestMenuHolder(UUID owner, ViewState state, int maxPage) { this.owner = owner; this.state = state; this.maxPage = maxPage; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
