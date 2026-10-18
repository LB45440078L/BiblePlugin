package org.bibleplugin.source.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleInfo;

/**
 * Turns downloaded or user-supplied Bible JSON into a {@link Bible}.
 *
 * <p>Two layouts are understood, detected from the first character:
 * <ul>
 *   <li>{@code {...}} — the getBible layout: {@code {"books":[{"name":..,"chapters":[{"chapter":1,
 *       "verses":[{"verse":1,"text":..}]}]}]}}. Used by every catalogue edition.</li>
 *   <li>{@code [...]} — the array layout used by thiagobodruk/bible and older BiblePlugin files:
 *       {@code [{"name":..,"abbrev":..,"chapters":[["verse 1","verse 2"],...]}]}.</li>
 * </ul>
 *
 * <p>Both readers stream the input, so a 9&nbsp;MB Bible never exists as a full JSON tree in memory.
 */
public final class BibleJsonImporter {

    private BibleJsonImporter() {
    }

    /** Reads a Bible from a file on disk. */
    public static Bible read(Path file, BibleInfo info) {
        try (InputStream in = Files.newInputStream(file)) {
            return read(in, info);
        } catch (IOException e) {
            throw new BibleException(Problem.STORAGE_ERROR, "Could not read " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads a Bible from a stream. The stream is consumed but not closed.
     *
     * @throws BibleException {@link Problem#FORMAT_ERROR} when the layout is not recognised
     */
    public static Bible read(InputStream in, BibleInfo info) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 16);
            skipBomAndWhitespace(reader);
            reader.mark(1);
            int first = reader.read();
            reader.reset();
            return switch (first) {
                case '{' -> new GetBibleLayout(reader, info).read();
                case '[' -> new ArrayLayout(reader, info).read();
                case -1 -> throw new BibleException(Problem.FORMAT_ERROR, "The file is empty.");
                default -> throw new BibleException(Problem.FORMAT_ERROR,
                        "Unrecognised Bible JSON: expected '{' or '[' but found '" + (char) first + "'.");
            };
        } catch (IOException e) {
            throw new BibleException(Problem.FORMAT_ERROR, "Could not parse the Bible JSON: " + e.getMessage(), e);
        }
    }

    /** Normalises a verse for storage: removes HTML-style tags and collapses whitespace. */
    static String cleanVerse(String raw) {
        if (raw == null) {
            return "";
        }
        String withoutTags = raw.indexOf('<') >= 0 ? raw.replaceAll("<[^>]*>", " ") : raw;
        return withoutTags.replaceAll("\\s+", " ").strip();
    }

    static Bible assemble(BibleInfo info, List<BibleBook> books) {
        if (books.isEmpty()) {
            throw new BibleException(Problem.FORMAT_ERROR, "The Bible " + info.name() + " contains no books.");
        }
        return new Bible(info, books);
    }

    private static void skipBomAndWhitespace(BufferedReader reader) throws IOException {
        reader.mark(4);
        int c = reader.read();
        if (c == '﻿') {
            reader.mark(1);
            c = reader.read();
        }
        while (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
            reader.mark(1);
            c = reader.read();
        }
        if (c != -1) {
            reader.reset();
        }
    }
}
