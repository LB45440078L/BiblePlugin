package org.bibleplugin.spigot;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bibleplugin.api.BiblePluginApi;
import org.bibleplugin.core.search.SearchEngine;
import org.bibleplugin.persistence.store.BibleFileStore;
import org.bibleplugin.persistence.store.PlayerDataStore;
import org.bibleplugin.source.catalog.BibleCatalogue;
import org.bibleplugin.source.http.BibleDownloader;
import org.bibleplugin.spigot.book.TemporaryBookService;
import org.bibleplugin.spigot.command.BibleCommand;
import org.bibleplugin.spigot.command.LibraryCommands;
import org.bibleplugin.spigot.command.ReadingCommands;
import org.bibleplugin.spigot.command.SearchCommands;
import org.bibleplugin.spigot.config.Settings;
import org.bibleplugin.spigot.gui.MenuListener;
import org.bibleplugin.spigot.listener.PlayerLifecycleListener;
import org.bibleplugin.spigot.runtime.BibleRegistry;
import org.bibleplugin.spigot.runtime.BibleService;
import org.bibleplugin.spigot.text.Messages;

/**
 * Plugin entry point. Builds the object graph once in {@link #onEnable()}, then leaves the work to
 * the collaborators held by {@link PluginContext}.
 */
public final class BiblePlugin extends JavaPlugin {

    /** One minute, in ticks: how often expired temporary books are swept. */
    private static final long SWEEP_PERIOD_TICKS = 20L * 60;

    private ExecutorService executor;
    private PluginContext context;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Settings settings = Settings.from(getConfig());
        Messages messages = Messages.load(this);
        Path dataFolder = getDataFolder().toPath();

        executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("bibleplugin-", 0).factory());
        BibleRegistry registry = new BibleRegistry(new SearchEngine(), settings.defaultBible());
        BibleService bibles = new BibleService(
                BibleCatalogue.bundled(),
                new BibleDownloader(settings.downloadTimeout(), settings.downloadAttempts()),
                new BibleFileStore(dataFolder.resolve("bibles")),
                registry,
                executor,
                dataFolder);
        context = new PluginContext(this, executor, settings, messages, registry, bibles,
                new PlayerDataStore(dataFolder.resolve("players")),
                new TemporaryBookService(this, settings));

        PluginCommand command = getCommand("bible");
        if (command != null) {
            BibleCommand router = new BibleCommand(context, List.of(
                    new LibraryCommands(), new ReadingCommands(), new SearchCommands()));
            command.setExecutor(router);
            command.setTabCompleter(router);
        }

        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(context.prompts(), this);
        plugins.registerEvents(new MenuListener(), this);
        plugins.registerEvents(new PlayerLifecycleListener(context), this);

        getServer().getServicesManager().register(BiblePluginApi.class, registry, this, ServicePriority.Normal);
        getServer().getScheduler().runTaskTimer(this, this::sweepTemporaryBooks,
                SWEEP_PERIOD_TICKS, SWEEP_PERIOD_TICKS);

        bibles.loadAll(settings.keepLoaded(), true).thenRun(() -> getLogger().info(
                "BiblePlugin ready: " + registry.loadedBibleIds().size() + " Bible(s) loaded, default "
                        + settings.defaultBible() + "."));
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void sweepTemporaryBooks() {
        long now = System.currentTimeMillis();
        for (Player player : getServer().getOnlinePlayers()) {
            context.books().sweep(player, now);
        }
    }
}
