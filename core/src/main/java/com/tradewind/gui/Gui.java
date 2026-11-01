package com.tradewind.gui;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Base for every plugin inventory: the listener cancels all clicks, the GUI decides what runs. */
public abstract class Gui implements InventoryHolder {

    protected final PluginContext context;
    protected final Player viewer;
    protected final Inventory inventory;

    protected Gui(PluginContext context, Player viewer, int rows, String title) {
        this.context = context;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, Math.max(1, Math.min(6, rows)) * 9,
                Text.render(title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public abstract void handleClick(InventoryClickEvent event);

    protected void fill(ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, item);
            }
        }
    }

    protected static ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (!lore.isEmpty()) {
                meta.lore(new ArrayList<>(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    protected ItemStack filler() {
        return icon(Material.GRAY_STAINED_GLASS_PANE, Component.empty(), List.of());
    }
}

