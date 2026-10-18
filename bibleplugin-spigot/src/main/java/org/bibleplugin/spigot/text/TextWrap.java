package org.bibleplugin.spigot.text;

import java.util.ArrayList;
import java.util.List;

/** Word-wraps plain text to a fixed width. Pure, so it can be unit-tested without a server. */
public final class TextWrap {

    private TextWrap() {
    }

    /**
     * @param text  text to wrap; runs of whitespace collapse to single spaces
     * @param width maximum visible characters per line, at least 8
     * @return the wrapped lines, never empty for non-blank input
     */
    public static List<String> wrap(String text, int width) {
        if (width < 8) {
            throw new IllegalArgumentException("width must be at least 8");
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.strip().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            if (word.length() > width) {
                for (int start = 0; start < word.length(); start += width) {
                    if (start > 0) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    line.append(word, start, Math.min(word.length(), start + width));
                }
            } else {
                line.append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
