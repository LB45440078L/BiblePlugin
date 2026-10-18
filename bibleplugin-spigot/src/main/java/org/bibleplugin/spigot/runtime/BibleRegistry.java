package org.bibleplugin.spigot.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bibleplugin.api.BibleListener;
import org.bibleplugin.api.BiblePluginApi;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.core.model.VerseText;
import org.bibleplugin.core.readability.Readability;
import org.bibleplugin.core.reference.ReferenceParser;
import org.bibleplugin.core.search.SearchEngine;
import org.bibleplugin.persistence.binary.StoredBible;

/**
 * The set of Bibles currently in memory, and the public {@link BiblePluginApi} implementation.
 * Lookups are lock-free ({@link ConcurrentHashMap}); listeners are notified synchronously in the
 * thread that changed the set.
 */
public final class BibleRegistry implements BiblePluginApi {

    private final Map<String, StoredBible> loaded = new ConcurrentHashMap<>();
    private final List<BibleListener> listeners = new CopyOnWriteArrayList<>();
    private final SearchEngine search;
    private volatile String defaultBibleId;

    public BibleRegistry(SearchEngine search, String defaultBibleId) {
        this.search = search;
        this.defaultBibleId = defaultBibleId;
    }

    /** Registers a Bible and notifies listeners. Replaces any previous copy with the same id. */
    public void put(StoredBible stored) {
        String id = stored.bible().id();
        loaded.put(id, stored);
        listeners.forEach(listener -> listener.onBibleLoaded(id));
    }

    /** @return {@code true} when a Bible was removed. */
    public boolean remove(String id) {
        StoredBible removed = loaded.remove(id);
        if (removed == null) {
            return false;
        }
        search.invalidate(removed.bible());
        listeners.forEach(listener -> listener.onBibleUnloaded(id));
        return true;
    }

    public void setDefaultBibleId(String id) {
        this.defaultBibleId = id;
    }

    /** @return the loaded Bible with this id. */
    public Optional<Bible> find(String id) {
        return Optional.ofNullable(loaded.get(id)).map(StoredBible::bible);
    }

    /**
     * @throws BibleException {@link Problem#BIBLE_NOT_FOUND} when the Bible is not loaded
     */
    public Bible require(String id) {
        return stored(id).bible();
    }

    public SearchEngine search() {
        return search;
    }

    @Override
    public Set<String> loadedBibleIds() {
        return new TreeSet<>(loaded.keySet());
    }

    @Override
    public Optional<Bible> loadedBible(String id) {
        return find(id);
    }

    @Override
    public Optional<String> defaultBibleId() {
        return Optional.ofNullable(defaultBibleId);
    }

    @Override
    public Optional<Readability> readability(String bibleId) {
        return Optional.ofNullable(loaded.get(bibleId)).map(StoredBible::readability);
    }

    @Override
    public Reference resolve(String bibleId, String reference) {
        return ReferenceParser.resolve(require(bibleId), reference);
    }

    @Override
    public List<VerseText> passage(String bibleId, String reference) {
        Bible bible = require(bibleId);
        return bible.passage(ReferenceParser.resolve(bible, reference));
    }

    @Override
    public SearchEngine.Result search(String bibleId, Optional<Reference> scope, String query, int limit) {
        return search.search(require(bibleId), scope, query, limit);
    }

    @Override
    public void addListener(BibleListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(BibleListener listener) {
        listeners.remove(listener);
    }

    private StoredBible stored(String id) {
        StoredBible stored = loaded.get(id);
        if (stored == null) {
            throw new BibleException(Problem.BIBLE_NOT_FOUND, "The Bible " + id + " is not loaded.");
        }
        return stored;
    }
}
