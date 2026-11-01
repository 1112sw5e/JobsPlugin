package com.tradewind.boost;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.config.BoostDefinition;
import com.tradewind.model.ActiveBoost;
import com.tradewind.model.BoostType;
import com.tradewind.model.PlayerData;
import java.util.Optional;
import org.bukkit.entity.Player;

/** Purchasing and applying money/xp multipliers. */
public final class BoostService {

    private final PluginContext context;

    public BoostService(PluginContext context) {
        this.context = context;
    }

    public double multiplier(PlayerData data, BoostType type) {
        return data.boost(type).map(ActiveBoost::multiplier).orElse(1.0D);
    }

    public Optional<ActiveBoost> active(PlayerData data, BoostType type) {
        return data.boost(type);
    }

    public enum Purchase {
        OK, DISABLED, NO_DEFINITION, NO_PERMISSION, NO_ECONOMY, NOT_ENOUGH_MONEY, WITHDRAW_FAILED
    }

    public Purchase buy(Player player, BoostType type) {
        if (!context.config().buyer().enabled() && !context.config().levels().enabled()) {
            return Purchase.DISABLED;
        }
        BoostDefinition definition = context.config().boost(type);
        if (definition == null) {
            return Purchase.NO_DEFINITION;
        }
        if (!player.hasPermission(definition.permission())) {
            return Purchase.NO_PERMISSION;
        }
        if (!context.economy().available()) {
            return Purchase.NO_ECONOMY;
        }
        PlayerData data = context.players().get(player);
        if (data == null) {
            return Purchase.NO_ECONOMY;
        }
        if (context.economy().balance(player) < definition.price()) {
            return Purchase.NOT_ENOUGH_MONEY;
        }
        if (!context.economy().withdraw(player, definition.price())) {
            return Purchase.WITHDRAW_FAILED;
        }
        long now = System.currentTimeMillis();
        long base = data.boost(type).map(ActiveBoost::expiresAt).orElse(now);
        long expiry = Math.max(now, base) + definition.durationMillis();
        data.boost(new ActiveBoost(type, definition.multiplier(), expiry));
        data.markDirty();
        return Purchase.OK;
    }

    /** Admin grant: no economy involved, duration and multiplier from the config. */
    public boolean grant(PlayerData data, BoostType type, long durationMillis) {
        BoostDefinition definition = context.config().boost(type);
        if (definition == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long base = data.boost(type).map(ActiveBoost::expiresAt).orElse(now);
        data.boost(new ActiveBoost(type, definition.multiplier(),
                Math.max(now, base) + Math.max(1L, durationMillis)));
        data.markDirty();
        return true;
    }
}

