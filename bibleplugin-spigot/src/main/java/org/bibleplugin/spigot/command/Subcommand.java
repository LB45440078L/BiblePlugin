package org.bibleplugin.spigot.command;

import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * One {@code /bible <name>} action. Each instance is a self-contained command object: its name,
 * permission, help text, behaviour and tab completion travel together.
 *
 * @param name       the word after {@code /bible}
 * @param aliases    alternative words that run the same action
 * @param permission the permission node, or {@code null} for everyone
 * @param usage      argument syntax shown in help, e.g. {@code verse [bible] <reference>}
 * @param summary    one-line description shown in help
 * @param handler    what the command does
 * @param completer  tab completion for the arguments after the name
 */
public record Subcommand(
        String name,
        List<String> aliases,
        String permission,
        String usage,
        String summary,
        Handler handler,
        Completer completer) {

    /** Executes the command. Throw {@link org.bibleplugin.core.exception.BibleException} to report a problem. */
    @FunctionalInterface
    public interface Handler {
        void run(CommandContext context);
    }

    /** Suggests arguments for tab completion. */
    @FunctionalInterface
    public interface Completer {
        List<String> complete(CommandContext context);

        Completer NONE = context -> List.of();
    }

    public Subcommand {
        aliases = List.copyOf(aliases);
        completer = completer == null ? Completer.NONE : completer;
    }

    public boolean matches(String word) {
        return name.equalsIgnoreCase(word) || aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(word));
    }

    public boolean allowedFor(CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }
}
