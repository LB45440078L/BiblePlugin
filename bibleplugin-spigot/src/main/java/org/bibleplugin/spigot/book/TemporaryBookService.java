package org.bibleplugin.spigot.book;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bibleplugin.core.book.TemporaryBook;
import org.bibleplugin.spigot.config.Settings;
import org.bibleplugin.spigot.text.Style;

/**
 * Hands out written books that disappear again. Each book is tagged with the time it was made in
 * its persistent data container; they are removed after the configured lifetime, when the owner
 * logs out, and when they would drop on death. Books are never stacked into chests by the plugin.
 */
public final class TemporaryBookService {

    private static final Pattern VERSE_NUMBER = Pattern.compile("(?m)^(\\d{1,3}) ");

    private final NamespacedKey key;
    private volatile Settings settings;

    public TemporaryBookService(Plugin plugin, Settings settings) {
        this.key = new NamespacedKey(plugin, "temporary-book");
        this.settings = settings;
    }

    /** Picks up new lifetime and author values after a configuration reload. */
    public void reconfigure(Settings updated) {
        this.settings = updated;
    }

    /** Builds the written book item for {@code book}, tagged as temporary. */
    public ItemStack create(TemporaryBook book, long createdAt) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setTitle(book.title());
        meta.setAuthor(settings.bookAuthor());
        meta.setGeneration(BookMeta.Generation.ORIGINAL);
        for (String page : book.pages()) {
            meta.addPage(styled(page));
        }
        meta.setLore(List.of(
                Style.DIM + "Temporary · vanishes after " + minutes(settings.temporaryBookLifetime()) + " min",
                Style.DIM + "or when you log out."));
        meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, createdAt);
        item.setItemMeta(meta);
        return item;
    }

    /** Gives the book to the player, dropping it at their feet when the inventory is full. */
    public void give(Player player, TemporaryBook book) {
        ItemStack item = create(book, System.currentTimeMillis());
        var overflow = player.getInventory().addItem(item);
        overflow.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    /** Removes expired temporary books from one player's inventory. */
    public int sweep(Player player, long now) {
        long lifetimeMillis = settings.temporaryBookLifetime().toMillis();
        return removeMatching(player, item -> {
            Long createdAt = createdAt(item);
            return createdAt != null && now - createdAt >= lifetimeMillis;
        });
    }

    /** Removes every temporary book from a player's inventory, e.g. when they log out. */
    public int purge(Player player) {
        return removeMatching(player, item -> createdAt(item) != null);
    }

    /** Removes temporary books from a death drop list. */
    public void purgeDrops(List<ItemStack> drops) {
        drops.removeIf(item -> createdAt(item) != null);
    }

    private int removeMatching(Player player, java.util.function.Predicate<ItemStack> test) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item != null && test.test(item)) {
                player.getInventory().setItem(slot, null);
                removed++;
            }
        }
        return removed;
    }

    private Long createdAt(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.LONG);
    }

    /** Gives verse numbers a bold style, so pages are easier to scan. */
    static String styled(String page) {
        return VERSE_NUMBER.matcher(page).replaceAll("§l$1§r ");
    }

    private static long minutes(Duration lifetime) {
        return Math.max(1, lifetime.toMinutes());
    }
}
