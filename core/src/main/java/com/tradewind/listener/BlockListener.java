package com.tradewind.listener;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.profession.BlockOutcome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Wires block placement and breaking into the profession service.
 *
 * <p>Breaking runs at HIGHEST and respects cancellation by protection plugins; the reward is
 * computed before vanilla drops, so auto-sell can suppress them and drop the rest itself.
 */
public final class BlockListener implements Listener {

    private final PluginContext context;

    public BlockListener(PluginContext context) {
        this.context = context;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (context.config().antiExploit().trackPlacedBlocks()) {
            context.placedBlocks().record(event.getBlockPlaced());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        BlockOutcome outcome = context.professions().handleBlockBreak(event.getPlayer(), event.getBlock());
        if (!outcome.paid()) {
            return;
        }
        if (outcome.consumedDrops()) {
            event.setDropItems(false);
            for (ItemStack remaining : outcome.remainingDrops()) {
                event.getBlock().getWorld().dropItemNaturally(
                        event.getBlock().getLocation().add(0.5, 0.5, 0.5), remaining);
            }
        }
    }
}

