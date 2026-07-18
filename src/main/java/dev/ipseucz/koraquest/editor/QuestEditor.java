package dev.ipseucz.koraquest.editor;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.model.Difficulty;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-game quest editor. YAML remains the source of truth; every edit is validated and saved immediately. */
@SuppressWarnings("deprecation")
public final class QuestEditor implements Listener {
    private final KoraQuestPlugin plugin;
    private final QuestManager manager;
    private final QuestConfig config;
    private final MessageService messages;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public QuestEditor(KoraQuestPlugin plugin, QuestManager manager, QuestConfig config, MessageService messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
        this.messages = messages;
    }

    public void open(Player player) { openList(player, 0); }

    public void open(Player player, String questId) {
        if (!allowed(player)) return;
        QuestDefinition quest = config.quest(questId);
        if (quest == null) { messages.send(player, "quest-not-found", Map.of("%quest_id%", questId)); return; }
        openEdit(player, quest);
    }

    private boolean allowed(Player player) {
        if (player != null && player.hasPermission("koraquest.admin.editor")) return true;
        if (player != null) messages.send(player, "no-permission");
        return false;
    }

    private void openList(Player player, int requestedPage) {
        if (!allowed(player)) return;
        List<QuestDefinition> quests = config.quests().values().stream().sorted(java.util.Comparator.comparing(QuestDefinition::id)).toList();
        int pageSize = 45;
        int maxPage = Math.max(0, (quests.size() - 1) / pageSize);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        EditorHolder holder = new EditorHolder(player.getUniqueId(), Mode.LIST, null, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, Text.color(messages.raw("editor.list-title")));
        holder.inventory = inventory;
        int from = page * pageSize;
        int to = Math.min(quests.size(), from + pageSize);
        for (int index = from; index < to; index++) {
            QuestDefinition quest = quests.get(index);
            int slot = index - from;
            inventory.setItem(slot, questIcon(quest));
            holder.questSlots.put(slot, quest.id());
        }
        inventory.setItem(45, button(Material.ARROW, "editor.previous", Map.of("%page%", String.valueOf(page))));
        inventory.setItem(49, button(Material.EMERALD, "editor.create", Map.of()));
        inventory.setItem(50, button(Material.ANVIL, "editor.validate-all", Map.of("%errors%", String.valueOf(config.validationErrors().size()))));
        inventory.setItem(53, button(Material.ARROW, "editor.next", Map.of("%page%", String.valueOf(page + 2))));
        player.openInventory(inventory);
    }

    private void openEdit(Player player, QuestDefinition quest) {
        EditorHolder holder = new EditorHolder(player.getUniqueId(), Mode.EDIT, quest.id(), 0);
        Inventory inventory = Bukkit.createInventory(holder, 45, Text.color(messages.raw("editor.edit-title").replace("%quest_id%", quest.id())));
        holder.inventory = inventory;
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        inventory.setItem(4, questIcon(quest));
        set(inventory, holder, 10, Field.CYCLE, Material.CLOCK, quest.cycle());
        set(inventory, holder, 11, Field.MATERIAL, quest.material(), quest.material().name());
        set(inventory, holder, 12, Field.TITLE, Material.NAME_TAG, quest.title());
        set(inventory, holder, 13, Field.LORE, Material.WRITABLE_BOOK, String.join(" | ", quest.lore()));
        set(inventory, holder, 14, Field.TYPE, Material.TARGET, quest.type().name());
        set(inventory, holder, 15, Field.TARGET, Material.COMPASS, quest.target());
        set(inventory, holder, 16, Field.REQUIRED, Material.REPEATER, String.valueOf(quest.required()));
        set(inventory, holder, 19, Field.FILTERS, Material.HOPPER, describeFilters(quest));
        set(inventory, holder, 20, Field.REQUIREMENTS, Material.TRIPWIRE_HOOK, String.join(" | ", quest.requirements()));
        set(inventory, holder, 21, Field.REWARDS, Material.CHEST, String.join(" | ", quest.rewards()));
        set(inventory, holder, 22, Field.PERMISSION, Material.PAPER, quest.permission().isBlank() ? "-" : quest.permission());
        set(inventory, holder, 23, Field.COOLDOWN, Material.RECOVERY_COMPASS, quest.cooldownSeconds() + "s");
        set(inventory, holder, 24, Field.EFFECTS, Material.FIREWORK_STAR, quest.completionSound() + " / " + quest.completionParticle());
        set(inventory, holder, 25, Field.SEASON_XP, Material.EXPERIENCE_BOTTLE, String.valueOf(quest.seasonXp()));
        set(inventory, holder, 28, Field.DIFFICULTY, Material.NETHER_STAR, quest.difficulty().name());
        set(inventory, holder, 29, Field.REROLLABLE, Material.ENDER_EYE, String.valueOf(quest.rerollable()));
        set(inventory, holder, 30, Field.CLONE, Material.SLIME_BALL, quest.id());
        set(inventory, holder, 31, Field.PREVIEW, Material.SPYGLASS, quest.id());
        set(inventory, holder, 32, Field.VALIDATE, Material.KNOWLEDGE_BOOK, quest.id());
        set(inventory, holder, 33, Field.TEST_REWARD, Material.DISPENSER, quest.id());
        set(inventory, holder, 34, Field.DELETE, Material.BARRIER, quest.id());
        set(inventory, holder, 40, Field.BACK, Material.ARROW, "");
        player.openInventory(inventory);
    }

