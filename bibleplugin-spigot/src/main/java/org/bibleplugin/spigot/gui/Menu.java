package org.bibleplugin.spigot.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.sound.UiSound;

/**
 * Base class for chest-style menus. A menu is its own {@link InventoryHolder}, so click handling
 * can recognise menus without any global registry. Subclasses describe their layout in
 * {@link #populate(Player)} by calling {@link #set(int, ItemStack, Action)}.
 *
 * <p>Template method: {@link #open(Player)} and {@link #refresh()} run the same population step.
 */
public abstract class Menu implements InventoryHolder {

    /** What happens when a button is clicked. */
    @FunctionalInterface
    public interface Action {
        void run(Player player, ClickType click);
    }

    private record Button(ItemStack item, Action action) {
    }

    protected final PluginContext context;
    private final Map<Integer, Button> buttons = new HashMap<>();
    private Inventory inventory;
    private Player viewer;

    protected Menu(PluginContext context) {
        this.context = context;
    }

    /** @return the window title, using colour codes. */
    protected abstract String title();

    /** @return the number of rows, from 1 to 6. */
    protected abstract int rows();

    /** Places items and buttons for {@code viewer}. Called on every open and refresh. */
    protected abstract void populate(Player viewer);

    /** Opens a fresh inventory for {@code player}. */
    public final void open(Player player) {
        this.viewer = player;
        this.inventory = Bukkit.createInventory(this, rows() * 9, title());
        render();
        player.openInventory(inventory);
    }

    /** Re-renders the open inventory, e.g. after a page change. */
    public final void refresh() {
        if (viewer == null || inventory == null || !viewer.isOnline()) {
            return;
        }
        render();
    }

    /** Handles a click that landed in this menu's top inventory. */
    public final void handleClick(Player player, int slot, ClickType click) {
        Button button = buttons.get(slot);
        if (button != null && button.action() != null) {
            button.action().run(player, click);
        }
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    protected final void set(int slot, ItemStack item, Action action) {
        if (slot < 0 || slot >= rows() * 9) {
            return;
        }
        inventory.setItem(slot, item);
        buttons.put(slot, new Button(item, action));
    }

    protected final void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    /** Fills every empty slot with the same decorative pane. */
    protected final void fillEmpty(ItemStack filler) {
        for (int slot = 0; slot < rows() * 9; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    /** @return a click action that plays a click sound and then runs {@code next}. */
    protected final Action navigate(Runnable next) {
        return (player, click) -> {
            context.play(player, UiSound.CLICK);
            next.run();
        };
    }

    /**
     * Adds previous / page indicator / next buttons. {@code onPage} receives the new zero-based
     * page; the menu is refreshed afterwards.
     */
    protected final void pager(Paging<?> paging, int previousSlot, int indicatorSlot, int nextSlot, IntConsumer onPage) {
        if (paging.hasPrevious()) {
            set(previousSlot, Items.of(Material.ARROW, "§e◀ Previous page"),
                    (player, click) -> turn(player, paging.page() - 1, onPage));
        }
        set(indicatorSlot, Items.of(Material.PAPER, "§7Page §f" + (paging.page() + 1) + "§7 of §f" + paging.pageCount()));
        if (paging.hasNext()) {
            set(nextSlot, Items.of(Material.ARROW, "§eNext page ▶"),
                    (player, click) -> turn(player, paging.page() + 1, onPage));
        }
    }

    private void turn(Player player, int page, IntConsumer onPage) {
        context.play(player, UiSound.PAGE);
        onPage.accept(page);
        refresh();
    }

    protected final Player viewer() {
        return viewer;
    }

    private void render() {
        inventory.clear();
        buttons.clear();
        populate(viewer);
    }
}
