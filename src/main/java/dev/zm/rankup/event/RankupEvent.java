package dev.zm.rankup.event;

import dev.zm.rankup.rank.Rank;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankupEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final Rank fromRank;
    private final Rank toRank;
    private boolean cancelled = false;

    public RankupEvent(@NotNull Player player, @Nullable Rank fromRank, @NotNull Rank toRank) {
        this.player = player;
        this.fromRank = fromRank;
        this.toRank = toRank;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Nullable
    public Rank getFromRank() {
        return fromRank;
    }

    @NotNull
    public Rank getToRank() {
        return toRank;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
