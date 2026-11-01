package com.tradewind.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ordered shop contents. */
public final class ShopCatalog {

    private final boolean enabled;
    private final int rows;
    private final String title;
    private final Map<String, ShopItemDefinition> items;

    public ShopCatalog(boolean enabled, int rows, String title, Map<String, ShopItemDefinition> items) {
        this.enabled = enabled;
        this.rows = rows;
        this.title = title;
        this.items = Collections.unmodifiableMap(new LinkedHashMap<>(items));
    }

    public boolean enabled() {
        return enabled;
    }

    public int rows() {
        return rows;
    }

    public String title() {
        return title;
    }

    public Map<String, ShopItemDefinition> items() {
        return items;
    }

    public ShopItemDefinition find(String id) {
        return items.get(id);
    }
}

