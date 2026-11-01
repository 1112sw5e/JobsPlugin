package com.tradewind.listener;

import com.tradewind.bootstrap.PluginContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Loads profiles on join and persists/evicts them on quit. */
public final class SessionListener implements Listener {

    private final PluginContext context;

    public SessionListener(PluginContext context) {
        this.context = context;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        context.players().loadAsync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        context.limiter().clear(event.getPlayer().getUniqueId());
        context.buyer().clear(event.getPlayer().getUniqueId());
        context.professions().clear(event.getPlayer().getUniqueId());
        context.players().unloadAsync(event.getPlayer().getUniqueId());
    }
}

