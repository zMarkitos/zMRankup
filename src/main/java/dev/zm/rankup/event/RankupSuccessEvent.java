package dev.zm.rankup.event;

import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.reward.Reward;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RankupSuccessEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Rank oldRank;
    private final Rank newRank;
    private final List<Reward> rewards;

    public RankupSuccessEvent(@NotNull Player player, @Nullable Rank oldRank, @NotNull Rank newRank, @NotNull List<Reward> rewards) {
        this.player = player;
        this.oldRank = oldRank;
        this.newRank = newRank;
        this.rewards = rewards;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Nullable
    public Rank getOldRank() {
        return oldRank;
    }

    @NotNull
    public Rank getNewRank() {
        return newRank;
    }

    @NotNull
    public List<Reward> getRewards() {
        return rewards;
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
