package com.tradewind.config;

import com.tradewind.model.Profession;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

/** One unique craft sold in the shop GUI. */
public record ShopItemDefinition(
        String id,
        Material material,
        int amount,
        String display,
        List<String> lore,
        double price,
        Profession requiredProfession,
        int requiredLevel,
        Map<String, Integer> enchantments,
        boolean unbreakable,
        String permission
) {
}

