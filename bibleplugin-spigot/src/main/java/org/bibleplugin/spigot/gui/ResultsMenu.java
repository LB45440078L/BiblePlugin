package org.bibleplugin.spigot.gui;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.core.search.SearchEngine;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.text.Style;

/** Search hits, 44 to a page. Clicking a hit opens the reader at that verse. */
public final class ResultsMenu extends Menu {

    private static final int PAGE_SIZE = 44;

    private final String bibleId;
    private final String query;
    private final SearchEngine.Result result;
    private final Menu parent;
    private int page;

    public ResultsMenu(PluginContext context, String bibleId, String query, SearchEngine.Result result, Menu parent) {
        super(context);
        this.bibleId = bibleId;
        this.query = query;
        this.result = result;
        this.parent = parent;
    }

    @Override
    protected String title() {
        return Style.GOLD + "" + Style.BOLD + "Results" + Style.DIM + " · " + Style.MUTED + query;
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void populate(Player viewer) {
        Bible bible = context.registry().require(bibleId);
        var paging = org.bibleplugin.spigot.gui.Paging.of(result.hits(), page, PAGE_SIZE);
        page = paging.page();

        int slot = 0;
        for (SearchEngine.Hit hit : paging.items()) {
            Reference reference = hit.reference();
            String name = bible.book(reference.book()).orElseThrow().name();
            set(slot++, Items.of(Material.PAPER, Style.GOLD + reference.format(name),
                            Items.wrapped(hit.text(), Style.TEXT)),
                    navigate(() -> new ReaderMenu(context, bibleId, reference, this).open(viewer)));
        }

        pager(paging, 45, 49, 53, requested -> page = requested);
        set(50, Items.of(Material.BOOK, Style.SUCCESS + result.matchingVerses() + Style.MUTED + " verses",
                List.of(Style.MUTED + "Occurrences " + Style.TEXT + result.occurrences(),
                        Style.DIM + "Showing the first " + result.hits().size() + " hits.")));
        if (parent != null) {
            set(44, Items.of(Material.ARROW, "§7« Back"), navigate(() -> parent.open(viewer)));
        }
        fillEmpty(Items.filler());
    }
}
