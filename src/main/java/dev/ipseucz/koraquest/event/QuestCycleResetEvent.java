package dev.ipseucz.koraquest.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class QuestCycleResetEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String cycleId;
    private final long startedAt;
    public QuestCycleResetEvent(String cycleId, long startedAt) { this.cycleId = cycleId; this.startedAt = startedAt; }
    public String getCycleId() { return cycleId; }
    public long getStartedAt() { return startedAt; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
