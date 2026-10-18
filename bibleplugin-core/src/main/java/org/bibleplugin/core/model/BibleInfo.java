package org.bibleplugin.core.model;

import java.util.Objects;

/**
 * Descriptive metadata about a Bible edition. Immutable.
 *
 * @param id           stable lower-case identifier, e.g. {@code kjv}
 * @param name         human readable name, e.g. {@code King James Version}
 * @param languageCode BCP-47-ish language code, e.g. {@code en}
 * @param language     language name in English, e.g. {@code English}
 * @param license      license / distribution note, e.g. {@code Public Domain}
 */
public record BibleInfo(String id, String name, String languageCode, String language, String license) {

    public BibleInfo {
        id = requireText(id, "id").toLowerCase(java.util.Locale.ROOT);
        name = requireText(name, "name");
        languageCode = requireText(languageCode, "languageCode");
        language = requireText(language, "language");
        license = license == null || license.isBlank() ? "Unknown" : license.strip();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }
}
