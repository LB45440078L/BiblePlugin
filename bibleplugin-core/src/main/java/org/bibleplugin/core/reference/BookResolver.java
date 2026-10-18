package org.bibleplugin.core.reference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.search.TextNormalizer;

/**
 * Resolves what a user typed into a book number. Accepts, in order of preference:
 * a book number, an exact name ({@code 1 John}, {@code 1john}, {@code I John}), or a unique
 * prefix ({@code gen} for Genesis). Ambiguous prefixes are rejected with the candidates listed.
 */
public final class BookResolver {

    private static final Pattern ROMAN_PREFIX = Pattern.compile("^(I{1,3})\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private final Bible bible;
    private final Map<String, Integer> keys;

    public BookResolver(Bible bible) {
        this.bible = bible;
        Map<String, Integer> built = new HashMap<>();
        for (BibleBook book : bible.books()) {
            built.putIfAbsent(keyOf(book.name()), book.number());
        }
        this.keys = Map.copyOf(built);
    }

    /**
     * @param query book number, name, or unique name prefix
     * @return the 1-based book number
     * @throws BibleException {@link Problem#BOOK_NOT_FOUND}, {@link Problem#BOOK_AMBIGUOUS} or {@link Problem#EMPTY_QUERY}
     */
    public int resolve(String query) {
        String raw = query == null ? "" : query.strip();
        if (raw.isEmpty()) {
            throw new BibleException(Problem.EMPTY_QUERY, "A book name is required.");
        }
        if (raw.chars().allMatch(Character::isDigit)) {
            int number = Integer.parseInt(raw);
            if (bible.book(number).isPresent()) {
                return number;
            }
            throw new BibleException(Problem.BOOK_NOT_FOUND, "No book number " + raw + " in " + bible.name() + ".");
        }

        String key = keyOf(expandRoman(raw));
        Integer exact = keys.get(key);
        if (exact != null) {
            return exact;
        }

        List<Map.Entry<String, Integer>> prefixed = keys.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(key))
                .sorted(Map.Entry.comparingByValue())
                .toList();
        if (prefixed.size() == 1) {
            return prefixed.get(0).getValue();
        }
        if (prefixed.isEmpty()) {
            throw new BibleException(Problem.BOOK_NOT_FOUND, "No book matches \"" + raw + "\".");
        }
        String candidates = prefixed.stream()
                .limit(6)
                .map(entry -> bible.book(entry.getValue()).map(BibleBook::name).orElse("?"))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        throw new BibleException(Problem.BOOK_AMBIGUOUS, "\"" + raw + "\" could be: " + candidates + ".");
    }

    /** Lower-case, accent-free, space-free form used as a lookup key. */
    static String keyOf(String text) {
        return TextNormalizer.normalize(text).replace(" ", "");
    }

    static String expandRoman(String raw) {
        Matcher matcher = ROMAN_PREFIX.matcher(raw);
        if (!matcher.matches()) {
            return raw;
        }
        return matcher.group(1).length() + " " + matcher.group(2);
    }
}
