package com.tradewind.item;

import com.tradewind.config.ShopItemDefinition;
import com.tradewind.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Builds and recognises shop items; the marker also blocks re-selling them. */
public final class CustomItems {

    private final NamespacedKey shopKey;

    public CustomItems(JavaPlugin plugin) {
        this.shopKey = new NamespacedKey(plugin, "shop_item");
    }

    public boolean isCustom(ItemStack stack) {
        return id(stack).isPresent();
    }

    public Optional<String> id(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(meta.getPersistentDataContainer()
                .get(shopKey, PersistentDataType.STRING));
    }

    public ItemStack build(ShopItemDefinition definition) {
        ItemStack stack = new ItemStack(definition.material(), definition.amount());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        Component name = Text.render(definition.display());
        meta.displayName(name);
        if (!definition.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>(definition.lore().size());
            for (String line : definition.lore()) {
                lore.add(Text.render(line));
            }
            meta.lore(lore);
        }
        for (var entry : definition.enchantments().entrySet()) {
            Enchantment enchantment = com.tradewind.config.MainConfig.resolveEnchantment(entry.getKey());
            if (enchantment != null) {
                meta.addEnchant(enchantment, entry.getValue(), true);
            }
        }
        if (definition.unbreakable()) {
            meta.setUnbreakable(true);
        }
        meta.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, definition.id());
        stack.setItemMeta(meta);
        return stack;
    }
}

