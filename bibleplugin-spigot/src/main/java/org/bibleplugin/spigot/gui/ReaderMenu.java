package org.bibleplugin.spigot.gui;

import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bibleplugin.core.book.TemporaryBook;
import org.bibleplugin.core.book.TemporaryBookComposer;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.Bookmark;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.core.model.VerseText;
import org.bibleplugin.core.search.SearchEngine;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Style;

/**
 * Reads one chapter, 44 verses to a page. Left-click a verse to send it to chat, right-click to
 * bookmark it. The bottom row navigates chapters, searches, and makes temporary books.
 */
public final class ReaderMenu extends Menu {

    private static final int PAGE_SIZE = 44;

    private final String bibleId;
    private final Menu parent;
    private int bookNumber;
    private int chapter;
    private int page;

    public ReaderMenu(PluginContext context, String bibleId, Reference start, Menu parent) {
        super(context);
        this.bibleId = bibleId;
        this.parent = parent;
        this.bookNumber = start.book();
        this.chapter = Math.max(1, start.chapter());
        int startVerse = start.isVerseRange() ? start.fromVerse() : 1;
        this.page = (startVerse - 1) / PAGE_SIZE;
    }

    @Override
    protected String title() {
        Bible bible = bible();
        return Style.GOLD + "" + Style.BOLD + bible.book(bookNumber).map(BibleBook::name).orElse("?")
                + " " + chapter + Style.DIM + " · " + Style.MUTED + bible.id().toUpperCase(java.util.Locale.ROOT);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void populate(Player viewer) {
        Bible bible = bible();
        BibleBook book = bible.book(bookNumber).orElseThrow();
        List<VerseText> verses = bible.passage(Reference.ofChapter(bookNumber, chapter));
        var paging = Paging.of(verses, page, PAGE_SIZE);
        page = paging.page();

        int slot = 0;
        for (VerseText verse : paging.items()) {
            Reference reference = verse.reference();
            set(slot++, Items.of(Material.PAPER,
                            Style.AMBER + "" + reference.fromVerse() + Style.DIM + " · " + Style.MUTED + book.name() + " " + chapter,
                            Items.wrapped(verse.text(), Style.TEXT)),
                    (player, click) -> verseClicked(player, click, verse));
        }

        pager(paging, 45, 49, 53, requested -> page = requested);
        if (parent != null) {
            set(44, Items.of(Material.ARROW, "§7« Back"), navigate(() -> parent.open(viewer)));
        }
        if (chapter > 1) {
            set(46, Items.of(Material.SPECTRAL_ARROW, "§e◀ Chapter " + (chapter - 1)), (player, click) ->
                    moveTo(player, bookNumber, chapter - 1));
        }
        if (chapter < book.chapterCount()) {
            set(52, Items.of(Material.SPECTRAL_ARROW, "§eChapter " + (chapter + 1) + " ▶"), (player, click) ->
                    moveTo(player, bookNumber, chapter + 1));
        }
        set(47, Items.of(Material.COMPASS, "§bSearch", "§7Find a word or phrase", "§7in " + bible.name()),
                (player, click) -> context.prompts().requestText(player, "Search " + bible.id().toUpperCase(java.util.Locale.ROOT),
                        "", query -> search(player, query)));
        set(48, Items.of(Material.BOOKSHELF, "§6Books", "§7Jump to another book"),
                navigate(() -> new BookshelfMenu(context, bibleId, this).open(viewer)));
        set(50, Items.of(Material.NETHER_STAR, "§6★ Bookmark", "§7Save " + book.name() + " " + chapter),
                (player, click) -> bookmark(player));
        set(51, Items.of(Material.WRITABLE_BOOK, "§d✦ Temporary book", "§7Get this chapter as a book"),
                (player, click) -> temporaryBook(player));
        fillEmpty(Items.filler());
        rememberPosition(viewer);
    }

    private void verseClicked(Player player, ClickType click, VerseText verse) {
        if (click.isRightClick()) {
            context.players().update(player.getUniqueId(), record -> record.withBookmark(new Bookmark(
                    verse.reference().format(bible().book(verse.reference().book()).orElseThrow().name()),
                    bibleId, verse.reference(), System.currentTimeMillis())));
            context.play(player, UiSound.SUCCESS);
            player.sendMessage(context.messages().prefixed("bookmark.added", verse.reference().fromVerse()));
            return;
        }
        context.play(player, UiSound.CLICK);
        String label = bible().book(verse.reference().book()).orElseThrow().name() + " " + verse.reference().chapter()
                + ":" + verse.reference().fromVerse();
        player.sendMessage(Style.GOLD + label + Style.DIM + " " + Style.ARROW + " " + Style.TEXT + verse.text());
    }

    private void moveTo(Player player, int book, int newChapter) {
        context.play(player, UiSound.PAGE);
        this.bookNumber = book;
        this.chapter = newChapter;
        this.page = 0;
        refresh();
    }

    private void bookmark(Player player) {
        BibleBook book = bible().book(bookNumber).orElseThrow();
        Reference reference = Reference.ofChapter(bookNumber, chapter);
        context.players().update(player.getUniqueId(), record -> record.withBookmark(new Bookmark(
                reference.format(book.name()), bibleId, reference, System.currentTimeMillis())));
        context.play(player, UiSound.SUCCESS);
        player.sendMessage(context.messages().prefixed("bookmark.added", reference.format(book.name())));
    }

    private void temporaryBook(Player player) {
        Bible bible = bible();
        BibleBook book = bible.book(bookNumber).orElseThrow();
        Reference reference = Reference.ofChapter(bookNumber, chapter);
        context.async(() -> TemporaryBookComposer.compose(bible, reference, book.name(),
                        new TemporaryBookComposer.Layout(context.settings().bookCharsPerPage(), context.settings().bookMaxPages())))
                .whenComplete((temporary, failure) -> context.runSync(() -> {
                    if (failure != null) {
                        context.play(player, UiSound.ERROR);
                        return;
                    }
                    context.books().give(player, temporary);
                }));
    }

    private void search(Player player, String query) {
        Bible bible = bible();
        context.async(() -> context.registry().search().search(bible, Optional.empty(), query,
                        context.settings().maxSearchResults()))
                .whenComplete((result, failure) -> context.runSync(() -> {
                    if (failure != null) {
                        context.play(player, UiSound.ERROR);
                        String message = failure.getCause() instanceof BibleException error
                                ? error.getMessage() : String.valueOf(failure.getMessage());
                        player.sendMessage(context.messages().prefixed("search.error", message));
                        return;
                    }
                    new ResultsMenu(context, bibleId, query, result, this).open(player);
                }));
    }

    private void rememberPosition(Player viewer) {
        Reference reference = Reference.ofChapter(bookNumber, chapter);
        context.async(() -> context.players().update(viewer.getUniqueId(),
                record -> record.withLastPosition(bibleId, reference)));
    }

    private Bible bible() {
        return context.registry().require(bibleId);
    }
}
