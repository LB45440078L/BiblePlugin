package org.bibleplugin.core.readability;

/**
 * Reading statistics for a text.
 *
 * @param readingEase Flesch Reading Ease (higher is easier, roughly 0-100)
 * @param gradeLevel  Flesch-Kincaid grade level (US school grade)
 * @param difficulty  the band derived from {@code gradeLevel}
 */
public record Readability(double readingEase, double gradeLevel, Difficulty difficulty) {

    public static Readability unrated() {
        return new Readability(Double.NaN, Double.NaN, Difficulty.UNRATED);
    }
}
