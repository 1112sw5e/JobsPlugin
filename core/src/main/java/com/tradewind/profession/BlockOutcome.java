package com.tradewind.profession;

import java.util.List;
import org.bukkit.inventory.ItemStack;

/**
 * What the block listener has to do after a break: drop the items nobody bought,
 * and suppress the vanilla drops when auto-sell consumed them.
 */
public record BlockOutcome(Reward reward, boolean consumedDrops, List<ItemStack> remainingDrops) {

    public static final BlockOutcome NONE = new BlockOutcome(Reward.NONE, false, List.of());

    public boolean paid() {
        return reward.counted();
    }
}

