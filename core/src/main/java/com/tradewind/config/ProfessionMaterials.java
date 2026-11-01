package com.tradewind.config;

import com.tradewind.model.Profession;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.Tag;

/** Default block sets per profession, used when the config list is empty. */
public final class ProfessionMaterials {

    private ProfessionMaterials() {
    }

    public static Set<Material> defaults(Profession profession) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        switch (profession) {
            case MINE -> {
                materials.addAll(List.of(
                        Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
                        Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.CALCITE,
                        Material.TUFF, Material.DRIPSTONE_BLOCK, Material.NETHERRACK, Material.BASALT,
                        Material.BLACKSTONE, Material.END_STONE, Material.ANCIENT_DEBRIS,
                        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
                        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
                        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
                        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
                        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
                        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
                        Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE
                ));
            }
            case WOOD -> materials.addAll(Tag.LOGS.getValues());
            case FARM -> materials.addAll(List.of(
                    Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
                    Material.NETHER_WART, Material.PUMPKIN, Material.MELON, Material.SUGAR_CANE,
                    Material.COCOA, Material.SWEET_BERRY_BUSH, Material.BAMBOO, Material.CACTUS
            ));
            case SNOW -> materials.addAll(List.of(
                    Material.SNOW, Material.SNOW_BLOCK, Material.POWDER_SNOW, Material.ICE,
                    Material.PACKED_ICE, Material.BLUE_ICE
            ));
            case FISH -> {
                // Fish is caught with a rod, no block set is required.
            }
        }
        return materials;
    }
}

