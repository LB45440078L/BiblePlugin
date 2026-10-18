package org.bibleplugin.core.model;

import java.util.List;
import java.util.Objects;

/**
 * One book of a Bible. Immutable. Chapter offsets are precomputed so that verse lookups by
 * global ordinal stay O(log n).
 */
public final class BibleBook {

    private final int number;
    private final String name;
    private final List<BibleChapter> chapters;
    private final int[] chapterOffsets;

    /**
     * @param number   book number, starting at 1 and sequential within a {@link Bible}
     * @param name     display name, e.g. {@code Genesis}
     * @param chapters chapters in order, chapter {@code n} at index {@code n - 1}
     */
    public BibleBook(int number, String name, List<BibleChapter> chapters) {
        if (number < 1) {
            throw new IllegalArgumentException("book number must be >= 1: " + number);
        }
        this.number = number;
        this.name = Objects.requireNonNull(name, "name").strip();
        this.chapters = List.copyOf(chapters);
        this.chapterOffsets = new int[this.chapters.size() + 1];
        int running = 0;
        for (int i = 0; i < this.chapters.size(); i++) {
            chapterOffsets[i] = running;
            running += this.chapters.get(i).verseCount();
        }
        chapterOffsets[this.chapters.size()] = running;
    }

    public int number() {
        return number;
    }

    public String name() {
        return name;
    }

    public List<BibleChapter> chapters() {
        return chapters;
    }

    public int chapterCount() {
        return chapters.size();
    }

    public int verseCount() {
        return chapterOffsets[chapters.size()];
    }

    /** @return the chapter with the given 1-based number, or {@code null}. */
    public BibleChapter chapter(int chapterNumber) {
        if (chapterNumber < 1 || chapterNumber > chapters.size()) {
            return null;
        }
        return chapters.get(chapterNumber - 1);
    }

    /** @return the number of verses that precede the first verse of {@code chapterNumber} in this book. */
    int chapterOffset(int chapterNumber) {
        return chapterOffsets[chapterNumber - 1];
    }

    /**
     * Finds the chapter that contains a book-local verse ordinal (0-based).
     *
     * @return the 1-based chapter number
     */
    int chapterForOrdinal(int localOrdinal) {
        int lo = 0;
        int hi = chapters.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (chapterOffsets[mid] <= localOrdinal) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo + 1;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BibleBook book && book.number == number && book.name.equals(name)
                && book.chapters.equals(chapters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, name);
    }

    @Override
    public String toString() {
        return "BibleBook[" + number + ", " + name + ", chapters=" + chapters.size() + "]";
    }
}
