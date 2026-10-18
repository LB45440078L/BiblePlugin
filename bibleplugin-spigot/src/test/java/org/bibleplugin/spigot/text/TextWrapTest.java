package org.bibleplugin.spigot.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextWrapTest {

    @Test
    void wrapsOnWordBoundaries() {
        List<String> lines = TextWrap.wrap("In the beginning God created the heaven and the earth.", 20);
        assertEquals(List.of("In the beginning God", "created the heaven", "and the earth."), lines);
    }

    @Test
    void everyLineFitsTheWidth() {
        String text = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish.";
        for (String line : TextWrap.wrap(text, 24)) {
            assertTrue(line.length() <= 24, "too long: " + line);
        }
    }

    @Test
    void splitsWordsLongerThanTheWidth() {
        assertEquals(List.of("abcdefgh", "ijklmnop"), TextWrap.wrap("abcdefghijklmnop", 8));
    }

    @Test
    void collapsesWhitespaceAndHandlesBlankInput() {
        assertEquals(List.of("a b c"), TextWrap.wrap("  a \t b\n c  ", 20));
        assertEquals(List.of(), TextWrap.wrap("   ", 20));
    }

    @Test
    void rejectsSillyWidths() {
        assertThrows(IllegalArgumentException.class, () -> TextWrap.wrap("text", 3));
    }
}
