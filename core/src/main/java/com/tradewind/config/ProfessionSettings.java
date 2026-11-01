package com.tradewind.config;

import com.tradewind.model.Profession;
import java.util.Set;
import org.bukkit.Material;

/** Per-profession tuning from {@code config.yml}. */
public record ProfessionSettings(
        Profession profession,
        boolean enabled,
        String display,
        String region,
        double salaryPerAction,
        long xpPerAction,
        Set<Material> blocks
) {

    public boolean accepts(Material material) {
        return blocks.contains(material);
    }
}

