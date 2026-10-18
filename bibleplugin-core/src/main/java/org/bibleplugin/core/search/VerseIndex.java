package org.bibleplugin.core.search;

import java.util.HashMap;
import java.util.Map;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.util.IntList;

/**
 * Inverted index over one Bible, built once and reused by every search.
 *
 * <ul>
 *   <li>{@link #padded(int)} holds each verse normalised and wrapped in spaces, so that
 *       whole-word and whole-phrase checks are a plain {@code indexOf} on {@code " word "}.</li>
 *   <li>{@link #postings(String)} maps each token to the sorted ordinals of verses containing it.</li>
 * </ul>
 */
final class VerseIndex {

    private final String[] padded;
    private final Map<String, int[]> postings;

    private VerseIndex(String[] padded, Map<String, int[]> postings) {
        this.padded = padded;
        this.postings = postings;
    }

    static VerseIndex build(Bible bible) {
        String[] padded = new String[bible.verseCount()];
        Map<String, IntList> lists = new HashMap<>();
        int ordinal = 0;
        for (BibleBook book : bible.books()) {
            for (BibleChapter chapter : book.chapters()) {
                for (String verse : chapter.verses()) {
                    String normalized = TextNormalizer.normalize(verse);
                    padded[ordinal] = " " + normalized + " ";
                    if (!normalized.isEmpty()) {
                        for (String token : normalized.split(" ")) {
                            IntList list = lists.computeIfAbsent(token, key -> new IntList());
                            if (list.lastOrDefault(-1) != ordinal) {
                                list.add(ordinal);
                            }
                        }
                    }
                    ordinal++;
                }
            }
        }
        Map<String, int[]> frozen = new HashMap<>(lists.size() * 2);
        lists.forEach((token, list) -> frozen.put(token, list.toArray()));
        return new VerseIndex(padded, Map.copyOf(frozen));
    }

    String padded(int ordinal) {
        return padded[ordinal];
    }

    int verseCount() {
        return padded.length;
    }

    /** @return sorted verse ordinals containing {@code token}, or {@code null} when none do. */
    int[] postings(String token) {
        return postings.get(token);
    }
}
