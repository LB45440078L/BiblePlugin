package org.bibleplugin.core;

import java.time.LocalDate;
import java.util.Random;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;

/** Picks verses at random, or deterministically for a given day ("verse of the day"). */
public final class VersePicker {

    private VersePicker() {
    }

    /** @return a uniformly random verse. */
    public static Reference random(Bible bible, Random random) {
        requireVerses(bible);
        return bible.reference(random.nextInt(bible.verseCount()));
    }

    /**
     * The same verse for everyone on the same date, and a different one for each Bible.
     */
    public static Reference daily(Bible bible, LocalDate date) {
        requireVerses(bible);
        long seed = date.toEpochDay() * 0x9E3779B97F4A7C15L ^ bible.id().hashCode();
        return bible.reference(new Random(seed).nextInt(bible.verseCount()));
    }

    private static void requireVerses(Bible bible) {
        if (bible.verseCount() == 0) {
            throw new BibleException(Problem.VERSE_OUT_OF_RANGE, bible.name() + " contains no verses.");
        }
    }
}
