package org.bibleplugin.core.readability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.bibleplugin.core.TestBibles;

class ReadabilityAnalyzerTest {

    @ParameterizedTest(name = "{0} has {1} syllables")
    @CsvSource({"cat, 1", "table, 2", "make, 1", "beautiful, 3", "the, 1", "god, 1", "created, 2", "love, 1"})
    void countsSyllables(String word, int expected) {
        assertEquals(expected, ReadabilityAnalyzer.countSyllables(word));
    }

    @Test
    void simpleTextIsEasierThanArchaicText() {
        Readability simple = ReadabilityAnalyzer.analyze(List.of(
                "The dog ran. The cat sat. We had fun.",
                "I see a big red ball."));
        Readability archaic = ReadabilityAnalyzer.analyze(List.of(
                "And it came to pass, that when he had considered the matter, "
                        + "he went forth unto the place which was appointed for him of God."));
        assertTrue(simple.gradeLevel() < archaic.gradeLevel());
        assertTrue(simple.readingEase() > archaic.readingEase());
    }

    @Test
    void markupIsIgnored() {
        Readability plain = ReadabilityAnalyzer.analyze(List.of("In the beginning God created."));
        Readability tagged = ReadabilityAnalyzer.analyze(List.of("In the <S>7225</S> beginning {note} God created."));
        assertEquals(plain.gradeLevel(), tagged.gradeLevel());
    }

    @Test
    void textWithoutEnglishWordsIsUnrated() {
        Readability unrated = ReadabilityAnalyzer.analyze(List.of("一二三", ""));
        assertEquals(Difficulty.UNRATED, unrated.difficulty());
    }

    @Test
    void analysesAWholeBible() {
        Readability bible = ReadabilityAnalyzer.analyze(TestBibles.sample());
        assertTrue(bible.difficulty() != Difficulty.UNRATED);
    }

    @Test
    void difficultyBandsFollowTheGradeThresholds() {
        assertEquals(Difficulty.EASY, Difficulty.fromGrade(6.5));
        assertEquals(Difficulty.MODERATE, Difficulty.fromGrade(7.1));
        assertEquals(Difficulty.ADVANCED, Difficulty.fromGrade(8.0));
        assertEquals(Difficulty.SCHOLARLY, Difficulty.fromGrade(8.6));
        assertEquals(Difficulty.UNRATED, Difficulty.fromGrade(Double.NaN));
    }
}