    private String describeFilters(QuestDefinition quest) {
        var f = quest.filters();
        return "world=" + f.worlds() + ", y=" + f.minY() + ".." + f.maxY() + ", weapon=" + f.weapon()
                + ", regions=" + f.regions() + ", island=" + f.requireOwnIsland() + ", plot=" + f.requireOwnPlot();
    }

    private ItemStack questIcon(QuestDefinition quest) {
        return new ItemBuilder(quest.material()).name(quest.title()).lore(List.of(
                "&#8A8A8AID: &f" + quest.id(), "&#8A8A8AChu kỳ: &f" + quest.cycle(),
                "&#8A8A8AObjective: &f" + quest.type() + " &8/ &f" + quest.target(),
                "&#8A8A8AYêu cầu: &f" + quest.required(), "", "&#75FF75Nhấn để chỉnh sửa"
        )).build();
    }

    private void set(Inventory inventory, EditorHolder holder, int slot, Field field, Material material, String value) {
        Map<String, String> map = Map.of("%field%", messages.raw("editor.fields." + field.name().toLowerCase(Locale.ROOT)), "%value%", value == null || value.isBlank() ? "-" : value);
        inventory.setItem(slot, new ItemBuilder(material).name(messages.raw("editor.field-name").replace("%field%", map.get("%field%")))
                .lore(messages.formatList("editor.field-lore", map)).build());
        holder.fields.put(slot, field);
    }

