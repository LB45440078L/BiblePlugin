package org.bibleplugin.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.bibleplugin.core.TestBibles;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;

class BibleTest {

    private final Bible bible = TestBibles.sample();

    @Test
    void ordinalsRoundTripThroughEveryVerse() {
        assertEquals(15, bible.verseCount());
        for (int ordinal = 0; ordinal < bible.verseCount(); ordinal++) {
            Reference reference = bible.reference(ordinal);
            assertEquals(ordinal, bible.ordinal(reference), "ordinal " + ordinal + " " + reference);
        }
    }

    @Test
    void firstVerseOfSecondBookIsOrdinalFive() {
        assertEquals(5, bible.ordinal(Reference.ofVerse(2, 1, 1)));
        assertEquals(Reference.ofVerse(2, 1, 1), bible.reference(5));
    }

    @Test
    void passageExpandsBooksChaptersAndRanges() {
        assertEquals(3, bible.passage(Reference.ofChapter(2, 1)).size());
        assertEquals(5, bible.passage(Reference.ofBook(2)).size());
        assertEquals(2, bible.passage(Reference.ofVerses(2, 1, 2, 3)).size());
        assertEquals("The same was in the beginning with God.", bible.verseText(Reference.ofVerse(2, 1, 2)));
    }

    @Test
    void rangesCoverTheRightOrdinals() {
        OrdinalRange john = bible.ordinalRange(Reference.ofBook(2));
        assertEquals(new OrdinalRange(5, 10), john);
        assertEquals(new OrdinalRange(5, 8), bible.ordinalRange(Reference.ofChapter(2, 1)));
        assertEquals(2, bible.ordinalRange(Reference.ofVerses(2, 1, 2, 3)).size());
    }

    @Test
    void validationReportsTheExactProblem() {
        BibleException chapter = assertThrows(BibleException.class,
                () -> bible.validate(Reference.ofChapter(1, 9)));
        assertEquals(Problem.CHAPTER_OUT_OF_RANGE, chapter.problem());

        BibleException verse = assertThrows(BibleException.class,
                () -> bible.validate(Reference.ofVerse(1, 2, 9)));
        assertEquals(Problem.VERSE_OUT_OF_RANGE, verse.problem());

        BibleException book = assertThrows(BibleException.class,
                () -> bible.validate(Reference.ofBook(42)));
        assertEquals(Problem.BOOK_NOT_FOUND, book.problem());
    }

    @Test
    void referenceNormalisesSingleVerses() {
        assertEquals(Reference.ofVerse(2, 1, 4), new Reference(2, 1, 4, 0));
        assertTrue(Reference.ofVerse(2, 1, 4).isVerseRange());
        assertEquals("John 1:4-6", Reference.ofVerses(2, 1, 4, 6).format("John"));
    }

    @Test
    void referenceRejectsImpossibleShapes() {
        assertThrows(IllegalArgumentException.class, () -> new Reference(1, 0, 3, 0));
        assertThrows(IllegalArgumentException.class, () -> new Reference(1, 1, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> Reference.ofVerses(1, 1, 5, 2));
    }

    @Test
    void bookNumbersMustBeSequential() {
        assertThrows(IllegalArgumentException.class, () -> new Bible(bible.info(),
                java.util.List.of(new BibleBook(2, "Only", java.util.List.of()))));
    }
}
