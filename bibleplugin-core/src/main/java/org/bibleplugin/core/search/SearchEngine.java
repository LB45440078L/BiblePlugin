package org.bibleplugin.core.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;

/**
 * Searches a Bible by word, phrase or substring, optionally limited to one book or chapter.
 *
 * <p>The inverted index is built lazily the first time a Bible is searched and then cached.
 * It is keyed weakly, so unloading a Bible releases its index. Instances are thread-safe.
 */
public final class SearchEngine {

    /** Verses matched, total occurrences, and the first {@code limit} hits in canonical order. */
    public record Result(String query, long matchingVerses, long occurrences, List<Hit> hits) {
        public Result {
            hits = List.copyOf(hits);
        }
    }

    /** One matching verse. */
    public record Hit(Reference reference, String text) {
    }

    private static final SearchStrategy INDEXED = new IndexedStrategy();
    private static final SearchStrategy SCAN = new ScanStrategy();

    private final Map<Bible, VerseIndex> indexes = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * @param bible the Bible to search
     * @param scope limit the search to this book or chapter; empty for the whole Bible
     * @param query the words or phrase
     * @param limit maximum number of hits to materialise (occurrences are always counted in full)
     */
    public Result search(Bible bible, Optional<Reference> scope, String query, int limit) {
        SearchQuery parsed = SearchQuery.of(query);
        VerseIndex index = indexFor(bible);
        SearchStrategy strategy = strategyFor(index, parsed);
        int[] range = rangeOf(bible, scope);

        int[] candidates = strategy.candidates(index, parsed, range[0], range[1]);
        long verses = 0;
        long occurrences = 0;
        List<Hit> hits = new ArrayList<>(Math.min(limit, 64));
        for (int ordinal : candidates) {
            int count = strategy.occurrences(index.padded(ordinal), parsed);
            if (count == 0) {
                continue;
            }
            verses++;
            occurrences += count;
            if (hits.size() < limit) {
                Reference reference = bible.reference(ordinal);
                hits.add(new Hit(reference, bible.verseText(reference)));
            }
        }
        return new Result(query, verses, occurrences, hits);
    }

    /**
     * @return the number of times {@code query} occurs in the scope, counting every occurrence.
     */
    public long countOccurrences(Bible bible, Optional<Reference> scope, String query) {
        SearchQuery parsed = SearchQuery.of(query);
        VerseIndex index = indexFor(bible);
        SearchStrategy strategy = strategyFor(index, parsed);
        int[] range = rangeOf(bible, scope);
        long total = 0;
        for (int ordinal : strategy.candidates(index, parsed, range[0], range[1])) {
            total += strategy.occurrences(index.padded(ordinal), parsed);
        }
        return total;
    }

    /** Drops the cached index of {@code bible}, e.g. after the Bible was replaced. */
    public void invalidate(Bible bible) {
        indexes.remove(bible);
    }

    private VerseIndex indexFor(Bible bible) {
        synchronized (indexes) {
            return indexes.computeIfAbsent(bible, VerseIndex::build);
        }
    }

    private static SearchStrategy strategyFor(VerseIndex index, SearchQuery query) {
        for (String token : query.tokens()) {
            if (index.postings(token) == null) {
                return SCAN;
            }
        }
        return INDEXED;
    }

    private static int[] rangeOf(Bible bible, Optional<Reference> scope) {
        if (scope.isEmpty()) {
            return new int[] {0, bible.verseCount()};
        }
        var range = bible.ordinalRange(scope.get());
        return new int[] {range.from(), range.to()};
    }
}
