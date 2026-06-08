package dev.zm.rankup.event;

import dev.zm.rankup.requirement.Requirement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RequirementCheckEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Requirement requirement;
    private boolean result;
    private boolean cancelled = false;

    public RequirementCheckEvent(@NotNull Player player, @NotNull Requirement requirement, boolean result) {
        this.player = player;
        this.requirement = requirement;
        this.result = result;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Requirement getRequirement() {
        return requirement;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
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
