package dev.ipseucz.koraquest.event;

import dev.ipseucz.koraquest.model.QuestDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class QuestCancelEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final QuestDefinition quest;
    private boolean cancelled;

    public QuestCancelEvent(Player player, QuestDefinition quest) { this.player = player; this.quest = quest; }
    public Player getPlayer() { return player; }
    public QuestDefinition getQuest() { return quest; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
