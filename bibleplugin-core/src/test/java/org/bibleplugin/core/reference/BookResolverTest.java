package org.bibleplugin.core.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.bibleplugin.core.TestBibles;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;

class BookResolverTest {

    private final BookResolver resolver = new BookResolver(TestBibles.sample());

    @Test
    void resolvesNumbersExactNamesAndPrefixes() {
        assertEquals(1, resolver.resolve("1"));
        assertEquals(2, resolver.resolve("John"));
        assertEquals(2, resolver.resolve("joh"));
        assertEquals(3, resolver.resolve("1 John"));
        assertEquals(3, resolver.resolve("1john"));
        assertEquals(3, resolver.resolve("I John"));
        assertEquals(4, resolver.resolve("psalm"));
    }

    @Test
    void ignoresAccentsAndCase() {
        assertEquals(5, resolver.resolve("SENOR notes"));
    }

    @Test
    void rejectsAmbiguousPrefixesListingTheCandidates() {
        BibleException error = assertThrows(BibleException.class, () -> resolver.resolve("j"));
        assertEquals(Problem.BOOK_AMBIGUOUS, error.problem());
        assertTrue(error.getMessage().contains("John"));
        assertTrue(error.getMessage().contains("Jonah"));
        assertEquals(6, resolver.resolve("jona"));
    }

    @Test
    void rejectsUnknownAndEmptyBooks() {
        assertEquals(Problem.BOOK_NOT_FOUND,
                assertThrows(BibleException.class, () -> resolver.resolve("Zephaniah")).problem());
        assertEquals(Problem.EMPTY_QUERY,
                assertThrows(BibleException.class, () -> resolver.resolve("  ")).problem());
        assertEquals(Problem.BOOK_NOT_FOUND,
                assertThrows(BibleException.class, () -> resolver.resolve("99")).problem());
    }
}
