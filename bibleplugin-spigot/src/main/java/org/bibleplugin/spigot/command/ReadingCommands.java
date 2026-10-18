package org.bibleplugin.spigot.command;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.Player;
import org.bibleplugin.core.VersePicker;
import org.bibleplugin.core.book.TemporaryBookComposer;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.BibleBook;
import org.bibleplugin.core.model.Bookmark;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.core.model.VerseText;
import org.bibleplugin.core.reference.BookResolver;
import org.bibleplugin.core.reference.ReferenceParser;
import org.bibleplugin.persistence.store.PlayerRecord;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.gui.ReaderMenu;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Style;
import org.bibleplugin.spigot.text.VerseFormatter;

/** Reading commands: verses, the reader menu, temporary books, bookmarks and simple counts. */
public final class ReadingCommands implements CommandModule {

    private static final int VERSE_LINE_LIMIT = 40;
    private static final int BOOKS_PER_PAGE = 12;

    @Override
    public List<Subcommand> commands(PluginContext s) {
        return List.of(
                new Subcommand("verse", List.of("v"), "bible.use", "[bible] <reference>",
                        "Show a verse, a range or a whole chapter. e.g. John 3:16-18",
                        ctx -> verse(ctx, s), ctx -> referenceCompletions(ctx, s)),
                new Subcommand("read", List.of("open"), "bible.use", "[bible] [reference]",
                        "Open the reader menu at a chapter.",
                        ctx -> read(ctx, s), ctx -> referenceCompletions(ctx, s)),
                new Subcommand("continue", List.of("resume"), "bible.use", "",
                        "Resume where you stopped reading.",
                        ctx -> resume(ctx, s), Subcommand.Completer.NONE),
                new Subcommand("book", List.of("temp", "pages"), "bible.book", "[bible] <reference>",
                        "Get a temporary written book with the passage.",
                        ctx -> temporaryBook(ctx, s), ctx -> referenceCompletions(ctx, s)),
                new Subcommand("chapters", List.of(), "bible.use", "[bible] <book>",
                        "Count the chapters in a book.",
                        ctx -> chapters(ctx, s), ctx -> bookCompletions(ctx, s)),
                new Subcommand("verses", List.of(), "bible.use", "[bible] <book:chapter>",
                        "Count the verses in a chapter.",
                        ctx -> verses(ctx, s), ctx -> referenceCompletions(ctx, s)),
                new Subcommand("books", List.of(), "bible.use", "[bible] [page]",
                        "List every book of a Bible.",
                        ctx -> books(ctx, s), ctx -> bookCompletions(ctx, s)),
                new Subcommand("random", List.of("rand"), "bible.use", "[bible]",
                        "Show a random verse.",
                        ctx -> random(ctx, s), ctx -> loadedIds(s)),
                new Subcommand("daily", List.of("votd"), "bible.use", "[bible]",
                        "Show today's verse. The same for everyone today.",
                        ctx -> daily(ctx, s), ctx -> loadedIds(s)),
                new Subcommand("bookmark", List.of("bm"), "bible.use", "add [bible] <reference> | list | remove <n>",
                        "Save, list or remove bookmarks.",
                        ctx -> bookmark(ctx, s), ctx -> List.of("add", "list", "remove")),
                new Subcommand("loginverse", List.of(), "bible.use", "on | off",
                        "Turn the verse shown when you join on or off.",
                        ctx -> loginVerse(ctx, s), ctx -> List.of("on", "off")));
    }

    private static void verse(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        Reference reference = ReferenceParser.resolve(bible, ctx.rest());
        List<VerseText> passage = bible.passage(reference);
        String title = reference.format(bible.book(reference.book()).orElseThrow().name());
        ctx.reply(Style.header(title + " " + Style.DIM + Style.DOT + " " + Style.MUTED + bible.id().toUpperCase(Locale.ROOT)));
        VerseFormatter.lines(bible, passage, VERSE_LINE_LIMIT).forEach(ctx::reply);
        ctx.reply(Style.divider());
        s.play(ctx.sender() instanceof Player p ? p : null, UiSound.PAGE);
    }

    private static void read(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        Reference start = ctx.hasNext() ? ReferenceParser.resolve(bible, ctx.rest()) : Reference.ofChapter(1, 1);
        Player player = ctx.player();
        new ReaderMenu(s, bible.id(), start, null).open(player);
        s.play(player, UiSound.OPEN);
    }

    private static void resume(CommandContext ctx, PluginContext s) {
        Player player = ctx.player();
        PlayerRecord record = s.players().get(player.getUniqueId());
        if (record.lastReference() == null || record.lastBibleId() == null) {
            ctx.replyKey("continue.none");
            return;
        }
        Bible bible = s.registry().find(record.lastBibleId()).orElseThrow(() ->
                new BibleException(Problem.BIBLE_NOT_FOUND, s.messages().get("bible.not-loaded", record.lastBibleId())));
        new ReaderMenu(s, bible.id(), record.lastReference(), null).open(player);
        s.play(player, UiSound.OPEN);
    }

