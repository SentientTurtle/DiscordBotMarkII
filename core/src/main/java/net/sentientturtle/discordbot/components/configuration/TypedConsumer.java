package net.sentientturtle.discordbot.components.configuration;

import java.util.function.Consumer;

/**
 * {@link Consumer} that retains its type at runtime
 * @param <T> Input type
 */
public interface TypedConsumer<T> extends Consumer<T> {
    Class<T> getType();

    static <T> TypedConsumer<T> fromConsumer(Class<T> tClass, Consumer<T> consumer) {
        return new TypedConsumer<>() {
            @Override
            public void accept(T t) {
                consumer.accept(t);
            }

            @Override
            public Class<T> getType() {
                return tClass;
            }
        };
    }
}
