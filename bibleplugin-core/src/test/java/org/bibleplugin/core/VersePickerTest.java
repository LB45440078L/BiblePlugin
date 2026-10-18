package org.bibleplugin.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;

class VersePickerTest {

    private final Bible bible = TestBibles.sample();

    @Test
    void dailyVerseIsStableForADate() {
        LocalDate day = LocalDate.of(2026, 10, 18);
        assertEquals(VersePicker.daily(bible, day), VersePicker.daily(bible, day));
    }

    @Test
    void dailyVerseChangesAcrossDays() {
        Set<Reference> seen = new HashSet<>();
        LocalDate day = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 60; i++) {
            seen.add(VersePicker.daily(bible, day.plusDays(i)));
        }
        assertTrue(seen.size() > 5, "expected variety across days but got " + seen.size());
    }

    @Test
    void randomVerseIsAlwaysInsideTheBible() {
        Random random = new Random(42);
        for (int i = 0; i < 200; i++) {
            Reference reference = VersePicker.random(bible, random);
            assertEquals(reference, bible.reference(bible.ordinal(reference)));
        }
    }
}
