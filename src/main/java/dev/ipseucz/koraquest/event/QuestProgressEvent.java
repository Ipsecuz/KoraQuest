package dev.ipseucz.koraquest.event;

import dev.ipseucz.koraquest.model.QuestDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class QuestProgressEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final QuestDefinition quest;
    private final int oldProgress;
    private final int progress;

    public QuestProgressEvent(Player player, QuestDefinition quest, int oldProgress, int progress) {
        this.player = player; this.quest = quest; this.oldProgress = oldProgress; this.progress = progress;
    }
    public Player getPlayer() { return player; }
    public QuestDefinition getQuest() { return quest; }
    public int getOldProgress() { return oldProgress; }
    public int getProgress() { return progress; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
