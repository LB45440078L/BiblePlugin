package org.bibleplugin.core;

import java.util.List;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.model.BibleInfo;

/** Small hand-written Bible used across the core tests. */
public final class TestBibles {

    private TestBibles() {
    }

    public static Bible sample() {
        return new Bible(
                new BibleInfo("test", "Test Version", "en", "English", "Public Domain"),
                List.of(
                        new BibleBook(1, "Genesis", List.of(
                                chapter(1, "In the beginning God created the heaven and the earth.",
                                        "And the earth was without form, and void.",
                                        "And God said, Let there be light: and there was light."),
                                chapter(2, "Thus the heavens and the earth were finished.",
                                        "And on the seventh day God ended his work."))),
                        new BibleBook(2, "John", List.of(
                                chapter(1, "In the beginning was the Word, and the Word was with God.",
                                        "The same was in the beginning with God.",
                                        "All things were made by him; and without him was not any thing made that was made."),
                                chapter(2, "And the third day there was a marriage in Cana of Galilee.",
                                        "Love is patient; love is kind."))),
                        new BibleBook(3, "1 John", List.of(
                                chapter(1, "This then is the message which we have heard of him.",
                                        "God is love; and he that dwelleth in love dwelleth in God."))),
                        new BibleBook(4, "Psalms", List.of(
                                chapter(1, "The LORD is my shepherd; I shall not want."))),
                        new BibleBook(5, "Señor Notes", List.of(
                                chapter(1, "The señor spoke."))),
                        new BibleBook(6, "Jonah", List.of(
                                chapter(1, "Now the word of the LORD came unto Jonah.")))));
    }

    private static BibleChapter chapter(int number, String... verses) {
        return new BibleChapter(number, List.of(verses));
    }
}
