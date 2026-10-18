package org.bibleplugin.core.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.bibleplugin.core.TestBibles;
import org.bibleplugin.core.model.Reference;

class BookPaginatorTest {

    @Test
    void neverExceedsThePageSize() {
        String text = "lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor";
        BookPaginator.Pages pages = BookPaginator.paginate(List.of(text, text, text), 30, 50);
        assertFalse(pages.truncated());
        for (String page : pages.pages()) {
            assertTrue(page.length() <= 30, "page too long: '" + page + "'");
        }
        assertTrue(pages.pages().size() > 3);
    }

    @Test
    void keepsWordsWholeWhereItCan() {
        BookPaginator.Pages pages = BookPaginator.paginate(List.of("alpha beta gamma delta"), 11, 10);
        assertEquals(List.of("alpha beta", "gamma delta"), pages.pages());
    }

    @Test
    void hardSplitsWordsLongerThanAPage() {
        BookPaginator.Pages pages = BookPaginator.paginate(List.of("abcdefghijklmnopqrstuvwxyz"), 16, 10);
        assertEquals(List.of("abcdefghijklmnop", "qrstuvwxyz"), pages.pages());
    }

    @Test
    void stopsAtTheMaximumNumberOfPages() {
        BookPaginator.Pages pages = BookPaginator.paginate(List.of("one two three four five six"), 8, 2);
        assertEquals(2, pages.pages().size());
        assertTrue(pages.truncated());
    }

    @Test
    void paragraphsStartOnTheirOwnLine() {
        BookPaginator.Pages pages = BookPaginator.paginate(List.of("First line", "Second line"), 40, 5);
        assertEquals(List.of("First line\nSecond line"), pages.pages());
    }

    @Test
    void composesAWholeChapterWithHeadingsAndVerseNumbers() {
        TemporaryBookComposer.Layout layout = TemporaryBookComposer.Layout.standard();
        TemporaryBook book = TemporaryBookComposer.compose(TestBibles.sample(), Reference.ofChapter(2, 1), "John", layout);
        assertEquals("John 1", book.title());
        assertEquals("test", book.bibleId());
        String joined = String.join("\n", book.pages());
        assertTrue(joined.contains("John 1"));
        assertTrue(joined.contains("1 In the beginning was the Word"));
        assertTrue(joined.contains("3 All things were made by him"));
        assertFalse(book.truncated());
    }

    @Test
    void longTitlesAreShortened() {
        String longName = "A Very Long Book Name That Goes Past The Limit";
        TemporaryBook book = TemporaryBookComposer.compose(TestBibles.sample(), Reference.ofBook(4), longName,
                TemporaryBookComposer.Layout.standard());
        assertTrue(book.title().length() <= TemporaryBookComposer.TITLE_LIMIT);
        assertThrows(IllegalArgumentException.class, () -> new TemporaryBookComposer.Layout(4, 1));
    }
}
