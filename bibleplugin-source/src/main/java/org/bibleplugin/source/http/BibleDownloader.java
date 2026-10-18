package org.bibleplugin.source.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.zip.GZIPInputStream;
import org.bibleplugin.core.exception.BibleException;
import org.bibleplugin.core.exception.BibleException.Problem;

/**
 * Downloads Bible files with retries, gzip support and atomic writes: data goes to a
 * {@code .part} file that is renamed only once the transfer completed, so a crash never leaves a
 * half-written Bible behind. Safe to call from any thread; blocking, so run it off the main thread.
 */
public final class BibleDownloader {

    /** Receives transfer progress. {@code totalBytes} is -1 when the server did not say. */
    @FunctionalInterface
    public interface Progress {
        Progress NONE = (downloaded, totalBytes) -> { };

        void update(long downloadedBytes, long totalBytes);
    }

    private static final String USER_AGENT = "BiblePlugin/2.0";
    private static final int BUFFER_SIZE = 1 << 16;

    private final HttpClient client;
    private final Duration timeout;
    private final int maxAttempts;

    public BibleDownloader(Duration timeout, int maxAttempts) {
        this.timeout = timeout;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Downloads {@code uri} into {@code target}, replacing any existing file.
     *
     * @return {@code target}
     * @throws BibleException {@link Problem#DOWNLOAD_FAILED} once every attempt has failed
     */
    public Path download(URI uri, Path target, Progress progress) {
        Path part = target.resolveSibling(target.getFileName() + ".part");
        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new BibleException(Problem.STORAGE_ERROR, "Cannot create " + target.getParent(), e);
        }

        IOException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                transfer(uri, part, progress == null ? Progress.NONE : progress);
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return target;
            } catch (IOException e) {
                last = e;
                backoff(attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BibleException(Problem.DOWNLOAD_FAILED, "Download interrupted.", e);
            }
        }
        throw new BibleException(Problem.DOWNLOAD_FAILED,
                "Could not download " + uri + " after " + maxAttempts + " attempts: "
                        + (last == null ? "unknown error" : last.getMessage()), last);
    }

    private void transfer(URI uri, Path part, Progress progress) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("HTTP " + response.statusCode());
        }
        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        boolean gzipped = response.headers().firstValue("Content-Encoding")
                .map(value -> value.equalsIgnoreCase("gzip")).orElse(false);

        try (InputStream raw = response.body();
             InputStream body = gzipped ? new GZIPInputStream(raw, BUFFER_SIZE) : raw;
             OutputStream out = Files.newOutputStream(part)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long done = 0;
            int read;
            while ((read = body.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                done += read;
                progress.update(done, total);
            }
        }
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
