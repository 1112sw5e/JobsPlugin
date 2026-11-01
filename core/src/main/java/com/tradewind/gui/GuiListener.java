package com.tradewind.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Cancels every interaction with plugin inventories before the GUI sees it.
 * This is what makes shift-clicks, number keys and drags harmless.
 */
public final class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Gui gui)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        gui.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Gui) {
            event.setCancelled(true);
        }
    }
}

