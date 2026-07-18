package dev.ipseucz.koraquest.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        Material safe = material == null || material == Material.AIR ? Material.BARRIER : material;
        this.item = new ItemStack(safe);
        this.meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(Text.color("&r" + (name == null ? "" : name)));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta == null) {
            return this;
        }
        List<String> lore = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                lore.add(Text.color("&r" + (line == null ? "" : line)));
            }
        }
        meta.setLore(lore);
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