    private static void temporaryBook(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        Reference reference = ReferenceParser.resolve(bible, ctx.rest());
        Player player = ctx.player();
        String bookName = bible.book(reference.book()).orElseThrow().name();
        var layout = new TemporaryBookComposer.Layout(s.settings().bookCharsPerPage(), s.settings().bookMaxPages());
        s.async(() -> TemporaryBookComposer.compose(bible, reference, bookName, layout))
                .whenComplete((book, failure) -> s.runSync(() -> {
                    if (failure != null) {
                        ctx.replyKey("error.generic", Feedback.messageOf(failure));
                        return;
                    }
                    s.books().give(player, book);
                }));
    }

    private static void chapters(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        int number = new BookResolver(bible).resolve(ctx.rest());
        BibleBook book = bible.book(number).orElseThrow();
        ctx.replyKey("chapters.count", book.name(), book.chapterCount());
    }

    private static void verses(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        Reference reference = ReferenceParser.resolve(bible, ctx.rest());
        if (reference.isBook()) {
            throw new BibleException(Problem.INVALID_REFERENCE, s.messages().get("verses.need-chapter"));
        }
        BibleBook book = bible.book(reference.book()).orElseThrow();
        int count = book.chapter(reference.chapter()).verseCount();
        ctx.replyKey("verses.count", book.name(), reference.chapter(), count);
    }

    private static void books(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        int requested = ctx.hasNext() && ctx.peek().chars().allMatch(Character::isDigit)
                ? Integer.parseInt(ctx.next()) - 1 : 0;
        var paging = org.bibleplugin.spigot.gui.Paging.of(bible.books(), requested, BOOKS_PER_PAGE);
        ctx.reply(Style.header(bible.name() + " " + Style.DIM + Style.DOT + " " + Style.MUTED + "books"));
        for (BibleBook book : paging.items()) {
            ctx.reply(Style.DIM + String.format(Locale.ROOT, "%2d ", book.number()) + Style.TEXT + book.name()
                    + Style.DIM + "  " + book.chapterCount() + " ch");
        }
        ctx.reply(Style.MUTED + "Page " + (paging.page() + 1) + "/" + paging.pageCount()
                + Style.DIM + "  — /bible books " + bible.id() + " <page>");
    }

    private static void random(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        Reference reference = VersePicker.random(bible, ThreadLocalRandom.current());
        showSingle(ctx, bible, reference);
    }

    private static void daily(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        Reference reference = VersePicker.daily(bible, LocalDate.now());
        ctx.reply(Style.header("Verse of the day"));
        showSingle(ctx, bible, reference);
    }

    private static void showSingle(CommandContext ctx, Bible bible, Reference reference) {
        List<VerseText> passage = bible.passage(reference);
        VerseFormatter.lines(bible, passage, 1).forEach(ctx::reply);
    }

    private static void bookmark(CommandContext ctx, PluginContext s) {
        String action = ctx.hasNext() ? ctx.next().toLowerCase(Locale.ROOT) : "list";
        Player player = ctx.player();
        switch (action) {
            case "add" -> {
                Bible bible = ctx.takeBibleOrDefault();
                Reference reference = ReferenceParser.resolve(bible, ctx.rest());
                String label = reference.format(bible.book(reference.book()).orElseThrow().name());
                s.players().update(player.getUniqueId(), record -> record.withBookmark(
                        new Bookmark(label, bible.id(), reference, System.currentTimeMillis())));
                ctx.replyKey("bookmark.added", label);
            }
            case "remove" -> {
                int index = Integer.parseInt(ctx.next()) - 1;
                s.players().update(player.getUniqueId(), record -> record.withoutBookmark(index));
                ctx.replyKey("bookmark.removed", index + 1);
            }
            case "list" -> listBookmarks(ctx, s, player);
            default -> throw new BibleException(Problem.INVALID_REFERENCE, s.messages().get("bookmark.usage"));
        }
    }

    private static void listBookmarks(CommandContext ctx, PluginContext s, Player player) {
        List<Bookmark> marks = s.players().get(player.getUniqueId()).bookmarks();
        if (marks.isEmpty()) {
            ctx.replyKey("bookmark.none");
            return;
        }
        ctx.reply(Style.header("Bookmarks"));
        for (int i = 0; i < marks.size(); i++) {
            Bookmark mark = marks.get(i);
            ctx.reply(Style.AMBER + String.valueOf(i + 1) + Style.DIM + ". " + Style.TEXT + mark.label()
                    + Style.DIM + " (" + mark.bibleId().toUpperCase(Locale.ROOT) + ")");
        }
    }

    private static void loginVerse(CommandContext ctx, PluginContext s) {
        boolean enabled = switch (ctx.next().toLowerCase(Locale.ROOT)) {
            case "on", "true", "yes" -> true;
            case "off", "false", "no" -> false;
            default -> throw new BibleException(Problem.INVALID_REFERENCE, s.messages().get("loginverse.usage"));
        };
        Player player = ctx.player();
        s.players().update(player.getUniqueId(), record -> record.withLoginVerse(enabled));
        ctx.replyKey(enabled ? "loginverse.on" : "loginverse.off");
    }

    private static List<String> referenceCompletions(CommandContext ctx, PluginContext s) {
        return loadedIds(s);
    }

    private static List<String> bookCompletions(CommandContext ctx, PluginContext s) {
        return ctx.peekBible().or(() -> s.registry().defaultBibleId().flatMap(s.registry()::find))
                .map(bible -> bible.books().stream().map(BibleBook::name).toList())
                .orElse(List.of());
    }

    private static List<String> loadedIds(PluginContext s) {
        return List.copyOf(s.registry().loadedBibleIds());
    }
}
