package org.bibleplugin.core.reference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;

/**
 * Parses human references such as {@code John 3:16}, {@code John:3:16-18}, {@code 1 Cor 13:4-7},
 * {@code Psalm 23} or {@code Genesis}.
 *
 * <p>Parsing is two-stage: this class splits the text into a book part and numeric parts,
 * then {@link #resolve(Bible, String)} binds the book part to a real book and validates the
 * numbers against the chosen Bible.
 */
public final class ReferenceParser {

    /**
     * Book, optional chapter, optional verse, optional verse end. Accepts {@code :} or whitespace
     * as the separator and {@code :} or {@code .} between chapter and verse.
     */
    private static final Pattern REFERENCE = Pattern.compile(
            "^((?:(?!:\\d).)+?)(?:[\\s:]+(\\d+)(?:[:.](\\d+)(?:\\s*[-–—]\\s*(\\d+))?)?)?$");

    private ReferenceParser() {
    }

    /** Syntactic result before the book name is resolved. Zero means "not given". */
    public record Parsed(String book, int chapter, int fromVerse, int toVerse) {
    }

    /**
     * @throws BibleException {@link Problem#INVALID_REFERENCE} when the text has no book part
     */
    public static Parsed parse(String input) {
        String text = input == null ? "" : input.strip();
        Matcher matcher = REFERENCE.matcher(text);
        if (text.isEmpty() || !matcher.matches()) {
            throw new BibleException(Problem.INVALID_REFERENCE, "Could not read the reference \"" + input + "\".");
        }
        return new Parsed(
                matcher.group(1).strip(),
                parseOrZero(matcher.group(2)),
                parseOrZero(matcher.group(3)),
                parseOrZero(matcher.group(4)));
    }

    /**
     * Parses and binds a reference to {@code bible}, validating that it exists.
     *
     * @throws BibleException with a specific problem for unknown books, bad numbers or out-of-range verses
     */
    public static Reference resolve(Bible bible, String input) {
        Parsed parsed = parse(input);
        int book = new BookResolver(bible).resolve(parsed.book());
        Reference reference;
        if (parsed.chapter() == 0) {
            reference = Reference.ofBook(book);
        } else if (parsed.fromVerse() == 0) {
            reference = Reference.ofChapter(book, parsed.chapter());
        } else {
            if (parsed.toVerse() != 0 && parsed.toVerse() < parsed.fromVerse()) {
                throw new BibleException(Problem.INVALID_REFERENCE, "The verse range " + input + " is reversed.");
            }
            reference = Reference.ofVerses(book, parsed.chapter(), parsed.fromVerse(), parsed.toVerse());
        }
        bible.validate(reference);
        return reference;
    }

    private static int parseOrZero(String digits) {
        if (digits == null) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException overflow) {
            throw new BibleException(Problem.INVALID_REFERENCE, "The number " + digits + " is too large.", overflow);
        }
    }
}
