package org.bibleplugin.core.search;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Canonical form used for every search comparison: decomposed, accent-free, lower-case,
 * with punctuation turned into single spaces. Keeps search case-insensitive and tolerant of
 * accents ({@code Señor} matches {@code senor}) while still respecting word boundaries.
 */
public final class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}]+");

    private TextNormalizer() {
    }

    /** @return the normalised text, never null, possibly empty. */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutMarks = DIACRITICS.matcher(decomposed).replaceAll("");
        return SEPARATORS.matcher(withoutMarks.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    }
}
