package dev.ipseucz.koraquest.event;

import dev.ipseucz.koraquest.model.QuestDefinition;
import dev.ipseucz.koraquest.model.RewardClaim;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class QuestRewardEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final QuestDefinition quest;
    private final RewardClaim claim;
    public QuestRewardEvent(Player player, QuestDefinition quest, RewardClaim claim) { this.player = player; this.quest = quest; this.claim = claim; }
    public Player getPlayer() { return player; }
    public QuestDefinition getQuest() { return quest; }
    public RewardClaim getClaim() { return claim; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
