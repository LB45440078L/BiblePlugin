package org.bibleplugin.spigot.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.Player;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleInfo;
import org.bibleplugin.core.readability.Readability;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.gui.LibraryMenu;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Messages;
import org.bibleplugin.spigot.text.Style;
import org.bibleplugin.source.catalog.CatalogueEntry;
import org.bibleplugin.source.http.BibleDownloader;

/** Commands that manage which editions exist, are loaded, and are the default. */
public final class LibraryCommands implements CommandModule {

    private static final int AVAILABLE_PAGE_SIZE = 12;

    @Override
    public List<Subcommand> commands(PluginContext s) {
        return List.of(
                new Subcommand("available", List.of("list"), "bible.use", "[language | page]",
                        "List the editions you can download.",
                        ctx -> available(ctx, s),
                        ctx -> languageCodes(s)),
                new Subcommand("downloaded", List.of("installed"), "bible.use", "",
                        "Show which editions are loaded or installed.",
                        ctx -> downloaded(ctx, s), Subcommand.Completer.NONE),
                new Subcommand("download", List.of(), "bible.download", "<id>",
                        "Download an edition from the catalogue.",
                        ctx -> download(ctx, s), ctx -> catalogueIds(s)),
                new Subcommand("load", List.of(), "bible.admin", "<id>",
                        "Load an installed edition into memory.",
                        ctx -> load(ctx, s), ctx -> installedIds(s)),
                new Subcommand("unload", List.of(), "bible.admin", "<id>",
                        "Remove an edition from memory (keeps the file).",
                        ctx -> unload(ctx, s), ctx -> loadedIds(s)),
                new Subcommand("delete", List.of(), "bible.admin", "<id>",
                        "Delete an edition from disk.",
                        ctx -> delete(ctx, s), ctx -> installedIds(s)),
                new Subcommand("default", List.of(), "bible.admin", "<id>",
                        "Choose the Bible used when none is named.",
                        ctx -> setDefault(ctx, s), ctx -> loadedIds(s)),
                new Subcommand("info", List.of("about"), "bible.use", "[bible]",
                        "Show an edition's details and reading difficulty.",
                        ctx -> info(ctx, s), ctx -> loadedIds(s)),
                new Subcommand("import", List.of(), "bible.admin", "<file> <id> <language-code> [name]",
                        "Import your own JSON Bible from the imports folder.",
                        ctx -> importFile(ctx, s), Subcommand.Completer.NONE),
                new Subcommand("gui", List.of("library", "menu"), "bible.use", "",
                        "Open the Bible library in a menu.",
                        ctx -> new LibraryMenu(s).open(ctx.player()), Subcommand.Completer.NONE),
                new Subcommand("reload", List.of(), "bible.admin", "",
                        "Reload config.yml and messages.yml.",
                        ctx -> reload(ctx, s), Subcommand.Completer.NONE));
    }

