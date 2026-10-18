package org.bibleplugin.persistence.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores one JSON file per player under {@code players/<uuid>.json}.
 *
 * <p>Records are cached in memory after their first read. {@link #update} applies a change and
 * writes it to disk atomically. Callers that run on the server thread should hand the write to an
 * executor; this class itself does no scheduling.
 */
public final class PlayerDataStore {

    private static final Logger LOG = Logger.getLogger(PlayerDataStore.class.getName());

    private final Path directory;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private final ConcurrentHashMap<UUID, PlayerRecord> cache = new ConcurrentHashMap<>();

    public PlayerDataStore(Path directory) {
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public PlayerRecord get(UUID player) {
        return cache.computeIfAbsent(player, this::readOrDefault);
    }

    /**
     * Applies {@code change} to the player's record and persists the result.
     *
     * @return the updated record
     */
    public PlayerRecord update(UUID player, UnaryOperator<PlayerRecord> change) {
        PlayerRecord updated = cache.compute(player, (id, current) ->
                change.apply(current == null ? readOrDefault(id) : current));
        write(player, updated);
        return updated;
    }

    /** Forgets the cached copy so the next {@link #get} re-reads the file. */
    public void evict(UUID player) {
        cache.remove(player);
    }

    private PlayerRecord readOrDefault(UUID player) {
        Path file = fileFor(player);
        if (!Files.isRegularFile(file)) {
            return PlayerRecord.defaults();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            PlayerRecord record = gson.fromJson(json, PlayerRecord.class);
            return record == null ? PlayerRecord.defaults() : record;
        } catch (IOException | JsonParseException e) {
            LOG.log(Level.WARNING, "Could not read player data for " + player + ", using defaults.", e);
            return PlayerRecord.defaults();
        }
    }

    private void write(UUID player, PlayerRecord record) {
        Path target = fileFor(player);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, gson.toJson(record), StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save player data for " + player, e);
        }
    }

    private Path fileFor(UUID player) {
        return directory.resolve(player + ".json");
    }
}
