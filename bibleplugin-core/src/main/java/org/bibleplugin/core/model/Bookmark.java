package org.bibleplugin.core.model;

import java.util.Objects;

/**
 * A saved reading position.
 *
 * @param label     user-facing label; defaults to the formatted reference
 * @param bibleId   the Bible the bookmark belongs to
 * @param reference where the bookmark points
 * @param createdAt creation time in epoch milliseconds
 */
public record Bookmark(String label, String bibleId, Reference reference, long createdAt) {

    public Bookmark {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(bibleId, "bibleId");
        Objects.requireNonNull(reference, "reference");
    }
}
