package org.bibleplugin.core.search;

import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;

/**
 * A user search, normalised once up front.
 *
 * @param raw        what the user typed
 * @param normalized the {@link TextNormalizer} form
 * @param tokens     the space-separated tokens of {@code normalized}
 */
record SearchQuery(String raw, String normalized, String[] tokens) {

    static SearchQuery of(String raw) {
        String normalized = TextNormalizer.normalize(raw);
        if (normalized.isEmpty()) {
            throw new BibleException(Problem.EMPTY_QUERY, "Type at least one letter or number to search.");
        }
        return new SearchQuery(raw, normalized, normalized.split(" "));
    }

    /** The whole-word form, e.g. {@code " love "}. */
    String wordNeedle() {
        return " " + normalized + " ";
    }

    boolean isSingleToken() {
        return tokens.length == 1;
    }
}
