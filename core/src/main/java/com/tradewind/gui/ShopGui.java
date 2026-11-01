package com.tradewind.gui;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.config.ShopItemDefinition;
import com.tradewind.shop.ShopService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Unique crafts: one icon per configured item, click to buy one. */
public final class ShopGui extends Gui {

    private final List<String> slotItem = new ArrayList<>();

    public ShopGui(PluginContext context, Player viewer) {
        super(context, viewer, context.config().shop().rows(), context.config().shop().title());
        build();
    }

    private void build() {
        List<Integer> slots = contentSlots(inventory.getSize());
        int index = 0;
        for (ShopItemDefinition definition : context.config().shop().items().values()) {
            if (index >= slots.size()) {
                break;
            }
            int slot = slots.get(index++);
            slotItem.add(definition.id());
            inventory.setItem(slot, icon(definition.material(),
                    context.messages().component("shop.gui.entry.name",
                            Map.of("display", definition.display())),
                    lore(definition)));
        }
        fill(filler());
    }

    private List<Component> lore(ShopItemDefinition definition) {
        Map<String, String> values = Map.of(
                "price", context.economy().format(definition.price()),
                "level", String.valueOf(definition.requiredLevel()),
                "profession", definition.requiredProfession() == null
                        ? "—" : context.config().profession(definition.requiredProfession()).display());
        List<Component> lore = new ArrayList<>(context.messages()
                .componentList("shop.gui.entry.lore", values));
        for (String line : definition.lore()) {
            lore.add(com.tradewind.text.Text.render(line));
        }
        return lore;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int index = contentSlots(inventory.getSize()).indexOf(event.getRawSlot());
        if (index < 0 || index >= slotItem.size()) {
            return;
        }
        ShopItemDefinition definition = context.shop().definition(slotItem.get(index));
        ShopService.Result result = context.shop().buy(viewer, definition);
        switch (result) {
            case OK -> {
                context.messages().send(viewer, "shop.bought",
                        "display", definition.display(),
                        "price", context.economy().format(definition.price()));
                new ShopGui(context, viewer).open();
            }
            case NOT_ENOUGH_MONEY -> context.messages().send(viewer, "shop.not-enough-money");
            case LOCKED -> context.messages().send(viewer, "shop.locked",
                    "level", String.valueOf(definition.requiredLevel()));
            case NO_PERMISSION -> context.messages().send(viewer, "command.no-permission");
            case NO_ECONOMY, WITHDRAW_FAILED -> context.messages().send(viewer, "economy-unavailable");
            case DISABLED -> context.messages().send(viewer, "shop.disabled");
            case UNKNOWN_ITEM -> context.messages().send(viewer, "shop.unknown-item");
        }
    }

    /** Interior slots in reading order, leaving one row of border. */
    private static List<Integer> contentSlots(int size) {
        List<Integer> slots = new ArrayList<>();
        int rows = size / 9;
        for (int row = 1; row < rows - 1; row++) {
            for (int column = 1; column < 8; column++) {
                slots.add(row * 9 + column);
            }
        }
        return slots;
    }
}

