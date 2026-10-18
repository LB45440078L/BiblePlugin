package org.bibleplugin.spigot.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bibleplugin.core.readability.Readability;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Style;
import org.bibleplugin.source.catalog.CatalogueEntry;
import org.bibleplugin.source.http.BibleDownloader;

/**
 * Browses the bundled catalogue of editions. Each item's lore shows language, licence and — for
 * English — measured reading difficulty. Clicking downloads the edition when needed, then opens
 * its bookshelf.
 */
public final class LibraryMenu extends Menu {

    private static final int PAGE_SIZE = 45;

    private String languageFilter;
    private int page;

    public LibraryMenu(PluginContext context) {
        super(context);
    }

    @Override
    protected String title() {
        return "§6§lBible Library";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void populate(Player viewer) {
        List<CatalogueEntry> entries = context.bibles().catalogue().all().stream()
                .filter(entry -> languageFilter == null || entry.languageCode().equals(languageFilter))
                .toList();
        Paging<CatalogueEntry> paging = Paging.of(entries, page, PAGE_SIZE);
        page = paging.page();

        int slot = 0;
        for (CatalogueEntry entry : paging.items()) {
            set(slot++, item(entry), (player, click) -> open(player, entry));
        }

        pager(paging, 45, 49, 53, requested -> page = requested);
        set(46, Items.of(Material.COMPASS, "§6Language §8» §f" + filterLabel(),
                "§7Click to cycle through languages."), (player, click) -> {
            context.play(player, UiSound.CLICK);
            languageFilter = nextLanguage(languageFilter);
            page = 0;
            refresh();
        });
        set(48, Items.of(Material.BARRIER, "§cClose"), (player, click) -> player.closeInventory());
        set(50, Items.of(Material.KNOWLEDGE_BOOK, "§bHow it works",
                "§7Pick an edition to download it.",
                "§7Difficulty is measured from the text",
                "§7with the Flesch-Kincaid formula."));
        fillEmpty(Items.filler());
    }

    private ItemStack item(CatalogueEntry entry) {
        boolean loaded = context.registry().find(entry.id()).isPresent();
        boolean installed = context.bibles().store().exists(entry.id());
        Material material = loaded ? Material.ENCHANTED_BOOK : Material.BOOK;

        List<String> lore = new ArrayList<>();
        lore.add(Style.MUTED + "Language " + Style.TEXT + entry.language() + Style.DIM + " (" + entry.languageCode() + ")");
        lore.add(Style.MUTED + "Licence " + Style.TEXT + entry.license());
        if (!entry.year().isEmpty()) {
            lore.add(Style.MUTED + "Edition year " + Style.TEXT + entry.year());
        }
        lore.add("");
        lore.add(difficultyLine(entry.readability().orElse(null)));
        lore.add("");
        if (loaded) {
            lore.add(Style.SUCCESS + "✔ Loaded " + Style.DIM + "— click to read");
        } else if (installed) {
            lore.add(Style.AMBER + "● Installed " + Style.DIM + "— click to load and read");
        } else {
            lore.add(Style.MUTED + "⬇ Not downloaded " + Style.DIM + "— click to download");
        }
        return Items.of(material, Style.GOLD + entry.name() + Style.DIM + " (" + entry.id().toUpperCase(Locale.ROOT) + ")", lore);
    }

    static String difficultyLine(Readability readability) {
        if (readability == null) {
            return Style.MUTED + "Difficulty " + Style.DIM + "not rated";
        }
        return Style.MUTED + "Difficulty " + Style.difficultyBar(readability.difficulty()) + " "
                + Style.TEXT + readability.difficulty().label()
                + Style.DIM + " · grade " + Style.TEXT + String.format(Locale.ROOT, "%.1f", readability.gradeLevel())
                + Style.DIM + " · ease " + Style.TEXT + String.format(Locale.ROOT, "%.0f", readability.readingEase());
    }

    private void open(Player player, CatalogueEntry entry) {
        context.play(player, UiSound.CLICK);
        if (context.registry().find(entry.id()).isPresent()) {
            new BookshelfMenu(context, entry.id(), this).open(player);
            return;
        }
        player.sendMessage(context.messages().prefixed("library.downloading", entry.name()));
        context.bibles().download(entry.id(), BibleDownloader.Progress.NONE)
                .whenComplete((bible, failure) -> context.runSync(() -> {
                    if (failure != null) {
                        context.play(player, UiSound.ERROR);
                        player.sendMessage(context.messages().prefixed("library.failed", failure.getMessage()));
                        return;
                    }
                    context.play(player, UiSound.DOWNLOADED);
                    new BookshelfMenu(context, bible.id(), this).open(player);
                }));
    }

    private String filterLabel() {
        return languageFilter == null ? "All" : languageFilter;
    }

    private String nextLanguage(String current) {
        List<String> codes = context.bibles().catalogue().languageCodes();
        if (current == null) {
            return codes.isEmpty() ? null : codes.get(0);
        }
        int index = codes.indexOf(current);
        return index + 1 < codes.size() ? codes.get(index + 1) : null;
    }
}
