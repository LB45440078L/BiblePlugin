package org.bibleplugin.persistence.store;

import java.util.List;
import java.util.Optional;
import org.bibleplugin.core.model.Bookmark;
import org.bibleplugin.core.model.Reference;

/**
 * Everything the plugin remembers about one player. Immutable; changes produce a new record.
 *
 * @param preferredBible the Bible used when a command omits one, or {@code null} for the server default
 * @param loginVerse     whether a verse is shown when the player joins
 * @param lastBibleId    the Bible the player last read
 * @param lastReference  where the player stopped reading, or {@code null}
 * @param bookmarks      saved positions, newest first
 */
public record PlayerRecord(
        String preferredBible,
        boolean loginVerse,
        String lastBibleId,
        Reference lastReference,
        List<Bookmark> bookmarks) {

    /** Maximum bookmarks kept per player. */
    public static final int MAX_BOOKMARKS = 50;

    public PlayerRecord {
        bookmarks = List.copyOf(bookmarks);
    }

    public static PlayerRecord defaults() {
        return new PlayerRecord(null, true, null, null, List.of());
    }

    public Optional<String> preferred() {
        return Optional.ofNullable(preferredBible);
    }

    public PlayerRecord withPreferredBible(String id) {
        return new PlayerRecord(id, loginVerse, lastBibleId, lastReference, bookmarks);
    }

    public PlayerRecord withLoginVerse(boolean enabled) {
        return new PlayerRecord(preferredBible, enabled, lastBibleId, lastReference, bookmarks);
    }

    public PlayerRecord withLastPosition(String bibleId, Reference reference) {
        return new PlayerRecord(preferredBible, loginVerse, bibleId, reference, bookmarks);
    }

    /** Adds a bookmark at the front, dropping the oldest once {@link #MAX_BOOKMARKS} is reached. */
    public PlayerRecord withBookmark(Bookmark bookmark) {
        List<Bookmark> updated = new java.util.ArrayList<>(bookmarks.size() + 1);
        updated.add(bookmark);
        updated.addAll(bookmarks);
        if (updated.size() > MAX_BOOKMARKS) {
            updated = updated.subList(0, MAX_BOOKMARKS);
        }
        return new PlayerRecord(preferredBible, loginVerse, lastBibleId, lastReference, updated);
    }

    public PlayerRecord withoutBookmark(int index) {
        List<Bookmark> updated = new java.util.ArrayList<>(bookmarks);
        if (index >= 0 && index < updated.size()) {
            updated.remove(index);
        }
        return new PlayerRecord(preferredBible, loginVerse, lastBibleId, lastReference, updated);
    }
}
