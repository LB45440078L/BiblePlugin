package org.bibleplugin.persistence.store;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;
import org.bibleplugin.persistence.binary.BibleCodec;
import org.bibleplugin.persistence.binary.StoredBible;

/**
 * Keeps installed Bibles as {@code <id>.bpb} files in one directory. Writes go to a temporary file
 * first and are then moved into place, so a crash never corrupts an existing Bible.
 */
public final class BibleFileStore {

    private static final String EXTENSION = ".bpb";
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

    private final Path directory;

    public BibleFileStore(Path directory) {
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path directory() {
        return directory;
    }

    public boolean exists(String id) {
        return Files.isRegularFile(fileFor(id));
    }

    public void save(StoredBible stored) {
        String id = stored.bible().id();
        Path target = fileFor(id);
        Path temporary = directory.resolve(id + EXTENSION + ".tmp");
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(temporary), 1 << 16)) {
            BibleCodec.write(stored, out);
        } catch (IOException e) {
            throw new BibleException(Problem.STORAGE_ERROR, "Could not save " + id + ": " + e.getMessage(), e);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new BibleException(Problem.STORAGE_ERROR, "Could not finalise " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * @throws BibleException {@link Problem#BIBLE_NOT_FOUND} when the id is not installed
     */
    public StoredBible load(String id) {
        Path file = fileFor(id);
        if (!Files.isRegularFile(file)) {
            throw new BibleException(Problem.BIBLE_NOT_FOUND, "The Bible " + id + " is not downloaded yet.");
        }
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file), 1 << 16)) {
            return BibleCodec.read(in);
        } catch (IOException e) {
            throw new BibleException(Problem.STORAGE_ERROR, "Could not read " + id + ": " + e.getMessage(), e);
        }
    }

    /** @return ids of every installed Bible, sorted. */
    public List<String> installed() {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(EXTENSION))
                    .map(name -> name.substring(0, name.length() - EXTENSION.length()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Optional<Path> fileOf(String id) {
        Path file = fileFor(id);
        return Files.isRegularFile(file) ? Optional.of(file) : Optional.empty();
    }

    /** @return {@code true} when a file was removed. */
    public boolean delete(String id) {
        try {
            return Files.deleteIfExists(fileFor(id));
        } catch (IOException e) {
            throw new BibleException(Problem.STORAGE_ERROR, "Could not delete " + id + ": " + e.getMessage(), e);
        }
    }

    /** Resolves an id to its file, rejecting anything that could escape the directory. */
    Path fileFor(String id) {
        String normalised = id == null ? "" : id.toLowerCase(Locale.ROOT);
        if (!VALID_ID.matcher(normalised).matches()) {
            throw new BibleException(Problem.BIBLE_NOT_FOUND, "\"" + id + "\" is not a valid Bible id.");
        }
        return directory.resolve(normalised + EXTENSION);
    }
}