    private ItemStack button(Material material, String path, Map<String, String> map) {
        return new ItemBuilder(material).name(messages.format(path + ".name", map)).lore(messages.formatList(path + ".lore", map)).build();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof EditorHolder holder)) return;
        event.setCancelled(true); event.setResult(Event.Result.DENY);
        if (!holder.owner.equals(player.getUniqueId()) || event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        int slot = event.getRawSlot();
        if (holder.mode == Mode.LIST) {
            String id = holder.questSlots.get(slot);
            if (id != null) { open(player, id); return; }
            if (slot == 45) openList(player, holder.page - 1);
            else if (slot == 53) openList(player, holder.page + 1);
            else if (slot == 49) prompt(player, "", Field.CREATE);
            else if (slot == 50) showValidation(player);
            return;
        }
        Field field = holder.fields.get(slot);
        if (field == null) return;
        QuestDefinition quest = config.quest(holder.questId);
        if (quest == null) { openList(player, 0); return; }
        switch (field) {
            case BACK -> openList(player, 0);
            case CYCLE -> cycleField(player, quest);
            case DIFFICULTY -> enumField(player, quest, field);
            case REROLLABLE -> update(player, quest.id(), "rerollable", !quest.rerollable());
            case PREVIEW -> preview(player, quest);
            case VALIDATE -> validateQuest(player, quest);
            case TEST_REWARD -> manager.testRewards(player, quest.id());
            case DELETE -> prompt(player, quest.id(), Field.DELETE);
            default -> prompt(player, quest.id(), field);
        }
    }

    private void cycleField(Player player, QuestDefinition quest) {
        List<String> cycles = new ArrayList<>(config.cycles().keySet());
        int current = cycles.indexOf(quest.cycle());
        update(player, quest.id(), "cycle", cycles.get((current + 1) % cycles.size()));
    }

    private void enumField(Player player, QuestDefinition quest, Field field) {
        if (field == Field.DIFFICULTY) {
            Difficulty[] values = Difficulty.values();
            update(player, quest.id(), "difficulty", values[(quest.difficulty().ordinal() + 1) % values.length].key());
        }
    }

    private void prompt(Player player, String questId, Field field) {
        prompts.put(player.getUniqueId(), new Prompt(questId, field));
        player.closeInventory();
        messages.send(player, "editor-prompt", Map.of("%field%", messages.raw("editor.fields." + field.name().toLowerCase(Locale.ROOT))));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Prompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) return;
        event.setCancelled(true);
        String input = event.getMessage().trim();
        plugin.scheduler().runEntity(event.getPlayer(), () -> applyPrompt(event.getPlayer(), prompt, input));
    }

    private void applyPrompt(Player player, Prompt prompt, String input) {
        if (input.equalsIgnoreCase("cancel")) { messages.send(player, "editor-cancelled"); reopen(player, prompt.questId()); return; }
        try {
            switch (prompt.field()) {
                case CREATE -> {
                    QuestConfig.CreateResult result = config.createQuest(input, "daily", Difficulty.EASY, ObjectiveType.BREAK, "STONE", 64, Material.BOOK);
                    messages.send(player, result == QuestConfig.CreateResult.CREATED ? "admin-created" : "editor-update-failed", Map.of("%quest_id%", input));
                    if (result == QuestConfig.CreateResult.CREATED) open(player, input); else openList(player, 0);
                    return;
                }
                case CLONE -> {
                    boolean success = config.cloneQuest(prompt.questId(), input);
                    messages.send(player, success ? "editor-updated" : "editor-update-failed", Map.of("%quest_id%", input));
                    if (success) open(player, input); else reopen(player, prompt.questId());
                    return;
                }
                case DELETE -> {
                    if (input.equalsIgnoreCase("DELETE " + prompt.questId()) || input.equalsIgnoreCase("XOA " + prompt.questId())) {
                        config.deleteQuest(prompt.questId()); manager.removeQuest(prompt.questId()); messages.send(player, "admin-deleted", Map.of("%quest_id%", prompt.questId())); openList(player, 0);
                    } else { messages.send(player, "editor-delete-cancelled"); reopen(player, prompt.questId()); }
                    return;
                }
                case MATERIAL -> update(player, prompt.questId(), "material", requireMaterial(input).name());
                case TITLE -> update(player, prompt.questId(), "title", input);
                case LORE -> update(player, prompt.questId(), "lore", splitList(input));
                case TYPE -> {
                    ObjectiveType type = ObjectiveType.from(input);
                    config.updateQuestField(prompt.questId(), "target", "ANY");
                    update(player, prompt.questId(), "type", type.name());
                }
                case TARGET -> update(player, prompt.questId(), "target", input);
                case REQUIRED -> update(player, prompt.questId(), "required", positiveInt(input));
                case REQUIREMENTS -> update(player, prompt.questId(), "requirements", splitList(input));
                case REWARDS -> update(player, prompt.questId(), "rewards", splitList(input));
                case PERMISSION -> update(player, prompt.questId(), "permission", input.equals("-") ? "" : input);
                case COOLDOWN -> update(player, prompt.questId(), "cooldown-seconds", nonNegativeLong(input));
                case SEASON_XP -> update(player, prompt.questId(), "season-xp", positiveOrZero(input));
                case EFFECTS -> {
                    String[] parts = input.split("\\|", -1);
                    config.updateQuestField(prompt.questId(), "completion.sound", parts.length > 0 ? parts[0].trim() : "");
                    update(player, prompt.questId(), "completion.particle", parts.length > 1 ? parts[1].trim() : "");
                }
                case FILTERS -> applyFilters(player, prompt.questId(), input);
                default -> reopen(player, prompt.questId());
            }
        } catch (Exception exception) {
            messages.send(player, "editor-invalid-value", Map.of("%value%", input));
            reopen(player, prompt.questId());
        }
    }

    private void applyFilters(Player player, String questId, String input) {
        // key=value; key=value. Supported: worlds,min-y,max-y,weapon,spawn-reasons,critical,regions,island,plot,location,radius
        for (String token : input.split(";")) {
            int equals = token.indexOf('=');
            if (equals < 1) continue;
            String key = token.substring(0, equals).trim().toLowerCase(Locale.ROOT);
            String value = token.substring(equals + 1).trim();
            String field = switch (key) {
                case "worlds" -> "filters.worlds"; case "min-y" -> "filters.min-y"; case "max-y" -> "filters.max-y";
                case "weapon" -> "filters.weapon"; case "spawn-reasons" -> "filters.spawn-reasons";
                case "critical" -> "filters.require-critical-hit"; case "regions" -> "filters.regions";
                case "island" -> "filters.require-own-island"; case "plot" -> "filters.require-own-plot";
                case "location" -> "filters.location"; case "radius" -> "filters.radius"; default -> null;
            };
            if (field == null) continue;
            Object parsed = switch (key) {
                case "worlds", "spawn-reasons", "regions" -> splitComma(value);
                case "min-y", "max-y" -> Integer.parseInt(value);
                case "radius" -> Double.parseDouble(value);
                case "critical", "island", "plot" -> Boolean.parseBoolean(value);
                default -> value;
            };
            config.updateQuestField(questId, field, parsed);
        }
        messages.send(player, "editor-updated", Map.of("%quest_id%", questId));
        reopen(player, questId);
    }

    private void update(Player player, String questId, String field, Object value) {
        boolean success = config.updateQuestField(questId, field, value);
        messages.send(player, success ? "editor-updated" : "editor-update-failed", Map.of("%quest_id%", questId));
        reopen(player, questId);
    }

    private void reopen(Player player, String questId) {
        plugin.scheduler().runEntityLater(player, () -> { if (player.isOnline()) { if (questId == null || questId.isBlank()) openList(player, 0); else open(player, questId); } }, 1L);
    }

    private void preview(Player player, QuestDefinition quest) {
        player.closeInventory();
        player.sendMessage(Text.color("&#FFB84D&lKoraQuest Editor &8— &f" + quest.title()));
        for (String line : quest.lore()) player.sendMessage(Text.color(line));
        player.sendMessage(Text.color("&#8A8A8A" + quest.type() + ": &f" + quest.target() + " &8× &f" + quest.required()));
        reopen(player, quest.id());
    }

    private void validateQuest(Player player, QuestDefinition quest) {
        List<QuestConfig.ValidationIssue> matching = config.validationErrors().stream()
                .filter(issue -> quest.id().equalsIgnoreCase(issue.placeholders().get("%quest_id%"))).toList();
        messages.send(player, matching.isEmpty() ? "editor-valid" : "editor-invalid", Map.of("%quest_id%", quest.id(), "%errors%", String.valueOf(matching.size())));
    }

    private void showValidation(Player player) {
        messages.send(player, config.validationErrors().isEmpty() ? "validation-ok" : "validation-header", Map.of("%count%", String.valueOf(config.validationErrors().size())));
        for (QuestConfig.ValidationIssue issue : config.validationErrors()) player.sendMessage(messages.format("validation-errors." + issue.key(), issue.placeholders()));
    }

    private Material requireMaterial(String input) {
        Material material = Material.matchMaterial(input);
        if (material == null || material.isAir()) throw new IllegalArgumentException();
        return material;
    }
    private int positiveInt(String value) { int number = Integer.parseInt(value); if (number <= 0) throw new IllegalArgumentException(); return number; }
    private int positiveOrZero(String value) { int number = Integer.parseInt(value); if (number < 0) throw new IllegalArgumentException(); return number; }
    private long nonNegativeLong(String value) { long number = Long.parseLong(value); if (number < 0) throw new IllegalArgumentException(); return number; }
    private List<String> splitList(String input) { return input.equals("-") ? List.of() : java.util.Arrays.stream(input.split("\\|", -1)).map(String::trim).toList(); }
    private List<String> splitComma(String input) { return input.equals("-") ? List.of() : java.util.Arrays.stream(input.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList(); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EditorHolder) { event.setCancelled(true); event.setResult(Event.Result.DENY); }
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { prompts.remove(event.getPlayer().getUniqueId()); }

    private enum Mode { LIST, EDIT }
    private enum Field { CREATE, CYCLE, MATERIAL, TITLE, LORE, TYPE, TARGET, REQUIRED, FILTERS, REQUIREMENTS, REWARDS, PERMISSION, COOLDOWN, EFFECTS, SEASON_XP, DIFFICULTY, REROLLABLE, CLONE, PREVIEW, VALIDATE, TEST_REWARD, DELETE, BACK }
    private record Prompt(String questId, Field field) { }
    private static final class EditorHolder implements InventoryHolder {
        private final UUID owner; private final Mode mode; private final String questId; private final int page;
        private final Map<Integer, String> questSlots = new HashMap<>(); private final Map<Integer, Field> fields = new HashMap<>(); private Inventory inventory;
        private EditorHolder(UUID owner, Mode mode, String questId, int page) { this.owner = owner; this.mode = mode; this.questId = questId; this.page = page; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
