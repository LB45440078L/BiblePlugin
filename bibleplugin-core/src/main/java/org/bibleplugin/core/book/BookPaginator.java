package org.bibleplugin.core.book;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out paragraphs into fixed-size pages without splitting words where avoidable.
 * Paragraph breaks are kept as line breaks. Words longer than a page are hard-split.
 */
public final class BookPaginator {

    /** Pages produced, and whether text was dropped because {@code maxPages} was reached. */
    public record Pages(List<String> pages, boolean truncated) {
        public Pages {
            pages = List.copyOf(pages);
        }
    }

    private BookPaginator() {
    }

    /**
     * @param paragraphs   paragraphs in reading order; blank ones are skipped
     * @param charsPerPage maximum characters per page, at least 4
     * @param maxPages     maximum pages to return, at least 1
     */
    public static Pages paginate(List<String> paragraphs, int charsPerPage, int maxPages) {
        if (charsPerPage < 4 || maxPages < 1) {
            throw new IllegalArgumentException("charsPerPage must be >= 4 and maxPages >= 1");
        }
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (paragraph == null || paragraph.isBlank()) {
                continue;
            }
            for (String word : paragraph.strip().split("\\s+")) {
                for (String piece : hardSplit(word, charsPerPage)) {
                    boolean lineStart = page.isEmpty() || page.charAt(page.length() - 1) == '\n';
                    int separator = lineStart ? 0 : 1;
                    if (page.length() + separator + piece.length() > charsPerPage) {
                        pages.add(page.toString());
                        page.setLength(0);
                        if (pages.size() == maxPages) {
                            return new Pages(pages, true);
                        }
                    } else if (separator == 1) {
                        page.append(' ');
                    }
                    page.append(piece);
                }
            }
            if (!page.isEmpty()) {
                if (page.length() + 1 > charsPerPage) {
                    pages.add(page.toString());
                    page.setLength(0);
                    if (pages.size() == maxPages) {
                        return new Pages(pages, true);
                    }
                } else {
                    page.append('\n');
                }
            }
        }
        if (!page.isEmpty()) {
            pages.add(page.toString().stripTrailing());
        }
        return new Pages(pages, false);
    }

    private static List<String> hardSplit(String word, int width) {
        if (word.length() <= width) {
            return List.of(word);
        }
        List<String> pieces = new ArrayList<>();
        for (int start = 0; start < word.length(); start += width) {
            pieces.add(word.substring(start, Math.min(word.length(), start + width)));
        }
        return pieces;
    }
}
