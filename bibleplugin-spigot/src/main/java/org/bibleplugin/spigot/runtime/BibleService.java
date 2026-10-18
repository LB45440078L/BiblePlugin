package org.bibleplugin.spigot.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleInfo;
import org.bibleplugin.core.readability.ReadabilityAnalyzer;
import org.bibleplugin.persistence.binary.StoredBible;
import org.bibleplugin.persistence.store.BibleFileStore;
import org.bibleplugin.source.catalog.BibleCatalogue;
import org.bibleplugin.source.catalog.CatalogueEntry;
import org.bibleplugin.source.http.BibleDownloader;
import org.bibleplugin.source.json.BibleJsonImporter;

/**
 * Coordinates everything that touches disk or the network: downloading editions, importing user
 * files, and loading installed ones into the {@link BibleRegistry}. Every public method returns
 * immediately and does its work on the supplied executor, never on the server thread.
 */
public final class BibleService {

    private static final Logger LOG = Logger.getLogger(BibleService.class.getName());

    private final BibleCatalogue catalogue;
    private final BibleDownloader downloader;
    private final BibleFileStore store;
    private final BibleRegistry registry;
    private final Executor executor;
    private final Path cacheDirectory;
    private final Path importsDirectory;

    public BibleService(BibleCatalogue catalogue, BibleDownloader downloader, BibleFileStore store,
                        BibleRegistry registry, Executor executor, Path dataFolder) {
        this.catalogue = catalogue;
        this.downloader = downloader;
        this.store = store;
        this.registry = registry;
        this.executor = executor;
        this.cacheDirectory = dataFolder.resolve("cache");
        this.importsDirectory = dataFolder.resolve("imports");
    }

    public BibleCatalogue catalogue() {
        return catalogue;
    }

    public BibleFileStore store() {
        return store;
    }

    public Path importsDirectory() {
        return importsDirectory;
    }

    /** Loads an installed edition into memory. */
    public CompletableFuture<Bible> load(String id) {
        return CompletableFuture.supplyAsync(() -> {
            StoredBible stored = store.load(id);
            registry.put(stored);
            return stored.bible();
        }, executor);
    }

    /**
     * Downloads an edition from the catalogue, stores it, and loads it. Downloads that are already
     * installed are simply loaded.
     *
     * @param progress receives {@code (downloadedBytes, totalBytes)}; may be called from the executor thread
     */
    public CompletableFuture<Bible> download(String id, BibleDownloader.Progress progress) {
        return CompletableFuture.supplyAsync(() -> {
            if (store.exists(id)) {
                StoredBible stored = store.load(id);
                registry.put(stored);
                return stored.bible();
            }
            CatalogueEntry entry = catalogue.find(id).orElseThrow(() ->
                    new BibleException(Problem.UNKNOWN_SOURCE, "No edition called " + id + " is in the catalogue."));
            Path json = cacheDirectory.resolve(entry.id() + ".json");
            try {
                downloader.download(entry.url(), json, progress);
                Bible bible = BibleJsonImporter.read(json, entry.info());
                return install(bible);
            } finally {
                deleteQuietly(json);
            }
        }, executor);
    }

    /**
     * Imports a user-supplied JSON file from the {@code imports} folder.
     *
     * @param fileName file inside the imports folder, e.g. {@code niv.json}
     */
    public CompletableFuture<Bible> importFile(String fileName, BibleInfo info) {
        return CompletableFuture.supplyAsync(() -> {
            Path source = importsDirectory.resolve(fileName).normalize();
            if (!source.startsWith(importsDirectory) || !Files.isRegularFile(source)) {
                throw new BibleException(Problem.STORAGE_ERROR,
                        "Put the file in plugins/BiblePlugin/imports/ first (" + fileName + " was not found).");
            }
            return install(BibleJsonImporter.read(source, info));
        }, executor);
    }

    /** Removes a Bible from memory. Its file on disk is kept. */
    public boolean unload(String id) {
        return registry.remove(id);
    }

    /** Removes a Bible from memory and disk. */
    public CompletableFuture<Boolean> delete(String id) {
        return CompletableFuture.supplyAsync(() -> {
            registry.remove(id);
            return store.delete(id);
        }, executor);
    }

    /**
     * Loads every Bible in {@code ids} that is installed. Missing ones are downloaded when
     * {@code downloadMissing} is set, otherwise reported and skipped.
     */
    public CompletableFuture<Void> loadAll(List<String> ids, boolean downloadMissing) {
        CompletableFuture<?>[] tasks = ids.stream().<CompletableFuture<?>>map(id -> {
            if (store.exists(id)) {
                return load(id).exceptionally(failure -> logFailure(id, failure));
            }
            if (downloadMissing) {
                return download(id, BibleDownloader.Progress.NONE).exceptionally(failure -> logFailure(id, failure));
            }
            LOG.info("Bible " + id + " is listed in keep-loaded but not installed yet; use /bible download " + id + ".");
            return CompletableFuture.completedFuture(null);
        }).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(tasks);
    }

    private Bible install(Bible bible) {
        StoredBible stored = new StoredBible(bible, ReadabilityAnalyzer.analyze(bible));
        store.save(stored);
        registry.put(stored);
        LOG.info("Installed " + bible.id() + " (" + bible.verseCount() + " verses).");
        return bible;
    }

    private static Bible logFailure(String id, Throwable failure) {
        Throwable cause = failure.getCause() == null ? failure : failure.getCause();
        LOG.log(Level.WARNING, "Could not prepare Bible " + id + ": " + cause.getMessage(), cause);
        return null;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A leftover cache file is harmless and is overwritten on the next download.
        }
    }
}
