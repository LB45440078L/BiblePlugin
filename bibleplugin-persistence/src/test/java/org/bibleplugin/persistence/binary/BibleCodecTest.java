package org.bibleplugin.persistence.binary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.model.BibleInfo;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.core.readability.Difficulty;
import org.bibleplugin.core.readability.Readability;

class BibleCodecTest {

    @Test
    void roundTripsBooksChaptersVersesAndStatistics() throws IOException {
        Bible bible = new Bible(
                new BibleInfo("rt", "Round Trip", "fr", "Français", "Public Domain"),
                List.of(
                        new BibleBook(1, "Génèse", List.of(
                                new BibleChapter(1, List.of("Au commencement, Dieu créa.", "Et la terre était informe.")),
                                new BibleChapter(2, List.of("Ainsi furent achevés."))))));
        StoredBible stored = new StoredBible(bible, new Readability(71.5, 8.2, Difficulty.ADVANCED));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        BibleCodec.write(stored, buffer);
        StoredBible copy = BibleCodec.read(new ByteArrayInputStream(buffer.toByteArray()));

        assertEquals(bible.info(), copy.bible().info());
        assertEquals(bible.books(), copy.bible().books());
        assertEquals(stored.readability(), copy.readability());
        assertEquals("Et la terre était informe.", copy.bible().verseText(Reference.ofVerse(1, 1, 2)));
    }

    @Test
    void rejectsForeignData() {
        byte[] garbage = "definitely not a bible".getBytes();
        assertThrows(IOException.class, () -> BibleCodec.read(new ByteArrayInputStream(garbage)));
    }
}
