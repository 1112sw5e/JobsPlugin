package com.tradewind.gui;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.boost.BoostService;
import com.tradewind.config.BoostDefinition;
import com.tradewind.model.ActiveBoost;
import com.tradewind.model.BoostType;
import com.tradewind.util.TimeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Boost shop: one icon per configured multiplier. */
public final class BoostGui extends Gui {

    private final List<BoostType> slotType = new ArrayList<>();

    public BoostGui(PluginContext context, Player viewer) {
        super(context, viewer, 3, "<dark_gray>Бусты</dark_gray>");
        build();
    }

    private void build() {
        int slot = 10;
        for (BoostDefinition definition : context.config().boosts().values()) {
            slotType.add(definition.type());
            Material material = Material.matchMaterial(definition.material());
            if (material == null || !material.isItem()) {
                material = Material.GOLD_INGOT;
            }
            inventory.setItem(slot++, icon(material,
                    com.tradewind.text.Text.render(definition.display()),
                    lore(definition)));
        }
        inventory.setItem(26, icon(Material.BARRIER,
                context.messages().component("buyer.gui.close.name"),
                context.messages().componentList("buyer.gui.close.lore")));
        fill(filler());
    }

    private List<Component> lore(BoostDefinition definition) {
        var data = context.players().get(viewer);
        ActiveBoost active = data == null ? null : data.boost(definition.type()).orElse(null);
        String remaining = active == null ? "—"
                : TimeUtil.format(active.remainingMillis(System.currentTimeMillis()));
        Map<String, String> values = Map.of(
                "multiplier", "x" + definition.multiplier(),
                "duration", TimeUtil.format(definition.durationMillis()),
                "price", context.economy().format(definition.price()),
                "remaining", remaining);
        return context.messages().componentList("boost.gui.entry.lore", values);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() == 26) {
            viewer.closeInventory();
            return;
        }
        int index = event.getRawSlot() - 10;
        if (index < 0 || index >= slotType.size()) {
            return;
        }
        BoostService.Purchase purchase = context.boosts().buy(viewer, slotType.get(index));
        switch (purchase) {
            case OK -> context.messages().send(viewer, "boost.bought");
            case NOT_ENOUGH_MONEY -> context.messages().send(viewer, "shop.not-enough-money");
            case NO_PERMISSION -> context.messages().send(viewer, "command.no-permission");
            case NO_DEFINITION -> context.messages().send(viewer, "boost.unavailable");
            case DISABLED -> context.messages().send(viewer, "boost.disabled");
            default -> context.messages().send(viewer, "economy-unavailable");
        }
        new BoostGui(context, viewer).open();
    }
}

