package org.bibleplugin.spigot.command;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bibleplugin.spigot.PluginContext;
import org.bibleplugin.spigot.sound.UiSound;

/** Turns the outcome of a background task into a chat message and sound, on the server thread. */
final class Feedback {

    private Feedback() {
    }

    /**
     * @param success   builds the success message from the result
     * @param failedKey message key used when the task fails; receives the failure text
     */
    static <T> void report(PluginContext services, CommandSender sender, CompletableFuture<T> future,
                           Function<T, String> success, String failedKey) {
        future.whenComplete((result, failure) -> services.runSync(() -> {
            Player player = sender instanceof Player p ? p : null;
            if (failure != null) {
                services.play(player, UiSound.ERROR);
                sender.sendMessage(services.messages().prefixed(failedKey, messageOf(failure)));
                return;
            }
            services.play(player, UiSound.SUCCESS);
            sender.sendMessage(success.apply(result));
        }));
    }

    /** @return the most useful text for an exception, unwrapping async wrappers. */
    static String messageOf(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof java.util.concurrent.ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof CancellationException) {
            return "cancelled";
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
