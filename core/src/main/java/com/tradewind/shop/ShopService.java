package com.tradewind.shop;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.config.ProfessionSettings;
import com.tradewind.config.ShopItemDefinition;
import com.tradewind.model.PlayerData;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Purchase flow for unique crafts. */
public final class ShopService {

    public enum Result {
        OK, DISABLED, UNKNOWN_ITEM, NO_PERMISSION, LOCKED, NO_ECONOMY,
        NOT_ENOUGH_MONEY, WITHDRAW_FAILED
    }

    private final PluginContext context;

    public ShopService(PluginContext context) {
        this.context = context;
    }

    public ShopItemDefinition definition(String id) {
        return context.config().shop().find(id);
    }

    public Result buy(Player player, ShopItemDefinition definition) {
        if (!context.config().shop().enabled()) {
            return Result.DISABLED;
        }
        if (definition == null) {
            return Result.UNKNOWN_ITEM;
        }
        if (!definition.permission().isBlank() && !player.hasPermission(definition.permission())) {
            return Result.NO_PERMISSION;
        }
        PlayerData data = context.players().get(player);
        if (data == null) {
            return Result.NO_ECONOMY;
        }
        if (definition.requiredProfession() != null) {
            ProfessionSettings required = context.config().profession(definition.requiredProfession());
            if (required == null || data.profession().orElse(null) != definition.requiredProfession()
                    || data.stats(definition.requiredProfession()).level() < definition.requiredLevel()) {
                return Result.LOCKED;
            }
        } else if (definition.requiredLevel() > 1) {
            var selected = data.profession().orElse(null);
            if (selected == null || data.stats(selected).level() < definition.requiredLevel()) {
                return Result.LOCKED;
            }
        }
        if (!context.economy().available()) {
            return Result.NO_ECONOMY;
        }
        if (context.economy().balance(player) < definition.price()) {
            return Result.NOT_ENOUGH_MONEY;
        }
        if (!context.economy().withdraw(player, definition.price())) {
            return Result.WITHDRAW_FAILED;
        }
        ItemStack item = context.items().build(definition);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack overflow : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
        return Result.OK;
    }
}

