package org.bibleplugin.spigot.input;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.config.Settings;
import org.bibleplugin.spigot.gui.Items;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Style;

/**
 * Asks players for a line of text. Two strategies share one interface: an anvil with a rename box
 * (default) and a sign editor. The configured {@code reader.input} picks which one opens; the
 * anvil is used as a fallback when a sign cannot be placed.
 *
 * <p>Strategy pattern: {@link #requestText} is the only entry point, and the caller never knows
 * which input device was used. Only the main thread touches the maps here.
 */
public final class PromptService implements Listener {

    /** What to do with the text, or when the player walks away without submitting. */
    public record Prompt(Consumer<String> onSubmit, Runnable onCancel) {
    }

    private static final long SIGN_TIMEOUT_TICKS = 20L * 60;

    private final PluginContext services;
    private final Map<UUID, Prompt> anvilPrompts = new HashMap<>();
    private final Map<UUID, SignSession> signSessions = new HashMap<>();

    public PromptService(PluginContext services) {
        this.services = services;
    }

    public void requestText(Player player, String title, String initial, Consumer<String> onSubmit) {
        requestText(player, title, initial, onSubmit, () -> { });
    }

    public void requestText(Player player, String title, String initial, Consumer<String> onSubmit, Runnable onCancel) {
        cancel(player);
        Prompt prompt = new Prompt(onSubmit, onCancel);
        if (services.settings().inputMode() == Settings.InputMode.SIGN && openSign(player, prompt)) {
            return;
        }
        openAnvil(player, title, initial, prompt);
    }

    /** Silently drops any prompt the player has open. */
    public void cancel(Player player) {
        anvilPrompts.remove(player.getUniqueId());
        SignSession session = signSessions.remove(player.getUniqueId());
        if (session != null) {
            session.restore();
        }
    }

    private void openAnvil(Player player, String title, String initial, Prompt prompt) {
        PromptHolder holder = new PromptHolder();
        Inventory inventory = Bukkit.createInventory(holder, InventoryType.ANVIL, title);
        holder.inventory = inventory;
        inventory.setItem(0, Items.of(Material.PAPER, initial == null || initial.isBlank() ? " " : initial));
        anvilPrompts.put(player.getUniqueId(), prompt);
        player.openInventory(inventory);
        services.play(player, UiSound.OPEN);
    }

    private boolean openSign(Player player, Prompt prompt) {
        Block target = player.getLocation().getBlock().getRelative(BlockFace.UP, 3);
        if (!target.getType().isAir()) {
            return false;
        }
        BlockData original = target.getBlockData();
        target.setType(Material.OAK_SIGN, false);
        if (!(target.getState() instanceof Sign sign)) {
            target.setBlockData(original, false);
            return false;
        }
        SignSession session = new SignSession(target, original, prompt, player);
        signSessions.put(player.getUniqueId(), session);
        player.openSign(sign, Side.FRONT);
        services.plugin().getServer().getScheduler().runTaskLater(services.plugin(), () -> {
            if (signSessions.remove(player.getUniqueId(), session)) {
                session.restore();
                prompt.onCancel().run();
            }
        }, SIGN_TIMEOUT_TICKS);
        return true;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getInventory().getHolder() instanceof PromptHolder) {
            event.setResult(Items.of(Material.NAME_TAG, Style.SUCCESS + "» Submit"));
        }
    }

    @EventHandler
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PromptHolder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() != 2 || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String rename = event.getView() instanceof AnvilView view ? view.getRenameText() : null;
        String text = rename == null ? "" : rename.strip();
        if (text.isEmpty()) {
            services.play(player, UiSound.ERROR);
            player.sendMessage(services.messages().prefixed("prompt.empty"));
            return;
        }
        Prompt prompt = anvilPrompts.remove(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        player.closeInventory();
        services.play(player, UiSound.CLICK);
        prompt.onSubmit().accept(text);
    }

    @EventHandler
    public void onAnvilClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PromptHolder
                && event.getPlayer() instanceof Player player) {
            Prompt prompt = anvilPrompts.remove(player.getUniqueId());
            if (prompt != null) {
                prompt.onCancel().run();
            }
        }
    }

    @EventHandler
    public void onAnvilDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PromptHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        SignSession session = signSessions.get(event.getPlayer().getUniqueId());
        if (session == null || !event.getBlock().getLocation().equals(session.location())) {
            return;
        }
        event.setCancelled(true);
        signSessions.remove(event.getPlayer().getUniqueId());
        session.restore();
        String text = String.join(" ", event.getLines()).strip();
        if (text.isEmpty()) {
            session.prompt().onCancel().run();
        } else {
            session.prompt().onSubmit().accept(text);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer());
    }

    /** Holder that marks an inventory as a prompt, so its clicks are never mistaken for menu clicks. */
    private static final class PromptHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** A temporary sign placed above the player, remembered so it can be removed again. */
    private record SignSession(Block block, BlockData original, Prompt prompt, Player player) {

        SignSession(Block block, BlockData original, Prompt prompt, Player player) {
            this.block = block;
            this.original = original;
            this.prompt = prompt;
            this.player = player;
        }

        org.bukkit.Location location() {
            return block.getLocation();
        }

        void restore() {
            block.setBlockData(original, false);
        }
    }
}
