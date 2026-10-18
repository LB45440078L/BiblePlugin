package org.bibleplugin.source.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.model.BibleInfo;

/**
 * Streaming reader for the array layout used by thiagobodruk/bible and earlier BiblePlugin releases:
 * <pre>{@code
 * [ { "name": "Genesis", "abbrev": "gn", "chapters": [ ["In the beginning...", "And the earth..."] ] } ]
 * }</pre>
 */
final class ArrayLayout {

    private final Reader source;
    private final BibleInfo info;

    ArrayLayout(Reader source, BibleInfo info) {
        this.source = source;
        this.info = info;
    }

    Bible read() throws IOException {
        // The JsonReader is intentionally not closed: the caller owns the underlying stream.
        JsonReader json = new JsonReader(source);
        List<BibleBook> books = new ArrayList<>();
        json.beginArray();
        while (json.hasNext()) {
            int number = books.size() + 1;
            String name = "Book " + number;
            List<BibleChapter> chapters = new ArrayList<>();
            json.beginObject();
            while (json.hasNext()) {
                switch (json.nextName()) {
                    case "name" -> name = json.nextString();
                    case "chapters" -> chapters = readChapters(json);
                    default -> json.skipValue();
                }
            }
            json.endObject();
            books.add(new BibleBook(number, name, chapters));
        }
        json.endArray();
        return BibleJsonImporter.assemble(info, books);
    }

    private static List<BibleChapter> readChapters(JsonReader json) throws IOException {
        List<BibleChapter> chapters = new ArrayList<>();
        json.beginArray();
        while (json.hasNext()) {
            List<String> verses = new ArrayList<>();
            json.beginArray();
            while (json.hasNext()) {
                verses.add(json.peek() == JsonToken.NULL
                        ? readNull(json)
                        : BibleJsonImporter.cleanVerse(json.nextString()));
            }
            json.endArray();
            chapters.add(new BibleChapter(chapters.size() + 1, verses));
        }
        json.endArray();
        return chapters;
    }

    private static String readNull(JsonReader json) throws IOException {
        json.nextNull();
        return "";
    }
}
