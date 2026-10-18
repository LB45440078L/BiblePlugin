package org.bibleplugin.core.search;

/**
 * Strategy for finding and counting matches. Two implementations exist:
 * {@link IndexedStrategy} for space-delimited scripts and {@link ScanStrategy} for scripts such as
 * Chinese or Japanese where a token is a whole run of characters.
 */
interface SearchStrategy {

    /**
     * @return ascending ordinals in {@code [from, to)} that may contain the query
     */
    int[] candidates(VerseIndex index, SearchQuery query, int from, int to);

    /**
     * @return how many times the query occurs in one padded verse (0 when absent)
     */
    int occurrences(String padded, SearchQuery query);

    /** Counts non-overlapping-start occurrences of {@code needle} inside {@code haystack}. */
    static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + 1);
        }
        return count;
    }
}
