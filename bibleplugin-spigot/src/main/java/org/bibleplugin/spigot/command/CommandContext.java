package org.bibleplugin.spigot.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.core.model.Bible;
import org.bibleplugin.spigot.PluginContext;

/**
 * Cursor over the arguments of one command invocation. Handlers consume arguments in order, and
 * the optional leading Bible id is taken with {@link #takeBibleOrDefault()}, so both
 * {@code /bible verse kjv John 3:16} and {@code /bible verse John 3:16} work.
 */
public final class CommandContext {

    private final CommandSender sender;
    private final String[] args;
    private final PluginContext services;
    private int cursor;

    public CommandContext(CommandSender sender, String[] args, PluginContext services) {
        this.sender = sender;
        this.args = args;
        this.services = services;
    }

    public CommandSender sender() {
        return sender;
    }

    public PluginContext services() {
        return services;
    }

    public boolean hasNext() {
        return cursor < args.length;
    }

    /** @return the next argument, consuming it. */
    public String next() {
        requireNext();
        return args[cursor++];
    }

    /** @return the next argument without consuming it, or {@code null} when none is left. */
    public String peek() {
        return hasNext() ? args[cursor] : null;
    }

    /**
     * @return all remaining arguments joined by single spaces, consuming them
     * @throws BibleException {@link Problem#EMPTY_QUERY} when nothing is left
     */
    public String rest() {
        requireNext();
        String joined = String.join(" ", Arrays.copyOfRange(args, cursor, args.length));
        cursor = args.length;
        return joined;
    }

    /** @return the remaining arguments, or {@code fallback} when there are none. */
    public String restOr(String fallback) {
        return hasNext() ? rest() : fallback;
    }

    /** @return the remaining arguments as a list without consuming them. */
    public List<String> remaining() {
        return List.of(Arrays.copyOfRange(args, cursor, args.length));
    }

    /** @return the player who ran the command.
     * @throws BibleException when the sender is the console */
    public Player player() {
        if (sender instanceof Player player) {
            return player;
        }
        throw new BibleException(Problem.FORMAT_ERROR, services.messages().get("error.player-only"));
    }

    /**
     * Consumes the next argument as a Bible id when it names a loaded Bible; otherwise uses the
     * default Bible and consumes nothing.
     *
     * @throws BibleException {@link Problem#BIBLE_NOT_FOUND} when no default is loaded
     */
    public Bible takeBibleOrDefault() {
        Optional<Bible> explicit = peekBible();
        if (explicit.isPresent()) {
            cursor++;
            return explicit.get();
        }
        return services.defaultBible();
    }

    /** @return the Bible named by the next argument, if it is loaded. Does not consume. */
    public Optional<Bible> peekBible() {
        String word = peek();
        if (word == null) {
            return Optional.empty();
        }
        return services.registry().find(word.toLowerCase(Locale.ROOT));
    }

    /** Sends a message to the sender, unprefixed. */
    public void reply(String message) {
        sender.sendMessage(message);
    }

    /** Sends a message with the plugin prefix. */
    public void replyKey(String key, Object... args) {
        sender.sendMessage(services.messages().prefixed(key, args));
    }

    private void requireNext() {
        if (!hasNext()) {
            throw new BibleException(Problem.EMPTY_QUERY, services.messages().get("error.missing-argument"));
        }
    }
}
