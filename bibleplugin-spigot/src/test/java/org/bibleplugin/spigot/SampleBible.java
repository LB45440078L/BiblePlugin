package org.bibleplugin.spigot;

import java.util.List;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.model.BibleInfo;

/** A tiny Bible for formatting tests: Genesis 1 (3 verses) and John 1-2 (5 verses). */
public final class SampleBible {

    private SampleBible() {
    }

    public static Bible create() {
        return new Bible(
                new BibleInfo("test", "Test Version", "en", "English", "Public Domain"),
                List.of(
                        new BibleBook(1, "Genesis", List.of(new BibleChapter(1, List.of(
                                "In the beginning God created the heaven and the earth.",
                                "And the earth was without form, and void.",
                                "And God said, Let there be light: and there was light.")))),
                        new BibleBook(2, "John", List.of(
                                new BibleChapter(1, List.of(
                                        "In the beginning was the Word, and the Word was with God.",
                                        "The same was in the beginning with God.",
                                        "All things were made by him.")),
                                new BibleChapter(2, List.of(
                                        "And the third day there was a marriage in Cana of Galilee.",
                                        "Love is patient; love is kind."))))));
    }
}
