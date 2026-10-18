package org.bibleplugin.spigot.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.sound.UiSound;
import org.bibleplugin.spigot.text.Style;

/**
 * The {@code /bible} entry point. Routes the first argument to a {@link Subcommand} from the
 * registered {@link CommandModule}s, checks permissions, reports errors in a consistent style,
 * and offers tab completion.
 */
public final class BibleCommand implements CommandExecutor, TabCompleter {

    private final PluginContext services;
    private final Map<String, Subcommand> byWord = new LinkedHashMap<>();
    private final List<Subcommand> commands = new ArrayList<>();

    public BibleCommand(PluginContext services, Collection<? extends CommandModule> modules) {
        this.services = services;
        for (CommandModule module : modules) {
            for (Subcommand command : module.commands(services)) {
                commands.add(command);
                byWord.put(command.name(), command);
                command.aliases().forEach(alias -> byWord.put(alias, command));
            }
        }
    }

    /** @return every registered subcommand, in registration order. */
    public List<Subcommand> commands() {
        return List.copyOf(commands);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            printHelp(sender);
            return true;
        }
        Subcommand subcommand = byWord.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            sender.sendMessage(services.messages().prefixed("error.unknown", args[0]));
            services.play(sender instanceof org.bukkit.entity.Player p ? p : null, UiSound.ERROR);
            return true;
        }
        if (!subcommand.allowedFor(sender)) {
            sender.sendMessage(services.messages().prefixed("error.no-permission"));
            services.play(sender instanceof org.bukkit.entity.Player p ? p : null, UiSound.ERROR);
            return true;
        }

        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        CommandContext context = new CommandContext(sender, rest, services);
        try {
            subcommand.handler().run(context);
        } catch (BibleException problem) {
            sender.sendMessage(services.messages().prefixed("error.generic", problem.getMessage()));
            services.play(sender instanceof org.bukkit.entity.Player p ? p : null, UiSound.ERROR);
        } catch (RuntimeException unexpected) {
            services.plugin().getLogger().log(Level.SEVERE, "Error while running /bible " + args[0], unexpected);
            sender.sendMessage(services.messages().prefixed("error.internal"));
        }
        return true;
    }

    /** Prints the help screen: every command the sender may use, with its usage and summary. */
    private void printHelp(CommandSender sender) {
        sender.sendMessage(Style.header("BiblePlugin"));
        sender.sendMessage(Style.MUTED + "Read, search and explore the Bible. The Bible name is optional when a default is set.");
        sender.sendMessage("");
        for (Subcommand command : commands) {
            if (command.allowedFor(sender)) {
                sender.sendMessage(usageLine(command));
                sender.sendMessage("  " + Style.DIM + Style.ARROW + " " + Style.TEXT + command.summary());
            }
        }
        sender.sendMessage(Style.divider());
        sender.sendMessage(Style.DIM + "Tip: " + Style.MUTED + "use " + Style.AMBER + "/bible read" + Style.MUTED
                + " to browse. Click a verse to share it, right-click to bookmark.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> words = new ArrayList<>(List.of("help"));
            commands.stream()
                    .filter(c -> c.allowedFor(sender))
                    .forEach(c -> {
                        words.add(c.name());
                        words.addAll(c.aliases());
                    });
            return filter(words, args[0]);
        }
        Subcommand subcommand = byWord.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null || !subcommand.allowedFor(sender)) {
            return List.of();
        }
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        CommandContext context = new CommandContext(sender, rest, services);
        return filter(subcommand.completer().complete(context), rest.length == 0 ? "" : rest[rest.length - 1]);
    }

    static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }

    /** Exposed for the help command so it can show a consistent usage line. */
    static String usageLine(Subcommand command) {
        return Style.AMBER + "/bible " + command.name() + (command.usage().isEmpty() ? "" : " " + Style.MUTED + command.usage());
    }
}
