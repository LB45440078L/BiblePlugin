package org.bibleplugin.core.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.bibleplugin.core.TestBibles;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;

class ReferenceParserTest {

    private final Bible bible = TestBibles.sample();

    @ParameterizedTest(name = "{0} -> book={1} chapter={2} from={3} to={4}")
    @CsvSource({
            "John:1:2,         John, 1, 2, 0",
            "John 1:2-3,       John, 1, 2, 3",
            "1 John 1:1,       1 John, 1, 1, 0",
            "Genesis,          Genesis, 0, 0, 0",
            "Psalm 23,         Psalm, 23, 0, 0",
            "Psalms:23:1–2, Psalms, 23, 1, 2",
            "Genesis 1.3,      Genesis, 1, 3, 0",
    })
    void parsesEveryAcceptedShape(String input, String book, int chapter, int from, int to) {
        ReferenceParser.Parsed parsed = ReferenceParser.parse(input);
        assertEquals(book.trim(), parsed.book());
        assertEquals(chapter, parsed.chapter());
        assertEquals(from, parsed.fromVerse());
        assertEquals(to, parsed.toVerse());
    }

    @Test
    void resolvesAgainstTheBible() {
        assertEquals(Reference.ofVerses(2, 1, 1, 3), ReferenceParser.resolve(bible, "john 1:1-3"));
        assertEquals(Reference.ofVerse(3, 1, 2), ReferenceParser.resolve(bible, "1 jo 1:2"));
        assertEquals(Reference.ofBook(4), ReferenceParser.resolve(bible, "psalm"));
    }

    @Test
    void reportsReversedRanges() {
        BibleException error = assertThrows(BibleException.class,
                () -> ReferenceParser.resolve(bible, "John 1:3-1"));
        assertEquals(Problem.INVALID_REFERENCE, error.problem());
    }

    @Test
    void reportsEmptyAndUnreadableInput() {
        assertEquals(Problem.INVALID_REFERENCE,
                assertThrows(BibleException.class, () -> ReferenceParser.parse("   ")).problem());
        assertEquals(Problem.INVALID_REFERENCE,
                assertThrows(BibleException.class, () -> ReferenceParser.parse("Exodus:6:15:4")).problem());
        assertEquals(Problem.INVALID_REFERENCE,
                assertThrows(BibleException.class, () -> ReferenceParser.resolve(bible, "Psalms:23:1:2")).problem());
    }

    @Test
    void reportsOutOfRangeVerses() {
        BibleException error = assertThrows(BibleException.class,
                () -> ReferenceParser.resolve(bible, "John 1:9"));
        assertEquals(Problem.VERSE_OUT_OF_RANGE, error.problem());
    }
}
