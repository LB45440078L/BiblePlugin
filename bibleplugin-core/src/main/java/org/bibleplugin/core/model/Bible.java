package org.bibleplugin.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;

/**
 * An immutable Bible edition: metadata plus books in canonical order.
 *
 * <p>Every verse also has a global <em>ordinal</em> (0 .. {@link #verseCount()} - 1), which the
 * search index and random picks use. Ordinals follow canonical order: book, chapter, verse.
 */
public final class Bible {

    private final BibleInfo info;
    private final List<BibleBook> books;
    private final int[] bookOffsets;

    /**
     * @param info  edition metadata
     * @param books books whose {@link BibleBook#number()} equal their 1-based position
     */
    public Bible(BibleInfo info, List<BibleBook> books) {
        this.info = Objects.requireNonNull(info, "info");
        this.books = List.copyOf(books);
        this.bookOffsets = new int[this.books.size() + 1];
        int running = 0;
        for (int i = 0; i < this.books.size(); i++) {
            BibleBook book = this.books.get(i);
            if (book.number() != i + 1) {
                throw new IllegalArgumentException("book " + book.name() + " has number " + book.number()
                        + " but sits at position " + (i + 1));
            }
            bookOffsets[i] = running;
            running += book.verseCount();
        }
        bookOffsets[this.books.size()] = running;
    }

    public BibleInfo info() {
        return info;
    }

    public String id() {
        return info.id();
    }

    public String name() {
        return info.name();
    }

    public List<BibleBook> books() {
        return books;
    }

    public int bookCount() {
        return books.size();
    }

    public int verseCount() {
        return bookOffsets[books.size()];
    }

    public Optional<BibleBook> book(int number) {
        if (number < 1 || number > books.size()) {
            return Optional.empty();
        }
        return Optional.of(books.get(number - 1));
    }

    /**
     * Finds a book by its exact display name, ignoring case.
     *
     * @return the book, or empty when no book has that name.
     */
    public Optional<BibleBook> bookNamed(String name) {
        for (BibleBook book : books) {
            if (book.name().equalsIgnoreCase(name)) {
                return Optional.of(book);
            }
        }
        return Optional.empty();
    }

    /**
     * Converts a verse reference into its global ordinal.
     *
     * @throws BibleException when the reference does not exist in this Bible
     */
    public int ordinal(Reference reference) {
        validate(reference);
        BibleBook book = books.get(reference.book() - 1);
        int localVerse = book.chapterOffset(reference.chapter()) + reference.fromVerse() - 1;
        return bookOffsets[reference.book() - 1] + localVerse;
    }

    /** Converts a global ordinal back into a single-verse reference. */
    public Reference reference(int ordinal) {
        if (ordinal < 0 || ordinal >= verseCount()) {
            throw new BibleException(Problem.VERSE_OUT_OF_RANGE, "verse ordinal out of range: " + ordinal);
        }
        int bookIndex = findBookIndex(ordinal);
        BibleBook book = books.get(bookIndex);
        int local = ordinal - bookOffsets[bookIndex];
        int chapter = book.chapterForOrdinal(local);
        int verse = local - book.chapterOffset(chapter) + 1;
        return Reference.ofVerse(book.number(), chapter, verse);
    }

    /**
     * Returns the verse text for a single-verse reference.
     *
     * @throws BibleException when the reference is out of range
     */
    public String verseText(Reference reference) {
        validate(reference);
        BibleChapter chapter = books.get(reference.book() - 1).chapter(reference.chapter());
        return chapter.verse(reference.fromVerse());
    }

    /**
     * Expands any reference into the verses it covers, in order.
     *
     * @throws BibleException when the reference is out of range
     */
    public List<VerseText> passage(Reference reference) {
        validate(reference);
        BibleBook book = books.get(reference.book() - 1);
        int firstChapter = reference.isBook() ? 1 : reference.chapter();
        int lastChapter = reference.isBook() ? book.chapterCount() : reference.chapter();
        java.util.ArrayList<VerseText> out = new java.util.ArrayList<>();
        for (int c = firstChapter; c <= lastChapter; c++) {
            BibleChapter chapter = book.chapter(c);
            int from = reference.isVerseRange() && c == reference.chapter() ? reference.fromVerse() : 1;
            int to = reference.isVerseRange() && c == reference.chapter() ? reference.toVerse() : chapter.verseCount();
            for (int v = from; v <= to; v++) {
                out.add(new VerseText(Reference.ofVerse(book.number(), c, v), chapter.verse(v)));
            }
        }
        return List.copyOf(out);
    }

    /**
     * Returns the half-open ordinal range covered by a book, chapter or verse range.
     *
     * @throws BibleException when the reference is out of range
     */
    public OrdinalRange ordinalRange(Reference reference) {
        validate(reference);
        if (reference.isBook()) {
            int bookIndex = reference.book() - 1;
            return new OrdinalRange(bookOffsets[bookIndex], bookOffsets[bookIndex + 1]);
        }
        int start = ordinal(Reference.ofVerse(reference.book(), reference.chapter(), 1));
        BibleBook book = books.get(reference.book() - 1);
        if (reference.isChapter()) {
            return new OrdinalRange(start, start + book.chapter(reference.chapter()).verseCount());
        }
        return new OrdinalRange(ordinal(reference), ordinal(reference) + reference.toVerse() - reference.fromVerse() + 1);
    }

    /**
     * Checks that a reference points at existing content.
     *
     * @throws BibleException with a specific {@link Problem} when it does not
     */
    public void validate(Reference reference) {
        Objects.requireNonNull(reference, "reference");
        if (reference.book() > books.size()) {
            throw new BibleException(Problem.BOOK_NOT_FOUND, "book " + reference.book() + " does not exist");
        }
        BibleBook book = books.get(reference.book() - 1);
        if (reference.isBook()) {
            return;
        }
        if (reference.chapter() > book.chapterCount()) {
            throw new BibleException(Problem.CHAPTER_OUT_OF_RANGE,
                    book.name() + " has only " + book.chapterCount() + " chapters");
        }
        if (reference.isChapter()) {
            return;
        }
        int verses = book.chapter(reference.chapter()).verseCount();
        if (reference.toVerse() > verses) {
            throw new BibleException(Problem.VERSE_OUT_OF_RANGE,
                    book.name() + " " + reference.chapter() + " has only " + verses + " verses");
        }
    }

    private int findBookIndex(int ordinal) {
        int lo = 0;
        int hi = books.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (bookOffsets[mid] <= ordinal) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    @Override
    public String toString() {
        return "Bible[" + info.id() + ", books=" + books.size() + ", verses=" + verseCount() + "]";
    }
}
