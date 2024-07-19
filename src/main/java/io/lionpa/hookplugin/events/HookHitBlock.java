package io.lionpa.hookplugin.events;

import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class HookHitBlock extends PlayerEvent {
    private static final HandlerList handlerList = new HandlerList();

    private final Trident hook;
    public HookHitBlock(@NotNull Player player, Trident hook) {
        super(player);
        this.hook = hook;
    }

    public Trident getHook() {
        return hook;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
