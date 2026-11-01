package com.tradewind.buyer;

import com.tradewind.anti.ActionLimiter;
import com.tradewind.bootstrap.PluginContext;
import com.tradewind.model.PlayerData;
import com.tradewind.util.Numbers;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Selling inventory contents for money.
 *
 * <p>A sale is all-or-nothing: items are removed, the balance is credited, and on any economy
 * failure the exact previous inventory is restored. A per-player limiter stops macro-driven
 * open/sell spam, and shop items are never accepted back.
 */
public final class BuyerService {

    public record Estimate(int items, double money) {
        public static final Estimate EMPTY = new Estimate(0, 0.0D);
    }

    public record SaleResult(boolean success, int items, double money, String failureKey) {
        public static SaleResult ok(int items, double money) {
            return new SaleResult(true, items, money, null);
        }

        public static SaleResult fail(String key) {
            return new SaleResult(false, 0, 0.0D, key);
        }
    }

    private final PluginContext context;
    private final ActionLimiter limiter;

    public BuyerService(PluginContext context) {
        this.context = context;
        this.limiter = new ActionLimiter(
                context.config().antiExploit().buyerCooldownMillis(),
                context.config().antiExploit().maxBuyerSalesPerMinute(),
                60_000L);
    }

    public Estimate estimate(Player player) {
        int items = 0;
        double money = 0.0D;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            double price = unitValue(stack);
            if (price > 0.0D) {
                items += stack.getAmount();
                money += price * stack.getAmount();
            }
        }
        return new Estimate(items, Numbers.round(money, context.config().economyRoundTo()));
    }

    public void clear(UUID uuid) {
        limiter.clear(uuid);
    }

    public SaleResult sellInventory(Player player) {
        String blocked = check(player);
        if (blocked != null) {
            return SaleResult.fail(blocked);
        }
        if (!limiter.allow(player.getUniqueId())) {
            return SaleResult.fail("buyer.cooldown");
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        ItemStack[] backup = new ItemStack[contents.length];
        List<Integer> soldSlots = new ArrayList<>();
        int items = 0;
        double money = 0.0D;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null) {
                continue;
            }
            backup[slot] = stack.clone();
            double unit = unitValue(stack);
            if (unit <= 0.0D) {
                continue;
            }
            items += stack.getAmount();
            money += unit * stack.getAmount();
            soldSlots.add(slot);
        }
        if (items == 0) {
            return SaleResult.fail("buyer.nothing");
        }
        money = Numbers.round(money, context.config().economyRoundTo());
        if (!context.economy().available()) {
            return SaleResult.fail("economy-unavailable");
        }

        for (int slot : soldSlots) {
            contents[slot] = null;
        }
        player.getInventory().setStorageContents(contents);

        if (!context.economy().deposit(player, money)) {
            player.getInventory().setStorageContents(backup);
            return SaleResult.fail("economy-unavailable");
        }
        creditStats(player, money);
        if (context.config().debug()) {
            context.plugin().getLogger().info(player.getName() + " продал " + items
                    + " предметов за " + money);
        }
        return SaleResult.ok(items, money);
    }

    public SaleResult sellHand(Player player) {
        String blocked = check(player);
        if (blocked != null) {
            return SaleResult.fail(blocked);
        }
        if (!limiter.allow(player.getUniqueId())) {
            return SaleResult.fail("buyer.cooldown");
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        double unit = unitValue(hand);
        if (unit <= 0.0D) {
            return SaleResult.fail("buyer.nothing");
        }
        int items = hand.getAmount();
        double money = Numbers.round(unit * items, context.config().economyRoundTo());
        ItemStack backup = hand.clone();
        player.getInventory().setItemInMainHand(null);
        if (!context.economy().deposit(player, money)) {
            player.getInventory().setItemInMainHand(backup);
            return SaleResult.fail("economy-unavailable");
        }
        creditStats(player, money);
        return SaleResult.ok(items, money);
    }

    private String check(Player player) {
        if (!context.config().buyer().enabled()) {
            return "buyer.disabled";
        }
        if (context.players().get(player) == null) {
            return "profile-loading";
        }
        return null;
    }

    private void creditStats(Player player, double money) {
        PlayerData data = context.players().get(player);
        if (data == null) {
            return;
        }
        data.profession().ifPresent(profession -> data.stats(profession).addMoney(money));
        data.markDirty();
    }

    /** Price per single item, 0 when the item is not buyable by the collector. */
    public double unitValue(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || context.items().isCustom(stack)) {
            return 0.0D;
        }
        return context.config().prices().price(stack.getType());
    }
}

