package org.bibleplugin.persistence.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.model.BibleInfo;
import org.bibleplugin.core.model.Bookmark;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.core.readability.Readability;
import org.bibleplugin.persistence.binary.StoredBible;

class StoresTest {

    @TempDir
    Path temp;

    @Test
    void bibleStoreSavesLoadsListsAndDeletes() {
        BibleFileStore store = new BibleFileStore(temp.resolve("bibles"));
        store.save(new StoredBible(sample("kjv"), Readability.unrated()));
        store.save(new StoredBible(sample("web"), Readability.unrated()));

        assertTrue(store.exists("KJV"));
        assertEquals(List.of("kjv", "web"), store.installed());
        assertEquals("Sample", store.load("kjv").bible().name());

        assertTrue(store.delete("kjv"));
        assertFalse(store.exists("kjv"));
        assertFalse(store.delete("kjv"));
    }

    @Test
    void loadingAMissingBibleReportsItClearly() {
        BibleFileStore store = new BibleFileStore(temp.resolve("bibles"));
        BibleException error = assertThrows(BibleException.class, () -> store.load("niv"));
        assertEquals(Problem.BIBLE_NOT_FOUND, error.problem());
    }

    @Test
    void idsCannotEscapeTheDirectory() {
        BibleFileStore store = new BibleFileStore(temp.resolve("bibles"));
        assertThrows(BibleException.class, () -> store.exists("../secrets"));
        assertThrows(BibleException.class, () -> store.exists("a/b"));
    }

    @Test
    void playerDataPersistsAcrossInstances() {
        UUID player = UUID.randomUUID();
        Path directory = temp.resolve("players");
        PlayerDataStore first = new PlayerDataStore(directory);
        assertTrue(first.get(player).loginVerse());

        first.update(player, record -> record
                .withPreferredBible("web")
                .withLoginVerse(false)
                .withLastPosition("web", Reference.ofVerse(2, 3, 16))
                .withBookmark(new Bookmark("John 3:16", "web", Reference.ofVerse(2, 3, 16), 1L)));

        PlayerDataStore second = new PlayerDataStore(directory);
        var record = second.get(player);
        assertEquals("web", record.preferredBible());
        assertFalse(record.loginVerse());
        assertEquals(Reference.ofVerse(2, 3, 16), record.lastReference());
        assertEquals(1, record.bookmarks().size());
    }

    @Test
    void bookmarksAreCappedAndNewestFirst() {
        var record = org.bibleplugin.persistence.store.PlayerRecord.defaults();
        for (int i = 1; i <= PlayerRecord.MAX_BOOKMARKS + 5; i++) {
            record = record.withBookmark(new Bookmark("b" + i, "kjv", Reference.ofBook(1), i));
        }
        assertEquals(PlayerRecord.MAX_BOOKMARKS, record.bookmarks().size());
        assertEquals("b55", record.bookmarks().get(0).label());
    }

    private static Bible sample(String id) {
        return new Bible(new BibleInfo(id, "Sample", "en", "English", "Public Domain"),
                List.of(new BibleBook(1, "Genesis", List.of(new BibleChapter(1, List.of("In the beginning."))))));
    }
}
