package org.bibleplugin.spigot.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.bibleplugin.core.readability.Difficulty;

class StyleTest {

    @Test
    void hexColoursUseTheSixteenPlusSequence() {
        assertEquals("§x§F§5§C§5§4§2", Style.hex("#F5C542"));
        assertEquals("§x§0§0§F§F§0§0", Style.hex("00ff00"));
        assertThrows(IllegalArgumentException.class, () -> Style.hex("#XYZ123"));
    }

    @Test
    void gradientKeepsEveryCharacterAndSpaces() {
        String out = Style.gradient("Bible", "#000000", "#FFFFFF");
        assertEquals("Bible", out.replaceAll("§x(§[0-9A-Fa-f]){6}", "").replaceAll("§r$", ""));
        assertTrue(out.endsWith(Style.RESET));
    }

    @Test
    void difficultyBarFillsByBand() {
        assertEquals(Style.SUCCESS + "▮" + Style.DIM + "▯▯▯" + Style.RESET, Style.difficultyBar(Difficulty.EASY));
        assertEquals(Style.DIM + "▯▯▯▯" + Style.RESET, Style.difficultyBar(Difficulty.UNRATED));
    }

    @Test
    void headerContainsTheTitle() {
        assertTrue(Style.header("John 3:16").replaceAll("§x(§[0-9A-Fa-f]){6}|§.", "").contains("John 3:16"));
    }
}
