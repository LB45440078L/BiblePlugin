package org.bibleplugin.spigot.listener;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bibleplugin.core.VersePicker;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.persistence.store.PlayerRecord;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Style;
import org.bibleplugin.spigot.text.VerseFormatter;

/** Shows a verse on join, and keeps temporary books from outliving their owner. */
public final class PlayerLifecycleListener implements Listener {

    /** Delay before the join verse, so it appears below the join message. */
    private static final long JOIN_DELAY_TICKS = 40L;

    private final PluginContext services;

    public PlayerLifecycleListener(PluginContext services) {
        this.services = services;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!services.settings().loginVerse()) {
            return;
        }
        Player player = event.getPlayer();
        PlayerRecord record = services.players().get(player.getUniqueId());
        if (!record.loginVerse()) {
            return;
        }
        services.plugin().getServer().getScheduler().runTaskLater(services.plugin(), () -> {
            if (!player.isOnline()) {
                return;
            }
            services.registry().defaultBibleId()
                    .flatMap(id -> services.registry().find(id))
                    .ifPresent(bible -> showJoinVerse(player, bible));
        }, JOIN_DELAY_TICKS);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        services.books().purge(event.getPlayer());
        services.prompts().cancel(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        services.books().purgeDrops(event.getDrops());
    }

    private void showJoinVerse(Player player, Bible bible) {
        var reference = VersePicker.random(bible, ThreadLocalRandom.current());
        player.sendMessage(Style.GOLD + "✦ " + Style.MUTED + "A verse for you " + Style.DIM + Style.DOT + " "
                + Style.AMBER + bible.id().toUpperCase(java.util.Locale.ROOT));
        VerseFormatter.lines(bible, bible.passage(reference), 3).forEach(player::sendMessage);
        services.play(player, UiSound.TURN_ON);
    }
}
