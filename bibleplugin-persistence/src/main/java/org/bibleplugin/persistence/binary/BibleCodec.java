package org.bibleplugin.persistence.binary;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.model.BibleInfo;
import org.bibleplugin.core.readability.Difficulty;
import org.bibleplugin.core.readability.Readability;

/**
 * Compact binary format for a {@link StoredBible}: a gzip stream of length-prefixed UTF-8 strings.
 *
 * <p>Layout (version 1): magic {@code BPB1}, format version, edition metadata, reading statistics,
 * then books → chapters → verses. Reading it back is several times faster than parsing the
 * original JSON, which is what makes startup load times small.
 */
public final class BibleCodec {

    private static final int MAGIC = 0x42504231; // "BPB1"
    private static final int VERSION = 1;
    private static final int BUFFER = 1 << 16;

    private BibleCodec() {
    }

    public static void write(StoredBible stored, OutputStream sink) throws IOException {
        Bible bible = stored.bible();
        BibleInfo info = bible.info();
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(sink, BUFFER), BUFFER))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            writeString(out, info.id());
            writeString(out, info.name());
            writeString(out, info.languageCode());
            writeString(out, info.language());
            writeString(out, info.license());

            Readability readability = stored.readability();
            out.writeDouble(readability.readingEase());
            out.writeDouble(readability.gradeLevel());
            writeString(out, readability.difficulty().name());

            out.writeInt(bible.bookCount());
            for (BibleBook book : bible.books()) {
                out.writeInt(book.number());
                writeString(out, book.name());
                out.writeInt(book.chapterCount());
                for (BibleChapter chapter : book.chapters()) {
                    out.writeInt(chapter.number());
                    out.writeInt(chapter.verseCount());
                    for (String verse : chapter.verses()) {
                        writeString(out, verse);
                    }
                }
            }
            out.flush();
        }
    }

    public static StoredBible read(InputStream source) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(source, BUFFER), BUFFER))) {
            if (in.readInt() != MAGIC) {
                throw new BibleException(Problem.FORMAT_ERROR, "Not a BiblePlugin cache file.");
            }
            int version = in.readInt();
            if (version != VERSION) {
                throw new BibleException(Problem.FORMAT_ERROR, "Unsupported cache version " + version + ".");
            }
            BibleInfo info = new BibleInfo(readString(in), readString(in), readString(in), readString(in), readString(in));
            double ease = in.readDouble();
            double grade = in.readDouble();
            Difficulty difficulty = Difficulty.valueOf(readString(in));
            Readability readability = new Readability(ease, grade, difficulty);

            int bookCount = in.readInt();
            List<BibleBook> books = new ArrayList<>(bookCount);
            for (int b = 0; b < bookCount; b++) {
                int number = in.readInt();
                String name = readString(in);
                int chapterCount = in.readInt();
                List<BibleChapter> chapters = new ArrayList<>(chapterCount);
                for (int c = 0; c < chapterCount; c++) {
                    int chapterNumber = in.readInt();
                    int verseCount = in.readInt();
                    List<String> verses = new ArrayList<>(verseCount);
                    for (int v = 0; v < verseCount; v++) {
                        verses.add(readString(in));
                    }
                    chapters.add(new BibleChapter(chapterNumber, verses));
                }
                books.add(new BibleBook(number, name, chapters));
            }
            return new StoredBible(new Bible(info, books), readability);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > 1 << 24) {
            throw new BibleException(Problem.FORMAT_ERROR, "Corrupt cache: string length " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
