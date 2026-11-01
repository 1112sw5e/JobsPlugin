package com.tradewind.gui;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.buyer.BuyerService;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Collector interface: shows what the inventory is worth and sells it in one click. */
public final class BuyerGui extends Gui {

    private static final int INFO_SLOT = 13;
    private static final int SELL_ALL_SLOT = 20;
    private static final int SELL_HAND_SLOT = 22;
    private static final int CLOSE_SLOT = 24;

    public BuyerGui(PluginContext context, Player viewer) {
        super(context, viewer, context.config().buyer().rows(), context.config().buyer().title());
        build();
    }

    private void build() {
        BuyerService.Estimate estimate = context.buyer().estimate(viewer);
        Map<String, String> values = Map.of(
                "items", String.valueOf(estimate.items()),
                "money", context.economy().format(estimate.money()));

        inventory.setItem(INFO_SLOT, icon(Material.PAPER,
                context.messages().component("buyer.gui.info.name", values),
                context.messages().componentList("buyer.gui.info.lore", values)));
        inventory.setItem(SELL_ALL_SLOT, icon(Material.EMERALD_BLOCK,
                context.messages().component("buyer.gui.sell-all.name", values),
                context.messages().componentList("buyer.gui.sell-all.lore", values)));
        inventory.setItem(SELL_HAND_SLOT, icon(Material.GOLD_INGOT,
                context.messages().component("buyer.gui.sell-hand.name", values),
                context.messages().componentList("buyer.gui.sell-hand.lore", values)));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER,
                context.messages().component("buyer.gui.close.name"),
                context.messages().componentList("buyer.gui.close.lore")));
        fill(filler());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case SELL_ALL_SLOT -> {
                var result = context.buyer().sellInventory(viewer);
                report(result);
            }
            case SELL_HAND_SLOT -> {
                var result = context.buyer().sellHand(viewer);
                report(result);
            }
            case CLOSE_SLOT -> viewer.closeInventory();
            default -> {
                return;
            }
        }
        new BuyerGui(context, viewer).open();
    }

    private void report(BuyerService.SaleResult result) {
        if (result.success()) {
            context.messages().send(viewer, "buyer.sold",
                    "items", String.valueOf(result.items()),
                    "money", context.economy().format(result.money()));
        } else {
            context.messages().send(viewer, result.failureKey());
        }
    }
}

