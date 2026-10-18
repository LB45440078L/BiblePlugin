package org.bibleplugin.core.exception;

import java.util.Objects;

/**
 * Domain error raised by the core. Every failure carries a {@link Problem} so that
 * presentation layers can pick a localised message without parsing text.
 */
public final class BibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Machine-readable reason for a failure. */
    public enum Problem {
        BIBLE_NOT_FOUND,
        BOOK_NOT_FOUND,
        BOOK_AMBIGUOUS,
        INVALID_REFERENCE,
        CHAPTER_OUT_OF_RANGE,
        VERSE_OUT_OF_RANGE,
        EMPTY_QUERY,
        FORMAT_ERROR,
        STORAGE_ERROR,
        DOWNLOAD_FAILED,
        UNKNOWN_SOURCE
    }

    private final Problem problem;

    public BibleException(Problem problem, String message) {
        super(message);
        this.problem = Objects.requireNonNull(problem, "problem");
    }

    public BibleException(Problem problem, String message, Throwable cause) {
        super(message, cause);
        this.problem = Objects.requireNonNull(problem, "problem");
    }

    public Problem problem() {
        return problem;
    }
}
