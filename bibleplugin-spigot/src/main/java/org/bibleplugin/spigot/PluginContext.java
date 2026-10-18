package org.bibleplugin.spigot;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.persistence.store.PlayerDataStore;
import org.bibleplugin.spigot.book.TemporaryBookService;
import org.bibleplugin.spigot.config.Settings;
import org.bibleplugin.spigot.input.PromptService;
import org.bibleplugin.spigot.runtime.BibleRegistry;
import org.bibleplugin.spigot.runtime.BibleService;
import org.bibleplugin.spigot.sound.SoundPlayer;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Messages;

/**
 * The composition root: one place that holds every collaborator. Commands, menus and listeners
 * receive this instead of reaching for statics, which keeps them testable and the wiring visible.
 */
public final class PluginContext {

    private final BiblePlugin plugin;
    private final Executor executor;
    private final BibleRegistry registry;
    private final BibleService bibles;
    private final PlayerDataStore players;
    private final PromptService prompts;
    private final TemporaryBookService books;
    private volatile Settings settings;
    private volatile Messages messages;
    private volatile SoundPlayer sounds;

    PluginContext(BiblePlugin plugin, Executor executor, Settings settings, Messages messages,
                  BibleRegistry registry, BibleService bibles, PlayerDataStore players,
                  TemporaryBookService books) {
        this.plugin = plugin;
        this.executor = executor;
        this.settings = settings;
        this.messages = messages;
        this.registry = registry;
        this.bibles = bibles;
        this.players = players;
        this.prompts = new PromptService(this);
        this.books = books;
        this.sounds = new SoundPlayer(settings);
    }

    /** Re-reads {@code config.yml} and {@code messages.yml}. Bibles already in memory are kept. */
    public void reload(Messages freshMessages) {
        plugin.reloadConfig();
        this.settings = Settings.from(plugin.getConfig());
        this.messages = freshMessages;
        this.sounds = new SoundPlayer(settings);
        this.books.reconfigure(settings);
        registry.setDefaultBibleId(settings.defaultBible());
    }

    public BiblePlugin plugin() {
        return plugin;
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public BibleRegistry registry() {
        return registry;
    }

    public BibleService bibles() {
        return bibles;
    }

    public PlayerDataStore players() {
        return players;
    }

    public PromptService prompts() {
        return prompts;
    }

    public TemporaryBookService books() {
        return books;
    }

    public void play(Player player, UiSound sound) {
        sounds.play(player, sound);
    }

    /** Runs {@code task} on the server thread. Safe to call from any thread. */
    public void runSync(Runnable task) {
        if (plugin.getServer().isPrimaryThread()) {
            task.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /** Runs {@code work} on the background executor. */
    public <T> CompletableFuture<T> async(Supplier<T> work) {
        return CompletableFuture.supplyAsync(work, executor);
    }

    /**
     * @return the default Bible, which must be loaded
     * @throws BibleException {@link Problem#BIBLE_NOT_FOUND} when it is not
     */
    public Bible defaultBible() {
        String id = settings.defaultBible();
        return registry.find(id).orElseThrow(() -> new BibleException(Problem.BIBLE_NOT_FOUND,
                messages.get("bible.not-loaded", id)));
    }
}
