package dev.ipseucz.koraquest.command;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
<<<<<<< HEAD
import dev.ipseucz.koraquest.data.QuestProgressEntry;
import dev.ipseucz.koraquest.editor.QuestEditor;
=======
import dev.ipseucz.koraquest.data.PlayerQuestData;
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
import dev.ipseucz.koraquest.gui.QuestGui;
import dev.ipseucz.koraquest.model.Difficulty;
import dev.ipseucz.koraquest.model.ObjectiveType;
import dev.ipseucz.koraquest.model.QuestDefinition;
import dev.ipseucz.koraquest.update.UpdateChecker;
import dev.ipseucz.koraquest.util.PlatformDetector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
<<<<<<< HEAD
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuestCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ADMIN_PERMISSIONS = List.of(
            "koraquest.admin.reload",
            "koraquest.admin.reset",
            "koraquest.admin.editor",
            "koraquest.admin.progress",
            "koraquest.admin.rewardretry",
            "koraquest.admin.update"
    );
=======
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class QuestCommand implements CommandExecutor, TabCompleter {
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    private final KoraQuestPlugin plugin;
    private final QuestManager manager;
    private final QuestConfig config;
    private final MessageService messages;
    private final QuestGui gui;
<<<<<<< HEAD
    private final QuestEditor editor;
    private final UpdateChecker updateChecker;

    public QuestCommand(KoraQuestPlugin plugin, QuestManager manager, QuestConfig config, MessageService messages,
                        QuestGui gui, QuestEditor editor, UpdateChecker updateChecker) {
=======
    private final UpdateChecker updateChecker;

    public QuestCommand(KoraQuestPlugin plugin, QuestManager manager, QuestConfig config, MessageService messages,
                        QuestGui gui, UpdateChecker updateChecker) {
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
        this.messages = messages;
        this.gui = gui;
<<<<<<< HEAD
        this.editor = editor;
=======
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        this.updateChecker = updateChecker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
<<<<<<< HEAD
            Player player = player(sender);
            if (player != null && permission(player, "koraquest.command.open")) gui.open(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (config.cycle(sub) != null) {
            Player player = player(sender);
            if (player != null && permission(player, "koraquest.command.open")) gui.open(player, sub, "");
            return true;
        }
        switch (sub) {
            case "help" -> showHelp(sender);
            case "active" -> active(sender, args.length >= 2 ? args[1] : null);
            case "search" -> search(sender, args);
            case "accept" -> action(sender, args, Action.ACCEPT);
            case "cancel" -> action(sender, args, Action.CANCEL);
            case "claim", "complete" -> action(sender, args, Action.CLAIM);
            case "reroll" -> action(sender, args, Action.REROLL);
            case "submit" -> submit(sender, args);
            case "editor" -> openEditor(sender, args);
            case "version" -> messages.send(sender, "version-line", Map.of(
                    "%current%", plugin.getDescription().getVersion(), "%platform%", PlatformDetector.name(),
                    "%storage%", config.storageType()));
            case "update" -> {
                if (permission(sender, "koraquest.admin.update")) updateChecker.checkNow(sender);
            }
            case "admin" -> {
                if (hasAnyAdminPermission(sender)) admin(sender, args);
                else messages.send(sender, "no-permission");
            }
=======
            Player player = requirePlayer(sender);
            if (player != null && requireUsePermission(player)) {
                gui.open(player);
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> messages.sendList(sender, "help", Map.of());
            case "active" -> showActive(sender);
            case "accept" -> questAction(sender, args, Action.ACCEPT);
            case "cancel" -> questAction(sender, args, Action.CANCEL);
            case "complete", "claim" -> questAction(sender, args, Action.COMPLETE);
            case "version" -> messages.send(sender, "version-line", Map.of(
                    "%current%", plugin.getDescription().getVersion(),
                    "%platform%", PlatformDetector.name()
            ));
            case "update" -> {
                if (!sender.hasPermission("koraquest.admin")) {
                    messages.send(sender, "no-permission");
                } else {
                    updateChecker.checkNow(sender);
                }
            }
            case "admin" -> handleAdmin(sender, args);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            default -> messages.send(sender, "unknown-command");
        }
        return true;
    }

<<<<<<< HEAD
    private void active(CommandSender sender, String cycle) {
        Player player = player(sender);
        if (player == null || !permission(player, "koraquest.command.open")) return;
        withLoadedData(player, () -> {
            Collection<QuestProgressEntry> entries = manager.playerData(player.getUniqueId()).activeEntries();
            if (cycle != null) entries = entries.stream().filter(entry -> entry.cycleName().equalsIgnoreCase(cycle)).toList();
            if (entries.isEmpty()) { messages.send(player, "active-empty"); return; }
            messages.send(player, "active-header");
            for (QuestProgressEntry entry : entries) {
                QuestDefinition quest = config.quest(entry.questId());
                if (quest == null) continue;
                player.sendMessage(messages.format("messages.active-line", manager.placeholders(player, quest, entry.progress())));
            }
        });
    }

    private void search(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null || !permission(player, "koraquest.command.open")) return;
        if (args.length < 2) { messages.send(player, "search-usage"); return; }
        String cycle = config.cycle(args[1]) != null ? args[1] : "daily";
        int start = config.cycle(args[1]) != null ? 2 : 1;
        String query = String.join(" ", Arrays.copyOfRange(args, start, args.length));
        gui.open(player, cycle, query);
    }

    private void action(CommandSender sender, String[] args, Action action) {
        Player player = player(sender);
        String permission = switch (action) {
            case ACCEPT -> "koraquest.command.accept";
            case CANCEL -> "koraquest.command.cancel";
            case CLAIM -> "koraquest.command.claim";
            case REROLL -> "koraquest.command.reroll";
        };
        if (player == null || !permission(player, permission)) return;
        if (args.length < 2) { messages.send(player, "quest-action-usage", Map.of("%action%", action.name().toLowerCase(Locale.ROOT))); return; }
        withLoadedData(player, () -> {
            switch (action) {
                case ACCEPT -> manager.accept(player, args[1]);
                case CANCEL -> manager.cancel(player, args[1]);
                case CLAIM -> manager.complete(player, args[1]);
                case REROLL -> manager.reroll(player, args[1]);
            }
        });
    }

    private void submit(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null || !permission(player, "koraquest.command.submit")) return;
        if (args.length < 2) { messages.send(player, "submit-usage"); return; }
        int amount = Integer.MAX_VALUE;
        if (args.length >= 3) try { amount = Math.max(1, Integer.parseInt(args[2])); }
        catch (NumberFormatException ignored) { messages.send(player, "invalid-number"); return; }
        int finalAmount = amount;
        withLoadedData(player, () -> manager.submitItems(player, args[1], finalAmount));
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

    private void openEditor(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null || !permission(player, "koraquest.admin.editor")) return;
        if (args.length >= 2) editor.open(player, args[1]); else editor.open(player);
    }

    private void showHelp(CommandSender sender) {
        messages.sendList(sender, hasAnyAdminPermission(sender) ? "help-admin" : "help", Map.of());
    }

    private void admin(CommandSender sender, String[] args) {
        if (!hasAnyAdminPermission(sender)) {
            messages.send(sender, "no-permission");
            return;
        }
        if (args.length < 2) { messages.sendList(sender, "admin-help", Map.of()); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "help" -> messages.sendList(sender, "admin-help", Map.of());
            case "reload" -> { if (permission(sender, "koraquest.admin.reload")) plugin.scheduler().runGlobal(() -> plugin.reloadPlugin(sender)); }
            case "reset" -> {
                if (!permission(sender, "koraquest.admin.reset")) return;
                String cycle = args.length >= 3 ? args[2] : "daily";
                plugin.scheduler().runGlobal(() -> manager.forceRotate(cycle, sender));
            }
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "progress" -> progress(sender, args);
            case "retryrewards" -> { if (permission(sender, "koraquest.admin.rewardretry")) manager.retryPendingRewards(sender); }
            case "validate" -> validate(sender);
            case "types" -> types(sender);
            case "editor" -> {
                Player player = player(sender);
                if (player != null && permission(player, "koraquest.admin.editor")) {
                    if (args.length >= 3) editor.open(player, args[2]); else editor.open(player);
                }
            }
            case "update" -> { if (permission(sender, "koraquest.admin.update")) updateChecker.checkNow(sender); }
=======
    private boolean requireUsePermission(CommandSender sender) {
        if (sender.hasPermission("koraquest.use")) {
            return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        messages.send(sender, "player-only");
        return null;
    }

    private void showActive(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requireUsePermission(player)) {
            return;
        }
        PlayerQuestData playerData = manager.playerData(player.getUniqueId());
        List<String> ids = playerData.activeIds();
        if (ids.isEmpty()) {
            messages.send(player, "active-empty");
            return;
        }
        messages.send(player, "active-header");
        for (String id : ids) {
            QuestDefinition quest = config.quest(id);
            if (quest == null) {
                continue;
            }
            player.sendMessage(messages.format("messages.active-line", manager.placeholders(player, quest, playerData.progressOf(id))));
        }
    }

    private void questAction(CommandSender sender, String[] args, Action action) {
        Player player = requirePlayer(sender);
        if (player == null || !requireUsePermission(player)) {
            return;
        }
        if (args.length < 2) {
            messages.send(player, "quest-action-usage", Map.of("%action%", action.name().toLowerCase(Locale.ROOT)));
            return;
        }
        switch (action) {
            case ACCEPT -> manager.accept(player, args[1]);
            case CANCEL -> manager.cancel(player, args[1]);
            case COMPLETE -> manager.complete(player, args[1]);
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("koraquest.admin")) {
            messages.send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            messages.sendList(sender, "admin-help", Map.of());
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> messages.sendList(sender, "admin-help", Map.of());
            case "reload" -> plugin.scheduler().runGlobal(() -> plugin.reloadPlugin(sender));
            case "reset" -> plugin.scheduler().runGlobal(() -> manager.forceRotate(sender));
            case "create" -> createQuest(sender, args);
            case "delete" -> deleteQuest(sender, args);
            case "progress" -> progressQuest(sender, args);
            case "validate" -> validate(sender);
            case "types" -> showTypes(sender);
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
            default -> messages.sendList(sender, "admin-help", Map.of());
        }
    }

<<<<<<< HEAD
    private void create(CommandSender sender, String[] args) {
        if (!permission(sender, "koraquest.admin.editor")) return;
        // /quest admin create <id> <cycle> <difficulty> <type> <target> <required> [material]
        if (args.length < 8) { messages.send(sender, "admin-create-usage"); return; }
        try {
            String id = args[2].toLowerCase(Locale.ROOT);
            String cycle = args[3].toLowerCase(Locale.ROOT);
            Difficulty difficulty = Difficulty.from(args[4]);
            ObjectiveType type = ObjectiveType.from(args[5]);
            String target = args[6];
            int required = Integer.parseInt(args[7]);
            Material material = args.length >= 9 ? Material.matchMaterial(args[8]) : Material.BOOK;
            if (material == null || material.isAir()) { messages.send(sender, "invalid-material"); return; }
            QuestConfig.CreateResult result = config.createQuest(id, cycle, difficulty, type, target, required, material);
            messages.send(sender, result == QuestConfig.CreateResult.CREATED ? "admin-created" : "admin-create-failed",
                    Map.of("%quest_id%", id, "%result%", result.name()));
        } catch (IllegalArgumentException exception) { messages.send(sender, "invalid-type"); }
    }

    private void delete(CommandSender sender, String[] args) {
        if (!permission(sender, "koraquest.admin.editor")) return;
        if (args.length < 3) { messages.send(sender, "admin-delete-usage"); return; }
        String id = args[2].toLowerCase(Locale.ROOT);
        if (config.deleteQuest(id)) { manager.removeQuest(id); messages.send(sender, "admin-deleted", Map.of("%quest_id%", id)); }
        else messages.send(sender, "quest-not-found", Map.of("%quest_id%", id));
    }

    private void progress(CommandSender sender, String[] args) {
        if (!permission(sender, "koraquest.admin.progress")) return;
        if (args.length < 5) { messages.send(sender, "admin-progress-usage"); return; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { messages.send(sender, "player-not-found"); return; }
        int amount;
        try { amount = Integer.parseInt(args[4]); } catch (NumberFormatException ignored) { messages.send(sender, "invalid-number"); return; }
        plugin.scheduler().runEntity(target, () -> {
            boolean changed = manager.progressById(target, args[3], amount);
            plugin.sendSafe(sender, changed ? "admin-progressed" : "admin-progress-failed", Map.of(
                    "%player%", target.getName(), "%quest_id%", args[3], "%amount%", String.valueOf(amount)));
=======
    private void createQuest(CommandSender sender, String[] args) {
        if (args.length < 7) {
            messages.send(sender, "admin-create-usage");
            return;
        }
        Difficulty difficulty;
        ObjectiveType type;
        int required;
        Material material = Material.BOOK;
        try {
            difficulty = Difficulty.from(args[3]);
            type = ObjectiveType.from(args[4]);
            required = Integer.parseInt(args[6]);
            if (args.length >= 8) {
                Material parsed = Material.matchMaterial(args[7]);
                if (parsed == null || parsed == Material.AIR) {
                    messages.send(sender, "invalid-material");
                    return;
                }
                material = parsed;
            }
        } catch (IllegalArgumentException exception) {
            if (exception instanceof NumberFormatException) {
                messages.send(sender, "invalid-number");
            } else {
                messages.send(sender, "invalid-type");
            }
            return;
        }
        if (required <= 0 || !type.validTarget(type.normalizeTarget(args[5]))) {
            messages.send(sender, "invalid-target");
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        Material finalMaterial = material;
        plugin.scheduler().runGlobal(() -> {
            QuestConfig.CreateResult result = config.createQuest(id, difficulty, type, args[5], required, finalMaterial);
            switch (result) {
                case CREATED -> {
                    plugin.sendSafe(sender, "admin-created", Map.of("%quest_id%", id));
                }
                case EXISTS -> plugin.sendSafe(sender, "admin-create-exists", Map.of());
                case INVALID_ID -> plugin.sendSafe(sender, "admin-create-invalid-id", Map.of());
                case INVALID_TARGET -> plugin.sendSafe(sender, "invalid-target", Map.of());
            }
        });
    }

    private void deleteQuest(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "admin-delete-usage");
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        plugin.scheduler().runGlobal(() -> {
            if (!config.deleteQuest(id)) {
                plugin.sendSafe(sender, "quest-not-found", Map.of("%quest_id%", id));
                return;
            }
            manager.removeQuest(id);
            plugin.sendSafe(sender, "admin-deleted", Map.of("%quest_id%", id));
        });
    }

    private void progressQuest(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, "admin-progress-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messages.send(sender, "player-not-found");
            return;
        }
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException exception) {
                messages.send(sender, "invalid-number");
                return;
            }
        }
        if (amount <= 0) {
            messages.send(sender, "invalid-number");
            return;
        }
        String questId = args[3].toLowerCase(Locale.ROOT);
        int finalAmount = amount;
        plugin.scheduler().runEntity(target, () -> {
            boolean changed = manager.progressById(target, questId, finalAmount);
            plugin.sendSafe(sender, changed ? "admin-progressed" : "admin-progress-no-change", Map.of(
                    "%amount%", String.valueOf(finalAmount),
                    "%player%", target.getName(),
                    "%quest_id%", questId
            ));
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
        });
    }

    private void validate(CommandSender sender) {
<<<<<<< HEAD
        if (!permission(sender, "koraquest.admin.editor")) return;
        if (config.validationErrors().isEmpty()) { messages.send(sender, "validation-ok"); return; }
        messages.send(sender, "validation-header", Map.of("%count%", String.valueOf(config.validationErrors().size())));
        for (QuestConfig.ValidationIssue issue : config.validationErrors()) sender.sendMessage(messages.format("validation-errors." + issue.key(), issue.placeholders()));
    }

    private void types(CommandSender sender) {
        if (!permission(sender, "koraquest.admin.editor")) return;
        sender.sendMessage(messages.format("messages.types-header", Map.of()));
        for (ObjectiveType type : ObjectiveType.values()) sender.sendMessage(messages.format("messages.types-line", Map.of("%type%", type.name(), "%hint%", messages.raw("objective-target-hints." + type.name()))));
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player player) return player;
        messages.send(sender, "player-only"); return null;
    }

    private boolean permission(CommandSender sender, String node) {
        if (sender.hasPermission(node) || sender.hasPermission("koraquest.admin")) return true;
        messages.send(sender, "no-permission"); return false;
=======
        List<QuestConfig.ValidationIssue> issues = config.validationErrors();
        if (issues.isEmpty()) {
            messages.send(sender, "validation-ok");
            return;
        }
        messages.send(sender, "validation-header", Map.of("%amount%", String.valueOf(issues.size())));
        for (QuestConfig.ValidationIssue issue : issues) {
            Map<String, String> placeholders = new LinkedHashMap<>(issue.placeholders());
            placeholders.put("%error%", messages.raw("validation-errors." + issue.key()));
            sender.sendMessage(messages.format("messages.validation-line", placeholders));
        }
    }

    private void showTypes(CommandSender sender) {
        messages.send(sender, "types-header");
        for (ObjectiveType type : ObjectiveType.values()) {
            sender.sendMessage(messages.format("messages.types-line", Map.of(
                    "%type%", type.name(),
                    "%target_hint%", messages.raw("objective-target-hints." + type.name())
            )));
        }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
<<<<<<< HEAD
            List<String> values = new ArrayList<>();
            values.add("help");
            values.add("version");
            if (can(sender, "koraquest.command.open")) {
                values.add("active");
                values.add("search");
                values.addAll(config.cycles().keySet());
            }
            if (can(sender, "koraquest.command.accept")) values.add("accept");
            if (can(sender, "koraquest.command.cancel")) values.add("cancel");
            if (can(sender, "koraquest.command.claim")) values.add("claim");
            if (can(sender, "koraquest.command.reroll")) values.add("reroll");
            if (can(sender, "koraquest.command.submit")) values.add("submit");
            if (can(sender, "koraquest.admin.editor")) values.add("editor");
            if (can(sender, "koraquest.admin.update")) values.add("update");
            if (hasAnyAdminPermission(sender)) values.add("admin");
            return filter(values, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("accept") && can(sender, "koraquest.command.accept")) return filter(config.quests().keySet(), args[1]);
        if (args.length == 2 && sub.equals("cancel") && can(sender, "koraquest.command.cancel")) return filter(config.quests().keySet(), args[1]);
        if (args.length == 2 && sub.equals("claim") && can(sender, "koraquest.command.claim")) return filter(config.quests().keySet(), args[1]);
        if (args.length == 2 && sub.equals("reroll") && can(sender, "koraquest.command.reroll")) return filter(config.quests().keySet(), args[1]);
        if (args.length == 2 && sub.equals("submit") && can(sender, "koraquest.command.submit")) return filter(config.quests().keySet(), args[1]);
        if (args.length == 2 && sub.equals("editor") && can(sender, "koraquest.admin.editor")) return filter(config.quests().keySet(), args[1]);
        if (args.length == 2 && sub.equals("active") && can(sender, "koraquest.command.open")) return filter(config.cycles().keySet(), args[1]);
        if (args.length == 2 && sub.equals("search") && can(sender, "koraquest.command.open")) return filter(config.cycles().keySet(), args[1]);

        if (sub.equals("admin")) {
            if (!hasAnyAdminPermission(sender)) return List.of();
            if (args.length == 2) {
                List<String> values = new ArrayList<>();
                if (can(sender, "koraquest.admin.reload")) values.add("reload");
                if (can(sender, "koraquest.admin.reset")) values.add("reset");
                if (can(sender, "koraquest.admin.editor")) {
                    values.add("create");
                    values.add("delete");
                    values.add("validate");
                    values.add("types");
                    values.add("editor");
                }
                if (can(sender, "koraquest.admin.progress")) values.add("progress");
                if (can(sender, "koraquest.admin.rewardretry")) values.add("retryrewards");
                if (can(sender, "koraquest.admin.update")) values.add("update");
                return filter(values, args[1]);
            }
            String adminSub = args[1].toLowerCase(Locale.ROOT);
            if (args.length == 3 && adminSub.equals("reset") && can(sender, "koraquest.admin.reset")) return filter(config.cycles().keySet(), args[2]);
            if (args.length == 3 && List.of("delete", "editor").contains(adminSub) && can(sender, "koraquest.admin.editor")) return filter(config.quests().keySet(), args[2]);
            if (args.length == 3 && adminSub.equals("progress") && can(sender, "koraquest.admin.progress")) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
            if (args.length == 4 && adminSub.equals("progress") && can(sender, "koraquest.admin.progress")) return filter(config.quests().keySet(), args[3]);
            if (args.length == 4 && adminSub.equals("create") && can(sender, "koraquest.admin.editor")) return filter(config.cycles().keySet(), args[3]);
            if (args.length == 5 && adminSub.equals("create") && can(sender, "koraquest.admin.editor")) return filter(Arrays.stream(Difficulty.values()).map(d -> d.name().toLowerCase(Locale.ROOT)).toList(), args[4]);
            if (args.length == 6 && adminSub.equals("create") && can(sender, "koraquest.admin.editor")) return filter(Arrays.stream(ObjectiveType.values()).map(Enum::name).toList(), args[5]);
        }
        return List.of();
    }

    private boolean hasAnyAdminPermission(CommandSender sender) {
        if (sender.hasPermission("koraquest.admin")) return true;
        return ADMIN_PERMISSIONS.stream().anyMatch(sender::hasPermission);
    }

    private boolean can(CommandSender sender, String node) {
        return sender.hasPermission("koraquest.admin") || sender.hasPermission(node);
    }

    private List<String> filter(Collection<String> values, String input) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().limit(100).toList();
    }

    private enum Action { ACCEPT, CANCEL, CLAIM, REROLL }
=======
            List<String> values = new ArrayList<>(List.of("help", "active", "accept", "cancel", "complete", "version"));
            if (sender.hasPermission("koraquest.admin")) {
                values.addAll(List.of("update", "admin"));
            }
            return filter(values, args[0]);
        }
        if (List.of("accept", "cancel", "complete", "claim").contains(args[0].toLowerCase(Locale.ROOT)) && args.length == 2) {
            return filter(config.quests().keySet(), args[1]);
        }
        if (!args[0].equalsIgnoreCase("admin") || !sender.hasPermission("koraquest.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return filter(List.of("help", "reload", "reset", "create", "delete", "progress", "validate", "types"), args[1]);
        }
        if (args[1].equalsIgnoreCase("create")) {
            return switch (args.length) {
                case 4 -> filter(List.of("easy", "medium", "hard"), args[3]);
                case 5 -> filter(Arrays.stream(ObjectiveType.values()).map(Enum::name).toList(), args[4]);
                case 8 -> filter(Arrays.stream(Material.values()).map(Enum::name).toList(), args[7]);
                default -> Collections.emptyList();
            };
        }
        if (args[1].equalsIgnoreCase("delete") && args.length == 3) {
            return filter(config.quests().keySet(), args[2]);
        }
        if (args[1].equalsIgnoreCase("progress") && args.length == 4) {
            return filter(config.quests().keySet(), args[3]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(Collection<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .limit(100)
                .collect(Collectors.toList());
    }

    private enum Action {
        ACCEPT,
        CANCEL,
        COMPLETE
    }
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
}
