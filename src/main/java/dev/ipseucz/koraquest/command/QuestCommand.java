package dev.ipseucz.koraquest.command;

import dev.ipseucz.koraquest.KoraQuestPlugin;
import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.config.MessageService;
import dev.ipseucz.koraquest.config.QuestConfig;
import dev.ipseucz.koraquest.data.PlayerQuestData;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class QuestCommand implements CommandExecutor, TabCompleter {
    private final KoraQuestPlugin plugin;
    private final QuestManager manager;
    private final QuestConfig config;
    private final MessageService messages;
    private final QuestGui gui;
    private final UpdateChecker updateChecker;

    public QuestCommand(KoraQuestPlugin plugin, QuestManager manager, QuestConfig config, MessageService messages,
                        QuestGui gui, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
        this.messages = messages;
        this.gui = gui;
        this.updateChecker = updateChecker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
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
            default -> messages.send(sender, "unknown-command");
        }
        return true;
    }

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
            default -> messages.sendList(sender, "admin-help", Map.of());
        }
    }

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
        });
    }

    private void validate(CommandSender sender) {
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
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
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
}
