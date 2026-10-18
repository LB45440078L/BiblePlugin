package org.bibleplugin.core.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.bibleplugin.core.TestBibles;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.model.BibleInfo;
import org.bibleplugin.core.model.Reference;

class SearchEngineTest {

    private final Bible bible = TestBibles.sample();
    private final SearchEngine engine = new SearchEngine();

    @Test
    void countsWholeWordOccurrencesNotSubstrings() {
        // Six verses hold "God" (7 times): Gen 1:1, 1:3, 2:2, John 1:1, 1:2 and 1 John 1:2.
        // "godly" or "godfrey" would not count.
        SearchEngine.Result result = engine.search(bible, Optional.empty(), "god", 100);
        assertEquals(6, result.matchingVerses());
        assertEquals(7, result.occurrences());
        assertEquals(6, result.hits().size());
    }

    @Test
    void phraseMatchingRespectsWordBoundaries() {
        // "Love is patient; love is kind." has two; "God is love; ... in love" has two more.
        assertEquals(4, engine.countOccurrences(bible, Optional.empty(), "love"));
        assertEquals(3, engine.countOccurrences(bible, Optional.empty(), "in the beginning"));
        assertEquals(0, engine.countOccurrences(bible, Optional.empty(), "beginning with the"));
    }

    @Test
    void searchIsCaseAndAccentInsensitive() {
        assertEquals(1, engine.countOccurrences(bible, Optional.empty(), "SEÑOR"));
        assertEquals(1, engine.countOccurrences(bible, Optional.empty(), "senor"));
    }

    @Test
    void scopeLimitsResultsToBookOrChapter() {
        assertEquals(2, engine.countOccurrences(bible, Optional.of(Reference.ofBook(2)), "God"));
        assertEquals(2, engine.countOccurrences(bible, Optional.of(Reference.ofChapter(2, 2)), "love"));
        assertEquals(0, engine.countOccurrences(bible, Optional.of(Reference.ofChapter(4, 1)), "God"));
    }

    @Test
    void hitsAreInCanonicalOrderAndLimited() {
        SearchEngine.Result result = engine.search(bible, Optional.empty(), "the", 2);
        assertEquals(2, result.hits().size());
        assertTrue(result.matchingVerses() > 2);
        assertEquals(Reference.ofVerse(1, 1, 1), result.hits().get(0).reference());
    }

    @Test
    void fallsBackToSubstringMatchingForScriptsWithoutSpaces() {
        Bible cjk = new Bible(new BibleInfo("cjk", "CJK", "zh", "Chinese", "Public Domain"), java.util.List.of(
                new BibleBook(1, "创世记", java.util.List.of(new BibleChapter(1,
                        java.util.List.of("起初神创造天地。", "神说要有光，就有了光。"))))));
        assertEquals(2, engine.countOccurrences(cjk, Optional.empty(), "神"));
        assertEquals(2, engine.countOccurrences(cjk, Optional.empty(), "光"));
    }

    @Test
    void emptyQueriesAreRejected() {
        BibleException error = assertThrows(BibleException.class,
                () -> engine.search(bible, Optional.empty(), "  ... ", 5));
        assertEquals(Problem.EMPTY_QUERY, error.problem());
    }

    @Test
    void unknownWordsHaveNoHits() {
        SearchEngine.Result result = engine.search(bible, Optional.empty(), "zebra", 5);
        assertFalse(result.hits().size() > 0);
        assertEquals(0, result.occurrences());
    }
}
