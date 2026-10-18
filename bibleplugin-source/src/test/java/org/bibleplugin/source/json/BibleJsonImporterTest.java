package org.bibleplugin.source.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleInfo;
import org.bibleplugin.core.model.Reference;

class BibleJsonImporterTest {

    private static final BibleInfo INFO = new BibleInfo("test", "Test", "en", "English", "Public Domain");

    private static final String GETBIBLE = """
            {
              "translation": "Test",
              "abbreviation": "test",
              "lang": "en",
              "books": [
                { "nr": 1, "name": "Genesis", "chapters": [
                    { "chapter": 1, "name": "Genesis 1", "verses": [
                        { "chapter": 1, "verse": 1, "name": "Genesis 1:1", "text": "In the beginning God created the heaven and the earth." },
                        { "chapter": 1, "verse": 2, "name": "Genesis 1:2", "text": "  And the earth was <i>without</i>   form. " }
                    ] } ] },
                { "nr": 43, "name": "John", "chapters": [
                    { "chapter": 3, "name": "John 3", "verses": [
                        { "chapter": 3, "verse": 1, "name": "John 3:1", "text": "For God so loved the world." }
                    ] } ] }
              ]
            }
            """;

    private static final String ARRAY = """
            [
              { "name": "Genesis", "abbrev": "gn", "chapters": [["In the beginning.", "And the earth."], ["Second chapter."]] },
              { "name": "Exodus", "abbrev": "ex", "chapters": [["Now these are the names."]] }
            ]
            """;

    @Test
    void readsTheGetBibleLayout() {
        Bible bible = BibleJsonImporter.read(stream(GETBIBLE), INFO);
        assertEquals(2, bible.bookCount());
        assertEquals("Genesis", bible.books().get(0).name());
        assertEquals(2, bible.books().get(0).chapter(1).verseCount());
        assertEquals("And the earth was without form.", bible.verseText(Reference.ofVerse(1, 1, 2)));
        assertEquals("For God so loved the world.", bible.verseText(Reference.ofVerse(2, 1, 1)));
    }

    @Test
    void numbersBooksByPositionEvenWhenNrIsDifferent() {
        Bible bible = BibleJsonImporter.read(stream(GETBIBLE), INFO);
        assertEquals(2, bible.book(2).orElseThrow().number());
    }

    @Test
    void readsTheArrayLayout() {
        Bible bible = BibleJsonImporter.read(stream(ARRAY), INFO);
        assertEquals(2, bible.bookCount());
        assertEquals(2, bible.books().get(0).chapterCount());
        assertEquals("Second chapter.", bible.verseText(Reference.ofVerse(1, 2, 1)));
        assertEquals("Exodus", bible.books().get(1).name());
    }

    @Test
    void skipsByteOrderMarksAndLeadingWhitespace() {
        Bible bible = BibleJsonImporter.read(stream("﻿   \n" + ARRAY), INFO);
        assertEquals(2, bible.bookCount());
    }

    @Test
    void rejectsUnknownLayouts() {
        BibleException error = assertThrows(BibleException.class,
                () -> BibleJsonImporter.read(stream("<xml/>"), INFO));
        assertEquals(Problem.FORMAT_ERROR, error.problem());
    }

    @Test
    void rejectsEmptyFiles() {
        BibleException error = assertThrows(BibleException.class,
                () -> BibleJsonImporter.read(stream("   "), INFO));
        assertEquals(Problem.FORMAT_ERROR, error.problem());
    }

    @Test
    void cleansTagsAndWhitespace() {
        assertEquals("a 1 b", BibleJsonImporter.cleanVerse("  a   <S>1</S>  b "));
        assertEquals("", BibleJsonImporter.cleanVerse(null));
    }

    private static ByteArrayInputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
