package org.bibleplugin.spigot.gui;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Style;

/** Shows every book of one Bible. Clicking a book opens its chapters. */
public final class BookshelfMenu extends Menu {

    private static final int PAGE_SIZE = 45;

    private final String bibleId;
    private final Menu parent;
    private int page;

    public BookshelfMenu(PluginContext context, String bibleId, Menu parent) {
        super(context);
        this.bibleId = bibleId;
        this.parent = parent;
    }

    @Override
    protected String title() {
        return Style.GOLD + "" + Style.BOLD + bibleId.toUpperCase(java.util.Locale.ROOT)
                + Style.DIM + " · " + Style.MUTED + "Books";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void populate(Player viewer) {
        Bible bible = context.registry().require(bibleId);
        Paging<BibleBook> paging = Paging.of(bible.books(), page, PAGE_SIZE);
        page = paging.page();

        int slot = 0;
        for (BibleBook book : paging.items()) {
            set(slot++, Items.of(Material.BOOK, Style.GOLD + book.name(),
                    List.of(Style.MUTED + "Chapters " + Style.TEXT + book.chapterCount(),
                            Style.MUTED + "Verses " + Style.TEXT + book.verseCount(),
                            "",
                            Style.DIM + "Click to choose a chapter")),
                    navigate(() -> new ChapterMenu(context, bibleId, book.number(), this).open(viewer)));
        }

        pager(paging, 45, 49, 53, requested -> page = requested);
        if (parent != null) {
            set(48, Items.of(Material.ARROW, "§7« Back"), navigate(() -> parent.open(viewer)));
        }
        fillEmpty(Items.filler());
    }
}
