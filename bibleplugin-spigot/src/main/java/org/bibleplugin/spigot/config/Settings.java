package org.bibleplugin.spigot.config;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed, immutable view of {@code config.yml}. Built with {@link Builder} so that tests and the
 * loader share one validated construction path; use {@link #from(FileConfiguration)} at runtime.
 */
public record Settings(
        String defaultBible,
        List<String> keepLoaded,
        Duration downloadTimeout,
        int downloadAttempts,
        int resultsPerPage,
        int maxSearchResults,
        InputMode inputMode,
        Duration temporaryBookLifetime,
        int bookCharsPerPage,
        int bookMaxPages,
        String bookAuthor,
        boolean soundsEnabled,
        float soundVolume,
        boolean loginVerse) {

    /** How the plugin asks a player for text. */
    public enum InputMode {
        /** An anvil with a rename box. Bukkit cannot reliably read the typed text, so this is optional. */
        ANVIL,
        /** A sign editor. Briefly places a real sign above the player. The default. */
        SIGN;

        public static InputMode parse(String value) {
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException unknown) {
                return SIGN;
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Settings from(FileConfiguration config) {
        return builder()
                .defaultBible(config.getString("bibles.default", "kjv"))
                .keepLoaded(config.getStringList("bibles.keep-loaded"))
                .downloadTimeout(Duration.ofSeconds(config.getInt("bibles.download.timeout-seconds", 30)))
                .downloadAttempts(config.getInt("bibles.download.attempts", 3))
                .resultsPerPage(config.getInt("search.results-per-page", 8))
                .maxSearchResults(config.getInt("search.max-results", 200))
                .inputMode(InputMode.parse(config.getString("reader.input", "SIGN")))
                .temporaryBookLifetime(Duration.ofMinutes(config.getLong("temporary-book.lifetime-minutes", 20)))
                .bookCharsPerPage(config.getInt("temporary-book.chars-per-page", 240))
                .bookMaxPages(config.getInt("temporary-book.max-pages", 100))
                .bookAuthor(config.getString("temporary-book.author", "BiblePlugin"))
                .soundsEnabled(config.getBoolean("sounds.enabled", true))
                .soundVolume((float) config.getDouble("sounds.volume", 0.7))
                .loginVerse(config.getBoolean("login-verse", true))
                .build();
    }

    public static final class Builder {
        private String defaultBible = "kjv";
        private List<String> keepLoaded = List.of("kjv");
        private Duration downloadTimeout = Duration.ofSeconds(30);
        private int downloadAttempts = 3;
        private int resultsPerPage = 8;
        private int maxSearchResults = 200;
        private InputMode inputMode = InputMode.SIGN;
        private Duration temporaryBookLifetime = Duration.ofMinutes(20);
        private int bookCharsPerPage = 240;
        private int bookMaxPages = 100;
        private String bookAuthor = "BiblePlugin";
        private boolean soundsEnabled = true;
        private float soundVolume = 0.7f;
        private boolean loginVerse = true;

        private Builder() {
        }

        public Builder defaultBible(String id) {
            this.defaultBible = id.toLowerCase(Locale.ROOT);
            return this;
        }

        public Builder keepLoaded(List<String> ids) {
            this.keepLoaded = ids.stream().map(id -> id.toLowerCase(Locale.ROOT)).toList();
            return this;
        }

        public Builder downloadTimeout(Duration timeout) {
            this.downloadTimeout = timeout;
            return this;
        }

        public Builder downloadAttempts(int attempts) {
            this.downloadAttempts = attempts;
            return this;
        }

        public Builder resultsPerPage(int size) {
            this.resultsPerPage = size;
            return this;
        }

        public Builder maxSearchResults(int max) {
            this.maxSearchResults = max;
            return this;
        }

        public Builder inputMode(InputMode mode) {
            this.inputMode = mode;
            return this;
        }

        public Builder temporaryBookLifetime(Duration lifetime) {
            this.temporaryBookLifetime = lifetime;
            return this;
        }

        public Builder bookCharsPerPage(int chars) {
            this.bookCharsPerPage = chars;
            return this;
        }

        public Builder bookMaxPages(int pages) {
            this.bookMaxPages = pages;
            return this;
        }

        public Builder bookAuthor(String author) {
            this.bookAuthor = author;
            return this;
        }

        public Builder soundsEnabled(boolean enabled) {
            this.soundsEnabled = enabled;
            return this;
        }

        public Builder soundVolume(float volume) {
            this.soundVolume = volume;
            return this;
        }

        public Builder loginVerse(boolean enabled) {
            this.loginVerse = enabled;
            return this;
        }

        /** @throws IllegalArgumentException when a value is out of its useful range */
        public Settings build() {
            require(resultsPerPage >= 1 && resultsPerPage <= 50, "search.results-per-page must be 1-50");
            require(maxSearchResults >= 1, "search.max-results must be at least 1");
            require(downloadAttempts >= 1 && downloadAttempts <= 10, "bibles.download.attempts must be 1-10");
            require(!downloadTimeout.isNegative() && !downloadTimeout.isZero(), "bibles.download.timeout-seconds must be positive");
            require(bookCharsPerPage >= 40 && bookCharsPerPage <= 512, "temporary-book.chars-per-page must be 40-512");
            require(bookMaxPages >= 1 && bookMaxPages <= 100, "temporary-book.max-pages must be 1-100");
            require(soundVolume >= 0f && soundVolume <= 1f, "sounds.volume must be between 0 and 1");
            return new Settings(defaultBible, keepLoaded, downloadTimeout, downloadAttempts, resultsPerPage,
                    maxSearchResults, inputMode, temporaryBookLifetime, bookCharsPerPage, bookMaxPages,
                    bookAuthor, soundsEnabled, soundVolume, loginVerse);
        }

        private static void require(boolean condition, String message) {
            if (!condition) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
