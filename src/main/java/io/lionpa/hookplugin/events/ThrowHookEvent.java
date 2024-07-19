package io.lionpa.hookplugin.events;

import io.lionpa.hookplugin.Events;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class ThrowHookEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlerList = new HandlerList();

    private boolean cancelled;
    private final Trident hook;

    public ThrowHookEvent(@NotNull Player player, Trident hook) {
        super(player);
        cancelled = false;
        this.hook = hook;
    }

    public Trident getHook() {
        return hook;
    }
    public void setDamageMultiplier(float damageMultiplier){
        hook.getPersistentDataContainer().set(Events.DAMAGE_MULTIPLIER, PersistentDataType.FLOAT,damageMultiplier);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
