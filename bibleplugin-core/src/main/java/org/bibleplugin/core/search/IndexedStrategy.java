package org.bibleplugin.core.search;

import java.util.Arrays;

/**
 * Uses the inverted index: candidates come from the postings of the query tokens, intersected
 * for phrases, so most verses are never touched.
 */
final class IndexedStrategy implements SearchStrategy {

    @Override
    public int[] candidates(VerseIndex index, SearchQuery query, int from, int to) {
        int[] result = null;
        for (String token : query.tokens()) {
            int[] postings = index.postings(token);
            if (postings == null) {
                return new int[0];
            }
            int[] inRange = slice(postings, from, to);
            result = result == null ? inRange : intersect(result, inRange);
            if (result.length == 0) {
                break;
            }
        }
        return result == null ? new int[0] : result;
    }

    @Override
    public int occurrences(String padded, SearchQuery query) {
        return SearchStrategy.countOccurrences(padded, query.wordNeedle());
    }

    /** @return the sub-array of sorted {@code values} with entries in {@code [from, to)}. */
    static int[] slice(int[] values, int from, int to) {
        int start = lowerBound(values, from);
        int end = lowerBound(values, to);
        return Arrays.copyOfRange(values, start, end);
    }

    static int[] intersect(int[] a, int[] b) {
        int[] out = new int[Math.min(a.length, b.length)];
        int i = 0;
        int j = 0;
        int n = 0;
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                out[n++] = a[i];
                i++;
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }
        }
        return Arrays.copyOf(out, n);
    }

    private static int lowerBound(int[] values, int key) {
        int lo = 0;
        int hi = values.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (values[mid] < key) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }
}
