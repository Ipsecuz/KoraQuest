package dev.ipseucz.koraquest.event;

import dev.ipseucz.koraquest.model.QuestDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class QuestRerollEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final QuestDefinition oldQuest;
    private final QuestDefinition newQuest;
    private boolean cancelled;
    public QuestRerollEvent(Player player, QuestDefinition oldQuest, QuestDefinition newQuest) { this.player = player; this.oldQuest = oldQuest; this.newQuest = newQuest; }
    public Player getPlayer() { return player; }
    public QuestDefinition getOldQuest() { return oldQuest; }
    public QuestDefinition getNewQuest() { return newQuest; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
