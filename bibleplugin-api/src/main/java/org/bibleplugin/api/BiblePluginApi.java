package org.bibleplugin.api;

import java.util.Optional;
import java.util.Set;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.core.model.VerseText;
import org.bibleplugin.core.readability.Readability;
import org.bibleplugin.core.search.SearchEngine;

/**
 * Read-only access to the Bibles that BiblePlugin has loaded. Obtain an instance from the
 * Bukkit {@code ServicesManager} after the plugin has enabled:
 *
 * <pre>{@code
 * RegisteredServiceProvider<BiblePluginApi> provider =
 *     getServer().getServicesManager().getRegistration(BiblePluginApi.class);
 * BiblePluginApi bible = provider.getProvider();
 * bible.passage("kjv", "John 3:16").forEach(v -> getLogger().info(v.text()));
 * }</pre>
 *
 * <p>Methods are safe to call from any thread. Nothing here blocks on disk or network access:
 * only Bibles that are already loaded can be queried.
 */
public interface BiblePluginApi {

    /** @return the ids of Bibles currently loaded in memory, sorted. */
    Set<String> loadedBibleIds();

    /** @return the loaded Bible with this id, if any. */
    Optional<Bible> loadedBible(String id);

    /** @return the server's default Bible id from the configuration, if one is set. */
    Optional<String> defaultBibleId();

    /** @return the reading statistics of a loaded Bible, if it has been measured. */
    Optional<Readability> readability(String bibleId);

    /**
     * Parses and validates a reference such as {@code John 3:16-18} against a loaded Bible.
     *
     * @throws org.bibleplugin.core.exception.BibleException when the Bible is not loaded or the reference is invalid
     */
    Reference resolve(String bibleId, String reference);

    /**
     * @return every verse covered by the reference, in order.
     * @throws org.bibleplugin.core.exception.BibleException when the Bible is not loaded or the reference is invalid
     */
    java.util.List<VerseText> passage(String bibleId, String reference);

    /**
     * Searches a loaded Bible for a word or phrase.
     *
     * @param scope the book or chapter to search, or empty for the whole Bible
     * @param limit the maximum number of hits to return
     */
    SearchEngine.Result search(String bibleId, Optional<Reference> scope, String query, int limit);

    /** Registers a callback fired when Bibles are loaded or unloaded. */
    void addListener(BibleListener listener);

    /** Removes a callback added with {@link #addListener(BibleListener)}. */
    void removeListener(BibleListener listener);
}
