package org.bibleplugin.spigot.text;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Player-facing text loaded from {@code messages.yml}. Keys are dotted paths; {@code {0}},
 * {@code {1}}... are replaced by arguments. {@code &} colour codes are translated when loaded.
 */
public final class Messages {

    private final Map<String, String> table;

    private Messages(Map<String, String> table) {
        this.table = Map.copyOf(table);
    }

    /** Loads the file from the plugin folder, falling back to the bundled defaults for missing keys. */
    public static Messages load(JavaPlugin plugin) {
        plugin.saveResource("messages.yml", false);
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(plugin.getResource("messages.yml"), java.nio.charset.StandardCharsets.UTF_8));
        YamlConfiguration custom = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));

        Map<String, String> table = new HashMap<>();
        for (String key : defaults.getKeys(true)) {
            if (defaults.isString(key)) {
                String value = custom.isString(key) ? custom.getString(key) : defaults.getString(key);
                table.put(key, ChatColor.translateAlternateColorCodes('&', value == null ? "" : value));
            }
        }
        return new Messages(table);
    }

    /** @return the formatted message, or the key itself when it is missing. */
    public String get(String key, Object... args) {
        String template = table.getOrDefault(key, key);
        String out = template;
        for (int i = 0; i < args.length; i++) {
            out = out.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return out;
    }

    /** @return the message prefixed with the plugin's chat prefix. */
    public String prefixed(String key, Object... args) {
        return get("prefix") + get(key, args);
    }
}
