package org.bibleplugin.core.search;

/**
 * Fallback for queries whose tokens are not in the index, typically scripts without spaces
 * (Chinese, Japanese, Thai). Matches substrings of the normalised verse, so it is slower but
 * correct where tokenisation is meaningless.
 */
final class ScanStrategy implements SearchStrategy {

    @Override
    public int[] candidates(VerseIndex index, SearchQuery query, int from, int to) {
        int[] all = new int[Math.max(0, to - from)];
        for (int i = 0; i < all.length; i++) {
            all[i] = from + i;
        }
        return all;
    }

    @Override
    public int occurrences(String padded, SearchQuery query) {
        return SearchStrategy.countOccurrences(padded, query.normalized());
    }
}
