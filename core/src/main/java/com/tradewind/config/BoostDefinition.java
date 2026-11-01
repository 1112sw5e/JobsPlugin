package com.tradewind.config;

import com.tradewind.model.BoostType;

/** One purchasable boost from {@code boosts.yml}. */
public record BoostDefinition(
        BoostType type,
        String display,
        double multiplier,
        long durationMillis,
        double price,
        String permission,
        String material
) {
}

