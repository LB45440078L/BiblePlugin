package org.bibleplugin.persistence.binary;

import java.util.Objects;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.readability.Readability;

/**
 * A Bible together with its precomputed reading statistics, which is what gets cached on disk so
 * that loading never has to re-measure the text.
 *
 * @param bible       the Bible
 * @param readability reading difficulty of the text
 */
public record StoredBible(Bible bible, Readability readability) {

    public StoredBible {
        Objects.requireNonNull(bible, "bible");
        Objects.requireNonNull(readability, "readability");
    }
}
