package com.tradewind.config;

/** {@code buyer} section. */
public record BuyerSettings(
        boolean enabled,
        int rows,
        String title,
        long cooldownMillis,
        boolean autoSellDefault
) {
}

