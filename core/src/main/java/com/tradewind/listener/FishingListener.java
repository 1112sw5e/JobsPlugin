package com.tradewind.listener;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.profession.BlockOutcome;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

/** Counts successful catches; auto-sell removes the item entity instead of dropping it. */
public final class FishingListener implements Listener {

    private final PluginContext context;

    public FishingListener(PluginContext context) {
        this.context = context;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                || !(event.getCaught() instanceof Item caught)) {
            return;
        }
        ItemStack stack = caught.getItemStack();
        BlockOutcome outcome = context.professions().handleFish(event.getPlayer(), stack);
        if (outcome.paid() && outcome.consumedDrops()) {
            event.setExpToDrop(0);
            caught.remove();
        }
    }
}

