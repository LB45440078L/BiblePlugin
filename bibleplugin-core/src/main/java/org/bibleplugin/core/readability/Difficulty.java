package org.bibleplugin.core.readability;

/**
 * Reading difficulty bands derived from the Flesch-Kincaid grade level.
 * Thresholds: below 6.6 is {@link #EASY}, below 7.5 {@link #MODERATE}, below 8.5 {@link #ADVANCED},
 * and anything above is {@link #SCHOLARLY}.
 */
public enum Difficulty {
    EASY("Easy", "Plain modern English, quick to read."),
    MODERATE("Moderate", "Standard English with some older vocabulary."),
    ADVANCED("Advanced", "Longer sentences and archaic phrasing."),
    SCHOLARLY("Scholarly", "Dense, formal or archaic wording; best with study notes."),
    UNRATED("Unrated", "Not enough English text to measure.");

    private final String label;
    private final String description;

    Difficulty(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /** @return the band for a Flesch-Kincaid grade level. */
    public static Difficulty fromGrade(double gradeLevel) {
        if (Double.isNaN(gradeLevel)) {
            return UNRATED;
        }
        if (gradeLevel < 6.6) {
            return EASY;
        }
        if (gradeLevel < 7.5) {
            return MODERATE;
        }
        if (gradeLevel < 8.5) {
            return ADVANCED;
        }
        return SCHOLARLY;
    }
}
