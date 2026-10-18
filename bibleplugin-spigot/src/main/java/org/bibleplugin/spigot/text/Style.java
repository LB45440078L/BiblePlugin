package org.bibleplugin.spigot.text;

import java.util.Locale;
import org.bibleplugin.core.readability.Difficulty;

/**
 * Colours and decorations used everywhere the plugin writes text. Legacy {@code §} codes are used
 * for maximum compatibility; hex colours are written in the 1.16+ {@code §x} form.
 */
public final class Style {

    public static final String GOLD = "§6";
    public static final String AMBER = "§e";
    public static final String AQUA = "§b";
    public static final String TEXT = "§f";
    public static final String MUTED = "§7";
    public static final String DIM = "§8";
    public static final String SUCCESS = "§a";
    public static final String ERROR = "§c";
    public static final String BOLD = "§l";
    public static final String RESET = "§r";

    /** Frame glyphs for headers and separators. */
    public static final String RULE = "━";
    public static final String DOT = "·";
    public static final String ARROW = "»";

    private Style() {
    }

    /** @return the 1.16+ {@code §x§R§R§G§G§B§B} sequence for an {@code #RRGGBB} colour. */
    public static String hex(String rrggbb) {
        String digits = rrggbb.startsWith("#") ? rrggbb.substring(1) : rrggbb;
        if (!digits.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("not a hex colour: " + rrggbb);
        }
        StringBuilder out = new StringBuilder("§x");
        for (char c : digits.toUpperCase(Locale.ROOT).toCharArray()) {
            out.append('§').append(c);
        }
        return out.toString();
    }

    /** Colours each visible character with a gradient between two hex colours. */
    public static String gradient(String text, String fromHex, String toHex) {
        int[] from = rgb(fromHex);
        int[] to = rgb(toHex);
        int length = Math.max(1, text.length() - 1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                out.append(' ');
                continue;
            }
            double t = (double) i / length;
            String color = String.format(Locale.ROOT, "#%02x%02x%02x",
                    (int) Math.round(from[0] + (to[0] - from[0]) * t),
                    (int) Math.round(from[1] + (to[1] - from[1]) * t),
                    (int) Math.round(from[2] + (to[2] - from[2]) * t));
            out.append(hex(color)).append(c);
        }
        return out.append(RESET).toString();
    }

    /** A framed title line, e.g. {@code ━━━ King James Version ━━━}. */
    public static String header(String title) {
        String rule = DIM + RULE.repeat(4) + " ";
        return rule + BOLD + gradient(title, "#F5C542", "#E8772E") + " " + DIM + RULE.repeat(4) + RESET;
    }

    /** A thin separator line. */
    public static String divider() {
        return DIM + RULE.repeat(34) + RESET;
    }

    /** A four-segment bar coloured by difficulty, e.g. {@code ▮▮▮▯}. */
    public static String difficultyBar(Difficulty difficulty) {
        int filled = switch (difficulty) {
            case EASY -> 1;
            case MODERATE -> 2;
            case ADVANCED -> 3;
            case SCHOLARLY -> 4;
            case UNRATED -> 0;
        };
        String colour = switch (difficulty) {
            case EASY -> SUCCESS;
            case MODERATE -> AMBER;
            case ADVANCED -> "§6";
            case SCHOLARLY -> ERROR;
            case UNRATED -> DIM;
        };
        String fill = filled == 0 ? "" : colour + "▮".repeat(filled);
        return fill + DIM + "▯".repeat(4 - filled) + RESET;
    }

    private static int[] rgb(String hex) {
        String digits = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[] {
            Integer.parseInt(digits.substring(0, 2), 16),
            Integer.parseInt(digits.substring(2, 4), 16),
            Integer.parseInt(digits.substring(4, 6), 16),
        };
    }
}
