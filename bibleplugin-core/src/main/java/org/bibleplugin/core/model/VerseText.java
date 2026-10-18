package org.bibleplugin.core.model;

/**
 * A single verse together with where it lives.
 *
 * @param reference the exact verse reference (book, chapter and verse)
 * @param text      the verse text as stored in the Bible
 */
public record VerseText(Reference reference, String text) {
}
