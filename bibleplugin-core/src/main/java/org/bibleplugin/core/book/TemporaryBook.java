package org.bibleplugin.core.book;

import java.util.List;
import org.bibleplugin.core.model.Reference;

/**
 * Pages of scripture ready to be written into a book item.
 *
 * @param title     at most {@link TemporaryBookComposer#TITLE_LIMIT} characters
 * @param bibleId   the Bible the text came from
 * @param reference what the book covers
 * @param pages     page texts, each within the requested page size
 * @param truncated true when the passage did not fit in the allowed number of pages
 */
public record TemporaryBook(String title, String bibleId, Reference reference, List<String> pages, boolean truncated) {

    public TemporaryBook {
        pages = List.copyOf(pages);
    }
}
