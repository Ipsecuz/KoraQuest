package dev.ipseucz.koraquest.gui;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.data.PlayerQuestData;
import dev.ipseucz.koraquest.model.Difficulty;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestGui implements Listener {
    private static final long CLICK_DEBOUNCE_NANOS = 150_000_000L;
    private final KoraQuestPlugin plugin;
    private final QuestManager manager;
    private final QuestConfig config;
    private final MessageService messages;
    private final Map<UUID, Long> lastClicks = new ConcurrentHashMap<>();

    public QuestGui(KoraQuestPlugin plugin, QuestManager manager, QuestConfig config, MessageService messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
        this.messages = messages;
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        QuestMenuHolder holder = new QuestMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, config.guiSize(), Text.color(messages.raw("gui.title")));
        holder.bind(inventory);

        ItemStack filler = new ItemBuilder(config.fillerMaterial()).name(messages.raw("gui.filler-name")).build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        Map<String, String> global = guiPlaceholders();
        setIfValid(inventory, config.guiSlot("gui.info", 4), configItem("gui.info", Material.CLOCK, global));
        setIfValid(inventory, config.guiSlot("gui.active", 49), configItem("gui.active", Material.BOOK, global));
        for (Difficulty difficulty : Difficulty.values()) {
            String path = "gui.difficulties." + difficulty.key();
            int fallbackSlot = switch (difficulty) {
                case EASY -> 10;
                case MEDIUM -> 13;
                case HARD -> 16;
            };
            Material fallbackMaterial = switch (difficulty) {
                case EASY -> Material.LIME_DYE;
                case MEDIUM -> Material.YELLOW_DYE;
                case HARD -> Material.RED_DYE;
            };
            setIfValid(inventory, config.guiSlot(path, fallbackSlot), difficultyItem(difficulty, config.guiMaterial(path, fallbackMaterial)));
        }

        int index = 0;
        for (QuestDefinition quest : manager.dailyQuests()) {
            if (index >= config.questSlots().size()) {
                break;
            }
            int slot = config.questSlots().get(index++);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            inventory.setItem(slot, questItem(player, quest));
            holder.questSlot(slot, quest.id());
        }
        player.openInventory(inventory);
    }

    public boolean isQuestMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof QuestMenuHolder;
    }

    private ItemStack difficultyItem(Difficulty difficulty, Material material) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%daily_count%", String.valueOf(manager.dailyQuests(difficulty).size()));
        placeholders.put("%daily_weight%", String.valueOf(config.dailyChancePercent(difficulty)));
        return new ItemBuilder(material)
                .name(messages.raw("gui.difficulty." + difficulty.key() + ".name"))
                .lore(parse("gui.difficulty." + difficulty.key() + ".lore", placeholders))
                .build();
    }

    private ItemStack questItem(Player player, QuestDefinition quest) {
        PlayerQuestData playerData = manager.playerData(player.getUniqueId());
        int progress = playerData.progressOf(quest.id());
        boolean active = playerData.isActive(quest.id());
        boolean completed = playerData.isCompleted(quest.id());
        boolean ready = active && progress >= quest.required();
        Map<String, String> placeholders = new HashMap<>(manager.placeholders(player, quest, progress));
        List<String> lore = new ArrayList<>();
        for (String line : messages.questLore(quest)) {
            lore.add(Text.placeholders(line, placeholders));
        }
        lore.addAll(parse("gui.quest.details", placeholders));
        String state = completed ? "completed" : ready ? "ready" : active ? "active" : "available";
        lore.addAll(parse("gui.quest.state." + state, placeholders));
        return new ItemBuilder(quest.material()).name(messages.questName(quest)).lore(lore).build();
    }

    private ItemStack configItem(String path, Material fallback, Map<String, String> placeholders) {
        return new ItemBuilder(config.guiMaterial(path, fallback))
                .name(Text.placeholders(messages.raw(path + ".name"), placeholders))
                .lore(parse(path + ".lore", placeholders))
                .build();
    }

    private List<String> parse(String path, Map<String, String> placeholders) {
        List<String> result = new ArrayList<>();
        for (String line : messages.rawList(path)) {
            result.add(Text.placeholders(line, placeholders));
        }
        return result;
    }

    private Map<String, String> guiPlaceholders() {
        Map<String, String> map = new HashMap<>();
        map.put("%reset_time%", Text.formatDuration(manager.millisUntilReset()));
        map.put("%daily_total%", String.valueOf(manager.dailyQuests().size()));
        map.put("%easy_count%", String.valueOf(manager.dailyQuests(Difficulty.EASY).size()));
        map.put("%medium_count%", String.valueOf(manager.dailyQuests(Difficulty.MEDIUM).size()));
        map.put("%hard_count%", String.valueOf(manager.dailyQuests(Difficulty.HARD).size()));
        map.put("%easy_chance%", String.valueOf(config.dailyChancePercent(Difficulty.EASY)));
        map.put("%medium_chance%", String.valueOf(config.dailyChancePercent(Difficulty.MEDIUM)));
        map.put("%hard_chance%", String.valueOf(config.dailyChancePercent(Difficulty.HARD)));
        return map;
    }

    private void setIfValid(Inventory inventory, int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        QuestMenuHolder holder = holder(event.getView().getTopInventory());
        if (holder == null) {
            return;
        }

        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }

        Inventory openedMenu = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        String questId = holder.questId(rawSlot);
        if (questId == null || !allowClick(player)) {
            return;
        }
        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT) {
            return;
        }

        PlayerQuestData data = manager.playerData(player.getUniqueId());
        QuestDefinition quest = config.quest(questId);
        if (quest == null) {
            messages.send(player, "quest-not-found", Map.of("%quest_id%", questId));
            refreshLater(player, openedMenu);
            return;
        }
        if (click == ClickType.RIGHT) {
            if (data.isActive(questId)) {
                manager.cancel(player, questId);
            }
            refreshLater(player, openedMenu);
            return;
        }
        if (data.isActive(questId)) {
            manager.complete(player, questId);
        } else {
            manager.accept(player, questId);
        }
        refreshLater(player, openedMenu);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        QuestMenuHolder holder = holder(event.getView().getTopInventory());
        if (holder == null) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastClicks.remove(event.getPlayer().getUniqueId());
    }

    private boolean allowClick(Player player) {
        long now = System.nanoTime();
        Long previous = lastClicks.put(player.getUniqueId(), now);
        return previous == null || now - previous >= CLICK_DEBOUNCE_NANOS;
    }

    private void refreshLater(Player player, Inventory expectedMenu) {
        plugin.scheduler().runEntityLater(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            Inventory current = player.getOpenInventory().getTopInventory();
            if (current == expectedMenu && isQuestMenu(current)) {
                open(player);
            }
        }, 1L);
    }

    private QuestMenuHolder holder(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof QuestMenuHolder menu ? menu : null;
    }

    private static final class QuestMenuHolder implements InventoryHolder {
        private final UUID owner;
        private final Map<Integer, String> questSlots = new HashMap<>();
        private Inventory inventory;

        private QuestMenuHolder(UUID owner) {
            this.owner = owner;
        }

        private UUID owner() {
            return owner;
        }

        private void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        private void questSlot(int slot, String questId) {
            questSlots.put(slot, questId);
        }

        private String questId(int slot) {
            return questSlots.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
