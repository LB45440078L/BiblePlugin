package org.bibleplugin.spigot.text;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.spigot.SampleBible;

class VerseFormatterTest {

    private final Bible bible = SampleBible.create();

    @Test
    void singleVerseShowsTheReferenceOnTheSameLine() {
        List<String> lines = VerseFormatter.lines(bible, bible.passage(Reference.ofVerse(2, 1, 1)), 10);
        assertTrue(lines.get(0).startsWith(Style.GOLD + "John 1:1"));
        assertTrue(lines.size() >= 1);
    }

    @Test
    void rangesNumberEachVerseAndSummariseTheRest() {
        List<String> lines = VerseFormatter.lines(bible, bible.passage(Reference.ofBook(2)), 2);
        assertTrue(lines.get(0).startsWith(Style.GOLD + "1"));
        assertTrue(lines.get(lines.size() - 1).contains("3 more verses"));
    }

    @Test
    void noSummaryWhenEverythingFits() {
        List<String> lines = VerseFormatter.lines(bible, bible.passage(Reference.ofChapter(1, 1)), 10);
        assertTrue(lines.size() >= 3, "wrapping can only add lines");
        assertTrue(lines.stream().noneMatch(line -> line.contains("more verses")));
    }
}
