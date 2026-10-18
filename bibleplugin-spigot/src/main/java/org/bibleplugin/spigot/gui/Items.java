package org.bibleplugin.spigot.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bibleplugin.spigot.text.TextWrap;

/** Factory for the items shown in menus. Keeps names and lore formatting in one place. */
public final class Items {

    /** Width, in characters, of a lore line before wrapping. */
    public static final int LORE_WIDTH = 38;

    private Items() {
    }

    public static ItemStack of(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack of(Material material, String name, String... lore) {
        return of(material, name, List.of(lore));
    }

    /** Wraps a long text into lore lines in {@code colour}. */
    public static List<String> wrapped(String text, String colour) {
        List<String> lines = new ArrayList<>();
        for (String line : TextWrap.wrap(text, LORE_WIDTH)) {
            lines.add(colour + line);
        }
        return lines;
    }

    /** A dark pane used to visually separate control rows. */
    public static ItemStack filler() {
        return of(Material.GRAY_STAINED_GLASS_PANE, " ");
    }
}
