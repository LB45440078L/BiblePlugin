package org.bibleplugin.source.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.bibleplugin.core.readability.Difficulty;

class BibleCatalogueTest {

    private final BibleCatalogue catalogue = BibleCatalogue.bundled();

    @Test
    void bundlesEveryKnownEdition() {
        assertTrue(catalogue.size() >= 100, "expected the full getBible list but found " + catalogue.size());
        assertTrue(catalogue.languageCodes().size() >= 60);
    }

    @Test
    void englishEditionsCarryMeasuredDifficulty() {
        List<CatalogueEntry> rated = catalogue.rated();
        assertEquals(11, rated.size());
        CatalogueEntry basic = catalogue.find("basicenglish").orElseThrow();
        assertEquals(Difficulty.EASY, basic.readability().orElseThrow().difficulty());
        CatalogueEntry ylt = catalogue.find("ylt").orElseThrow();
        assertEquals(Difficulty.SCHOLARLY, ylt.readability().orElseThrow().difficulty());
    }

    @Test
    void ratedEditionsAreSortedEasiestFirst() {
        List<CatalogueEntry> rated = catalogue.rated();
        for (int i = 1; i < rated.size(); i++) {
            assertTrue(rated.get(i - 1).readability().orElseThrow().gradeLevel()
                    <= rated.get(i).readability().orElseThrow().gradeLevel());
        }
    }

    @Test
    void lookupIsCaseInsensitiveAndOnlyFindsRealEditions() {
        assertTrue(catalogue.find("KJV").isPresent());
        assertFalse(catalogue.find("niv").isPresent());
    }

    @Test
    void urlsAreAbsoluteHttpsLinks() {
        for (CatalogueEntry entry : catalogue.all()) {
            assertEquals("https", entry.url().getScheme(), entry.id());
        }
    }
}