    private static void available(CommandContext ctx, PluginContext s) {
        var catalogue = s.bibles().catalogue();
        if (ctx.hasNext() && ctx.peek().chars().allMatch(Character::isDigit)) {
            List<String> codes = catalogue.languageCodes();
            var paging = org.bibleplugin.spigot.gui.Paging.of(codes,
                    Integer.parseInt(ctx.next()) - 1, AVAILABLE_PAGE_SIZE);
            ctx.reply(Style.header("Languages"));
            for (String code : paging.items()) {
                List<CatalogueEntry> editions = catalogue.byLanguage(code);
                ctx.reply(Style.DIM + "  " + Style.AMBER + code + Style.DIM + " " + Style.ARROW + " " + Style.TEXT
                        + editions.get(0).language() + Style.DIM + " (" + editions.size() + ")");
            }
            ctx.reply(Style.MUTED + "Page " + (paging.page() + 1) + "/" + paging.pageCount() + " — "
                    + Style.AMBER + "/bible available <code>" + Style.MUTED + " lists editions.");
            return;
        }
        String code = ctx.hasNext() ? ctx.next().toLowerCase(Locale.ROOT) : null;
        List<CatalogueEntry> editions = code == null ? catalogue.all() : catalogue.byLanguage(code);
        if (editions.isEmpty()) {
            throw new BibleException(Problem.UNKNOWN_SOURCE, s.messages().get("bible.no-language", code));
        }
        ctx.reply(Style.header(code == null ? "All editions" : editions.get(0).language()));
        for (CatalogueEntry entry : editions) {
            String status = s.registry().find(entry.id()).isPresent() ? Style.SUCCESS + "✔"
                    : s.bibles().store().exists(entry.id()) ? Style.AMBER + "●" : Style.DIM + "⬇";
            String difficulty = entry.readability()
                    .map(r -> Style.difficultyBar(r.difficulty()) + " " + Style.MUTED + r.difficulty().label())
                    .orElse(Style.DIM + "—");
            ctx.reply(status + " " + Style.AMBER + entry.id() + Style.DIM + " " + Style.DOT + " " + Style.TEXT
                    + entry.name() + " " + difficulty);
        }
        ctx.reply(Style.DIM + "✔ loaded  ● installed  ⬇ download  — difficulty is measured for English only");
    }

    private static void downloaded(CommandContext ctx, PluginContext s) {
        List<String> installed = s.bibles().store().installed();
        if (installed.isEmpty()) {
            ctx.replyKey("downloaded.none");
            return;
        }
        ctx.reply(Style.header("Downloaded Bibles"));
        for (String id : installed) {
            boolean loaded = s.registry().find(id).isPresent();
            String name = s.bibles().catalogue().find(id).map(CatalogueEntry::name).orElse(id);
            ctx.reply((loaded ? Style.SUCCESS + "✔ " : Style.AMBER + "● ") + Style.AMBER + id
                    + Style.DIM + " " + Style.DOT + " " + Style.TEXT + name
                    + Style.DIM + (loaded ? "  loaded" : "  installed, not loaded"));
        }
    }

    private static void download(CommandContext ctx, PluginContext s) {
        String id = ctx.next().toLowerCase(Locale.ROOT);
        CatalogueEntry entry = s.bibles().catalogue().find(id).orElseThrow(() ->
                new BibleException(Problem.UNKNOWN_SOURCE, s.messages().get("bible.unknown", id)));
        ctx.replyKey("download.started", entry.name());

        var sender = ctx.sender();
        var progressMilestone = new AtomicInteger(-25);
        BibleDownloader.Progress progress = (done, total) -> {
            if (total <= 0) {
                return;
            }
            int percent = (int) (done * 100 / total);
            int milestone = percent / 25 * 25;
            if (milestone > progressMilestone.get() && milestone < 100) {
                progressMilestone.set(milestone);
                s.runSync(() -> sender.sendMessage(s.messages().prefixed("download.progress", milestone)));
            }
        };
        Feedback.report(s, sender, s.bibles().download(id, progress),
                bible -> s.messages().prefixed("download.done", bible.name(), bible.verseCount()),
                "download.failed");
    }

    private static void load(CommandContext ctx, PluginContext s) {
        String id = ctx.next().toLowerCase(Locale.ROOT);
        ctx.replyKey("load.started", id);
        Feedback.report(s, ctx.sender(), s.bibles().load(id),
                bible -> s.messages().prefixed("load.done", bible.name(), bible.verseCount()),
                "load.failed");
    }

    private static void unload(CommandContext ctx, PluginContext s) {
        String id = ctx.next().toLowerCase(Locale.ROOT);
        if (s.bibles().unload(id)) {
            ctx.replyKey("unload.done", id);
            s.play(ctx.sender() instanceof Player p ? p : null, UiSound.CLICK);
        } else {
            throw new BibleException(Problem.BIBLE_NOT_FOUND, s.messages().get("bible.not-loaded", id));
        }
    }

