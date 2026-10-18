package org.bibleplugin.core.model;

/**
 * A pointer into a Bible at book, chapter or verse-range granularity.
 *
 * <p>Zero means "not specified": {@code chapter == 0} addresses a whole book, and
 * {@code fromVerse == 0} addresses a whole chapter. {@code toVerse == 0} is normalised to
 * {@code fromVerse}, so a single verse always has {@code fromVerse == toVerse}.
 *
 * @param book      1-based book number
 * @param chapter   1-based chapter, or 0 for the whole book
 * @param fromVerse first verse, or 0 for the whole chapter
 * @param toVerse   last verse (inclusive), or 0 when only {@code fromVerse} applies
 */
public record Reference(int book, int chapter, int fromVerse, int toVerse) {

    public Reference {
        if (book < 1) {
            throw new IllegalArgumentException("book must be >= 1");
        }
        if (chapter < 0 || fromVerse < 0 || toVerse < 0) {
            throw new IllegalArgumentException("reference parts must not be negative");
        }
        if (chapter == 0 && (fromVerse != 0 || toVerse != 0)) {
            throw new IllegalArgumentException("a verse needs a chapter");
        }
        if (fromVerse == 0 && toVerse != 0) {
            throw new IllegalArgumentException("a verse range needs a first verse");
        }
        if (fromVerse > 0 && toVerse == 0) {
            toVerse = fromVerse;
        }
        if (toVerse != 0 && toVerse < fromVerse) {
            throw new IllegalArgumentException("verse range is reversed");
        }
    }

    public static Reference ofBook(int book) {
        return new Reference(book, 0, 0, 0);
    }

    public static Reference ofChapter(int book, int chapter) {
        return new Reference(book, chapter, 0, 0);
    }

    public static Reference ofVerses(int book, int chapter, int fromVerse, int toVerse) {
        return new Reference(book, chapter, fromVerse, toVerse);
    }

    public static Reference ofVerse(int book, int chapter, int verse) {
        return new Reference(book, chapter, verse, verse);
    }

    public boolean isBook() {
        return chapter == 0;
    }

    public boolean isChapter() {
        return chapter > 0 && fromVerse == 0;
    }

    public boolean isVerseRange() {
        return fromVerse > 0;
    }

    /** @return the chapter-level reference that contains this reference. */
    public Reference toChapter() {
        return ofChapter(book, Math.max(chapter, 1));
    }

    /**
     * Formats the reference for display, e.g. {@code John 3:16-18}.
     *
     * @param bookName the display name of {@link #book()}
     */
    public String format(String bookName) {
        if (isBook()) {
            return bookName;
        }
        StringBuilder out = new StringBuilder(bookName).append(' ').append(chapter);
        if (fromVerse > 0) {
            out.append(':').append(fromVerse);
            if (toVerse > fromVerse) {
                out.append('-').append(toVerse);
            }
        }
        return out.toString();
    }
}
