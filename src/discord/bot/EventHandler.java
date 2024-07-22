package discord.bot;

import main.Session;
import org.jetbrains.annotations.NotNull;

/**
 * An implementation of {@link java.util.function.BiConsumer} that doesn't handle its own exceptions.
 * Useful for declaring throwing lambda expressions.
 * <p>
 * Example EventHandler lambda expression:
 * <code>(event, session) -> { event.reply("Command received").queue(); }</code>
 */
public interface EventHandler<T> {
    void accept(@NotNull T event, @NotNull Session session) throws Exception;
}
