package org.bibleplugin.source.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.model.BibleInfo;

/**
 * Streaming reader for the getBible layout:
 * <pre>{@code
 * { "translation": "...", "books": [
 *     { "nr": 1, "name": "Genesis", "chapters": [
 *         { "chapter": 1, "verses": [ { "verse": 1, "text": "In the beginning..." } ] } ] } ] }
 * }</pre>
 * Unknown keys are skipped, so extra metadata does not break the import.
 */
final class GetBibleLayout {

    private final Reader source;
    private final BibleInfo info;

    GetBibleLayout(Reader source, BibleInfo info) {
        this.source = source;
        this.info = info;
    }

    Bible read() throws IOException {
        // The JsonReader is intentionally not closed: the caller owns the underlying stream.
        JsonReader json = new JsonReader(source);
        List<BibleBook> books = List.of();
        json.beginObject();
        while (json.hasNext()) {
            if ("books".equals(json.nextName())) {
                books = readBooks(json);
            } else {
                json.skipValue();
            }
        }
        json.endObject();
        return BibleJsonImporter.assemble(info, books);
    }

    private static List<BibleBook> readBooks(JsonReader json) throws IOException {
        List<BibleBook> books = new ArrayList<>();
        json.beginArray();
        while (json.hasNext()) {
            int number = books.size() + 1;
            String name = "Book " + number;
            List<BibleChapter> chapters = List.of();
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
        return books;
    }

    private static List<BibleChapter> readChapters(JsonReader json) throws IOException {
        List<BibleChapter> chapters = new ArrayList<>();
        json.beginArray();
        while (json.hasNext()) {
            List<String> verses = List.of();
            json.beginObject();
            while (json.hasNext()) {
                if ("verses".equals(json.nextName())) {
                    verses = readVerses(json);
                } else {
                    json.skipValue();
                }
            }
            json.endObject();
            chapters.add(new BibleChapter(chapters.size() + 1, verses));
        }
        json.endArray();
        return chapters;
    }

    /** Verses are placed by their {@code verse} number, leaving "" for any gap. */
    private static List<String> readVerses(JsonReader json) throws IOException {
        TreeMap<Integer, String> byNumber = new TreeMap<>();
        json.beginArray();
        int position = 0;
        while (json.hasNext()) {
            position++;
            int verseNumber = position;
            String text = "";
            json.beginObject();
            while (json.hasNext()) {
                switch (json.nextName()) {
                    case "verse" -> verseNumber = json.peek() == JsonToken.NULL ? position : json.nextInt();
                    case "text" -> text = json.peek() == JsonToken.NULL ? "" : BibleJsonImporter.cleanVerse(json.nextString());
                    default -> json.skipValue();
                }
            }
            json.endObject();
            byNumber.put(verseNumber, text);
        }
        json.endArray();
        if (byNumber.isEmpty()) {
            return List.of();
        }
        int last = byNumber.lastKey();
        List<String> verses = new ArrayList<>(last);
        for (int v = 1; v <= last; v++) {
            verses.add(byNumber.getOrDefault(v, ""));
        }
        return verses;
    }
}
