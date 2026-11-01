package com.tradewind.config;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;

/** Material -> money price used by the buyer and auto-sell. */
public final class PriceIndex {

    private final Map<Material, Double> prices;

    public PriceIndex(Map<Material, Double> prices) {
        this.prices = Collections.unmodifiableMap(new EnumMap<>(prices));
    }

    public boolean sellable(Material material) {
        Double price = prices.get(material);
        return price != null && price > 0.0D;
    }

    public double price(Material material) {
        return prices.getOrDefault(material, 0.0D);
    }

    public int size() {
        return prices.size();
    }

    public Map<Material, Double> all() {
        return prices;
    }
}