    private static void delete(CommandContext ctx, PluginContext s) {
        String id = ctx.next().toLowerCase(Locale.ROOT);
        Feedback.report(s, ctx.sender(), s.bibles().delete(id),
                deleted -> deleted
                        ? s.messages().prefixed("delete.done", id)
                        : s.messages().prefixed("bible.not-installed", id),
                "delete.failed");
    }

    private static void setDefault(CommandContext ctx, PluginContext s) {
        String id = ctx.next().toLowerCase(Locale.ROOT);
        if (s.registry().find(id).isEmpty()) {
            throw new BibleException(Problem.BIBLE_NOT_FOUND, s.messages().get("bible.not-loaded", id));
        }
        s.plugin().getConfig().set("bibles.default", id);
        s.plugin().saveConfig();
        s.registry().setDefaultBibleId(id);
        ctx.replyKey("default.set", id);
    }

    private static void info(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        Readability readability = s.registry().readability(bible.id()).orElse(null);
        CatalogueEntry entry = s.bibles().catalogue().find(bible.id()).orElse(null);
        ctx.reply(Style.header(bible.name()));
        ctx.reply(Style.MUTED + "ID " + Style.TEXT + bible.id().toUpperCase(Locale.ROOT));
        ctx.reply(Style.MUTED + "Language " + Style.TEXT + bible.info().language() + Style.DIM + " (" + bible.info().languageCode() + ")");
        ctx.reply(Style.MUTED + "Licence " + Style.TEXT + bible.info().license());
        if (entry != null && !entry.year().isEmpty()) {
            ctx.reply(Style.MUTED + "Edition year " + Style.TEXT + entry.year());
        }
        ctx.reply(Style.MUTED + "Books " + Style.TEXT + bible.bookCount() + Style.DIM + "   " + Style.MUTED
                + "Verses " + Style.TEXT + bible.verseCount());
        if (readability == null || readability.difficulty() == org.bibleplugin.core.readability.Difficulty.UNRATED) {
            ctx.reply(Style.MUTED + "Difficulty " + Style.DIM + "not rated");
        } else {
            ctx.reply(Style.MUTED + "Difficulty " + Style.difficultyBar(readability.difficulty()) + " "
                    + Style.TEXT + readability.difficulty().label() + Style.DIM + " — "
                    + readability.difficulty().description());
            ctx.reply(Style.DIM + "  grade " + Style.TEXT + String.format(Locale.ROOT, "%.1f", readability.gradeLevel())
                    + Style.DIM + "   reading ease " + Style.TEXT + String.format(Locale.ROOT, "%.1f", readability.readingEase()));
        }
    }

    private static void importFile(CommandContext ctx, PluginContext s) {
        String file = ctx.next();
        String id = ctx.next().toLowerCase(Locale.ROOT);
        String code = ctx.next().toLowerCase(Locale.ROOT);
        String name = ctx.restOr(id.toUpperCase(Locale.ROOT));
        String language = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH);
        BibleInfo info = new BibleInfo(id, name, code, language.isBlank() ? code : language, "User supplied");
        Feedback.report(s, ctx.sender(), s.bibles().importFile(file, info),
                bible -> s.messages().prefixed("import.done", bible.name(), bible.verseCount()),
                "import.failed");
    }

    private static void reload(CommandContext ctx, PluginContext s) {
        s.reload(Messages.load(s.plugin()));
        ctx.replyKey("reload.done");
    }

    private static List<String> languageCodes(PluginContext s) {
        return s.bibles().catalogue().languageCodes();
    }

    private static List<String> catalogueIds(PluginContext s) {
        return s.bibles().catalogue().all().stream().map(CatalogueEntry::id).toList();
    }

    private static List<String> installedIds(PluginContext s) {
        return new ArrayList<>(s.bibles().store().installed());
    }

    private static List<String> loadedIds(PluginContext s) {
        return new ArrayList<>(s.registry().loadedBibleIds());
    }
}
