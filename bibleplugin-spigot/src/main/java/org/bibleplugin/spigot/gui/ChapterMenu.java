package org.bibleplugin.spigot.gui;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.BibleChapter;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.text.Style;

/** Lists the chapters of one book. Clicking a chapter opens the reader. */
public final class ChapterMenu extends Menu {

    private static final int PAGE_SIZE = 45;

    private final String bibleId;
    private final int bookNumber;
    private final Menu parent;
    private int page;

    public ChapterMenu(PluginContext context, String bibleId, int bookNumber, Menu parent) {
        super(context);
        this.bibleId = bibleId;
        this.bookNumber = bookNumber;
        this.parent = parent;
    }

    @Override
    protected String title() {
        BibleBook book = book();
        return Style.GOLD + "" + Style.BOLD + book.name() + Style.DIM + " · " + Style.MUTED + "Chapters";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void populate(Player viewer) {
        BibleBook book = book();
        Paging<BibleChapter> paging = Paging.of(book.chapters(), page, PAGE_SIZE);
        page = paging.page();

        int slot = 0;
        for (BibleChapter chapter : paging.items()) {
            Reference reference = Reference.ofChapter(bookNumber, chapter.number());
            set(slot++, Items.of(Material.PAPER, Style.AMBER + "Chapter " + chapter.number(),
                    List.of(Style.MUTED + "Verses " + Style.TEXT + chapter.verseCount(), "",
                            Style.DIM + "Click to read", Style.DIM + reference.format(book.name()))),
                    navigate(() -> new ReaderMenu(context, bibleId, reference, this).open(viewer)));
        }

        pager(paging, 45, 49, 53, requested -> page = requested);
        if (parent != null) {
            set(48, Items.of(Material.ARROW, "§7« Back"), navigate(() -> parent.open(viewer)));
        }
        fillEmpty(Items.filler());
    }

    private BibleBook book() {
        Bible bible = context.registry().require(bibleId);
        return bible.book(bookNumber).orElseThrow();
    }
}
