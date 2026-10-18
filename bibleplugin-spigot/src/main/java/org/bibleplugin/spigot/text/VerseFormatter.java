package org.bibleplugin.spigot.text;

import java.util.ArrayList;
import java.util.List;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.VerseText;

/** Renders verses as chat lines: gold numbers, white text, wrapped with a hanging indent. */
public final class VerseFormatter {

    /** Chat lines are wrapped at this width, which fits the default chat window. */
    public static final int CHAT_WIDTH = 52;

    private VerseFormatter() {
    }

    /**
     * @param bible  the Bible the verses came from, used for book names
     * @param verses verses in reading order
     * @param limit  the most verses to render; the rest are summarised in the last line
     */
    public static List<String> lines(Bible bible, List<VerseText> verses, int limit) {
        List<String> out = new ArrayList<>();
        int shown = Math.min(limit, verses.size());
        boolean singleVerse = verses.size() == 1;
        for (int i = 0; i < shown; i++) {
            VerseText verse = verses.get(i);
            String prefix = singleVerse
                    ? Style.GOLD + bible.book(verse.reference().book()).orElseThrow().name() + " "
                    + verse.reference().chapter() + ":" + verse.reference().fromVerse() + Style.DIM + " " + Style.ARROW + " " + Style.TEXT
                    : Style.GOLD + "" + verse.reference().fromVerse() + Style.DIM + "  " + Style.TEXT;
            String indent = singleVerse ? "  " : "    ";
            List<String> wrapped = TextWrap.wrap(verse.text(), CHAT_WIDTH - indent.length());
            if (wrapped.isEmpty()) {
                out.add(prefix.stripTrailing());
                continue;
            }
            out.add(prefix + wrapped.get(0));
            for (int w = 1; w < wrapped.size(); w++) {
                out.add(indent + Style.TEXT + wrapped.get(w));
            }
        }
        if (verses.size() > shown) {
            out.add(Style.DIM + "… " + Style.MUTED + (verses.size() - shown) + " more verses. Use " + Style.AMBER
                    + "/bible read" + Style.MUTED + " to browse them.");
        }
        return out;
    }
}
