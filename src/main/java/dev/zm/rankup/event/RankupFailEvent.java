package dev.zm.rankup.event;

import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.requirement.Requirement;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RankupFailEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Rank targetRank;
    private final List<Requirement> failedRequirements;

    public RankupFailEvent(@NotNull Player player, @NotNull Rank targetRank, @NotNull List<Requirement> failedRequirements) {
        this.player = player;
        this.targetRank = targetRank;
        this.failedRequirements = failedRequirements;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Rank getTargetRank() {
        return targetRank;
    }

    @NotNull
    public List<Requirement> getFailedRequirements() {
        return failedRequirements;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
