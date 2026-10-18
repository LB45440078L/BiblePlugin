package org.bibleplugin.spigot.command;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.core.model.Reference;
import org.bibleplugin.core.reference.ReferenceParser;
import org.bibleplugin.core.search.SearchEngine;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.gui.ResultsMenu;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Style;
import org.bibleplugin.spigot.text.TextWrap;

/**
 * Searching commands. A scope can be added with {@code in}, for example
 * {@code /bible occurrences kjv love in John 3} counts "love" in John chapter 3 only.
 */
public final class SearchCommands implements CommandModule {

    private static final Pattern SCOPE = Pattern.compile("^(.*?)\\s+in\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final int FIND_LINE_LIMIT = 8;

    @Override
    public List<Subcommand> commands(PluginContext s) {
        return List.of(
                new Subcommand("occurrences", List.of("count"), "bible.use", "[bible] <word> [in <reference>]",
                        "Count how often a word appears.",
                        ctx -> occurrences(ctx, s), ctx -> List.of("in")),
                new Subcommand("find", List.of("search-chat"), "bible.use", "[bible] <phrase> [in <reference>]",
                        "List verses that contain a phrase.",
                        ctx -> find(ctx, s), ctx -> List.of("in")),
                new Subcommand("search", List.of("s"), "bible.use", "[bible]",
                        "Search with an anvil prompt and browse results in a menu.",
                        ctx -> search(ctx, s), ctx -> List.copyOf(s.registry().loadedBibleIds())));
    }

    private static void occurrences(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        Query query = Query.parse(bible, ctx.rest());
        s.async(() -> s.registry().search().countOccurrences(bible, query.scope(), query.text()))
                .whenComplete((count, failure) -> s.runSync(() -> {
                    if (failure != null) {
                        ctx.replyKey("error.generic", Feedback.messageOf(failure));
                        return;
                    }
                    ctx.replyKey("occurrences.count", query.text(), count, query.describeScope(bible));
                    s.play(ctx.sender() instanceof org.bukkit.entity.Player p ? p : null, UiSound.SUCCESS);
                }));
    }

    private static void find(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        Query query = Query.parse(bible, ctx.rest());
        s.async(() -> s.registry().search().search(bible, query.scope(), query.text(), s.settings().maxSearchResults()))
                .whenComplete((result, failure) -> s.runSync(() -> {
                    if (failure != null) {
                        ctx.replyKey("error.generic", Feedback.messageOf(failure));
                        return;
                    }
                    if (result.matchingVerses() == 0) {
                        ctx.replyKey("find.none", query.text());
                        return;
                    }
                    ctx.reply(Style.header("“" + query.text() + "” " + Style.DIM + Style.DOT + " " + Style.MUTED
                            + result.matchingVerses() + " verses"));
                    int shown = 0;
                    for (SearchEngine.Hit hit : result.hits()) {
                        if (shown++ == FIND_LINE_LIMIT) {
                            break;
                        }
                        String name = bible.book(hit.reference().book()).orElseThrow().name();
                        List<String> wrapped = TextWrap.wrap(hit.text(), 50);
                        ctx.reply(Style.GOLD + hit.reference().format(name) + Style.DIM + " " + Style.ARROW + " " + Style.TEXT
                                + (wrapped.isEmpty() ? "" : wrapped.get(0)));
                        for (int i = 1; i < wrapped.size(); i++) {
                            ctx.reply("    " + Style.TEXT + wrapped.get(i));
                        }
                    }
                    if (result.hits().size() > FIND_LINE_LIMIT) {
                        ctx.reply(Style.DIM + "… " + (result.hits().size() - FIND_LINE_LIMIT) + " more. Use "
                                + Style.AMBER + "/bible search" + Style.DIM + " to browse them all.");
                    }
                    ctx.reply(Style.divider());
                }));
    }

    private static void search(CommandContext ctx, PluginContext s) {
        Bible bible = ctx.takeBibleOrDefault();
        var player = ctx.player();
        s.prompts().requestText(player, "Search " + bible.id().toUpperCase(java.util.Locale.ROOT), "", query ->
                s.async(() -> s.registry().search().search(bible, Optional.empty(), query, s.settings().maxSearchResults()))
                        .whenComplete((result, failure) -> s.runSync(() -> {
                            if (failure != null) {
                                s.play(player, UiSound.ERROR);
                                player.sendMessage(s.messages().prefixed("search.error", Feedback.messageOf(failure)));
                                return;
                            }
                            new ResultsMenu(s, bible.id(), query, result, null).open(player);
                        })));
    }

    /** A parsed "[text] [in scope]" argument pair. */
    private record Query(String text, Optional<Reference> scope, String scopeLabel) {

        static Query parse(Bible bible, String raw) {
            Matcher matcher = SCOPE.matcher(raw.strip());
            if (!matcher.matches()) {
                return new Query(raw.strip(), Optional.empty(), null);
            }
            Reference reference = ReferenceParser.resolve(bible, matcher.group(2));
            String label = reference.format(bible.book(reference.book()).orElseThrow().name());
            return new Query(matcher.group(1).strip(), Optional.of(reference), label);
        }

        String describeScope(Bible bible) {
            return scopeLabel == null ? bible.name() : scopeLabel;
        }
    }
}
