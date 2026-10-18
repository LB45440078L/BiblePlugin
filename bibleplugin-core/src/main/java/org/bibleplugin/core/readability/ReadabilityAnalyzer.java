package org.bibleplugin.core.readability;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;

/**
 * Estimates reading difficulty of English text using the Flesch formulas.
 *
 * <p>Words are ASCII letter runs, sentences end at {@code . ! ? ;} followed by whitespace or the end
 * of a verse, and syllables are counted as vowel groups. Markup such as {@code <S>1234</S>} or
 * {@code {note}} is ignored. The method is an estimate meant to rank translations against each
 * other, not an absolute measure.
 */
public final class ReadabilityAnalyzer {

    private static final Pattern MARKUP = Pattern.compile("<[^>]+>|\\{[^}]*}|\\[[^\\]]*]");
    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z'’-]*");
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?;]+(?=\\s|$)");
    private static final Pattern VOWEL_GROUP = Pattern.compile("[aeiouy]+");

    private ReadabilityAnalyzer() {
    }

    /** Analyses every verse of a Bible. */
    public static Readability analyze(Bible bible) {
        return analyze(allVerses(bible));
    }

    /**
     * @param verses verse texts, in order; empty strings and non-English text are ignored
     * @return the statistics, or {@link Readability#unrated()} when no English words were found
     */
    public static Readability analyze(Iterable<String> verses) {
        long words = 0;
        long syllables = 0;
        long sentences = 0;
        for (String verse : verses) {
            if (verse == null || verse.isBlank()) {
                continue;
            }
            String text = MARKUP.matcher(verse).replaceAll(" ");
            Matcher tokens = WORD.matcher(text);
            long verseWords = 0;
            while (tokens.find()) {
                verseWords++;
                syllables += countSyllables(tokens.group());
            }
            if (verseWords == 0) {
                continue;
            }
            words += verseWords;
            Matcher ends = SENTENCE_END.matcher(text);
            long found = 0;
            while (ends.find()) {
                found++;
            }
            sentences += Math.max(1, found);
        }
        if (words == 0) {
            return Readability.unrated();
        }
        double wordsPerSentence = (double) words / sentences;
        double syllablesPerWord = (double) syllables / words;
        double ease = 206.835 - 1.015 * wordsPerSentence - 84.6 * syllablesPerWord;
        double grade = 0.39 * wordsPerSentence + 11.8 * syllablesPerWord - 15.59;
        return new Readability(round(ease), round(grade), Difficulty.fromGrade(grade));
    }

    /**
     * Counts syllables in one word using the vowel-group heuristic with a silent-e adjustment.
     */
    public static int countSyllables(String word) {
        String letters = word.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (letters.isEmpty()) {
            return 0;
        }
        int count = 0;
        Matcher groups = VOWEL_GROUP.matcher(letters);
        while (groups.find()) {
            count++;
        }
        if (letters.endsWith("e") && !letters.endsWith("le") && !letters.endsWith("ee") && count > 1) {
            count--;
        }
        return Math.max(1, count);
    }

    private static Iterable<String> allVerses(Bible bible) {
        return () -> new java.util.Iterator<>() {
            private int bookIndex;
            private int chapterIndex;
            private int verseIndex;

            @Override
            public boolean hasNext() {
                while (bookIndex < bible.bookCount()) {
                    BibleBook book = bible.books().get(bookIndex);
                    if (chapterIndex < book.chapterCount()) {
                        BibleChapter chapter = book.chapters().get(chapterIndex);
                        if (verseIndex < chapter.verseCount()) {
                            return true;
                        }
                        chapterIndex++;
                        verseIndex = 0;
                    } else {
                        bookIndex++;
                        chapterIndex = 0;
                        verseIndex = 0;
                    }
                }
                return false;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                return bible.books().get(bookIndex).chapters().get(chapterIndex).verses().get(verseIndex++);
            }
        };
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
