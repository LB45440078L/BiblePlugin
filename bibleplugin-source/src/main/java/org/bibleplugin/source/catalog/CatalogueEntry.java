package org.bibleplugin.source.catalog;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import org.bibleplugin.core.model.BibleInfo;
import org.bibleplugin.core.readability.Readability;

/**
 * One downloadable Bible edition as listed in the catalogue.
 *
 * @param info        identity and metadata
 * @param year        year of the distribution, or empty when unknown
 * @param url         where the edition can be downloaded from
 * @param readability reading difficulty when it has been measured, otherwise empty
 */
public record CatalogueEntry(BibleInfo info, String year, URI url, Optional<Readability> readability) {

    public CatalogueEntry {
        Objects.requireNonNull(info, "info");
        year = year == null ? "" : year;
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(readability, "readability");
    }

    public String id() {
        return info.id();
    }

    public String name() {
        return info.name();
    }

    public String languageCode() {
        return info.languageCode();
    }

    public String language() {
        return info.language();
    }

    public String license() {
        return info.license();
    }
}
