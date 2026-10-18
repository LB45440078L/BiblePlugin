package org.bibleplugin.core.model;

import java.util.List;
import java.util.Objects;

/**
 * One chapter of a book. Verses are stored in order; verse {@code n} lives at index {@code n - 1}.
 *
 * @param number chapter number, starting at 1
 * @param verses verse texts, never null entries (empty string when a verse is missing)
 */
public record BibleChapter(int number, List<String> verses) {

    public BibleChapter {
        if (number < 1) {
            throw new IllegalArgumentException("chapter number must be >= 1: " + number);
        }
        verses = List.copyOf(Objects.requireNonNull(verses, "verses"));
    }

    public int verseCount() {
        return verses.size();
    }

    /** @return the text of verse {@code verseNumber} (1-based), or {@code null} when out of range. */
    public String verse(int verseNumber) {
        if (verseNumber < 1 || verseNumber > verses.size()) {
            return null;
        }
        return verses.get(verseNumber - 1);
    }
}
