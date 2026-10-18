package org.bibleplugin.core.book;

import java.util.ArrayList;
import java.util.List;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.core.model.VerseText;

/**
 * Turns a reference into a readable, paginated book. Each chapter gets a heading and verses are
 * prefixed by their number, e.g. {@code 16 For God so loved the world...}.
 */
public final class TemporaryBookComposer {

    /** Written books cap titles at 32 characters. */
    public static final int TITLE_LIMIT = 32;

    /**
     * @param charsPerPage characters per page
     * @param maxPages     maximum pages in the book
     */
    public record Layout(int charsPerPage, int maxPages) {
        public Layout {
            if (charsPerPage < 16 || maxPages < 1) {
                throw new IllegalArgumentException("invalid layout");
            }
        }

        public static Layout standard() {
            return new Layout(240, 100);
        }
    }

    private TemporaryBookComposer() {
    }

    /**
     * @param bible       the source Bible
     * @param reference   the passage to write out
     * @param bookName    the book's display name, used for headings and the title
     * @param layout      page size limits
     */
    public static TemporaryBook compose(Bible bible, Reference reference, String bookName, Layout layout) {
        List<VerseText> verses = bible.passage(reference);
        List<String> paragraphs = new ArrayList<>();
        int currentChapter = -1;
        for (VerseText verse : verses) {
            if (verse.reference().chapter() != currentChapter) {
                if (currentChapter != -1) {
                    paragraphs.add("");
                }
                currentChapter = verse.reference().chapter();
                paragraphs.add(bookName + " " + currentChapter);
            }
            paragraphs.add(verse.reference().fromVerse() + " " + verse.text());
        }
        BookPaginator.Pages pages = BookPaginator.paginate(paragraphs, layout.charsPerPage(), layout.maxPages());
        return new TemporaryBook(title(reference.format(bookName)), bible.id(), reference, pages.pages(), pages.truncated());
    }

    /** Shortens a title to the written-book limit. */
    static String title(String formatted) {
        if (formatted.length() <= TITLE_LIMIT) {
            return formatted;
        }
        return formatted.substring(0, TITLE_LIMIT - 3) + "...";
    }
}
