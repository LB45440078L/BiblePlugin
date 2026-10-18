package org.bibleplugin.spigot.command;

import java.util.List;
import org.bibleplugin.spigot.PluginContext;

/** A themed group of {@link Subcommand}s, registered with {@link BibleCommand}. */
public interface CommandModule {

    List<Subcommand> commands(PluginContext services);
}
