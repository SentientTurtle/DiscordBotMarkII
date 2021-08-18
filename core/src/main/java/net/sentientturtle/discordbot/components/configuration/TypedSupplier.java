package net.sentientturtle.discordbot.components.configuration;

import java.util.function.Supplier;

/**
 * {@link Supplier} that retains its type at runtime
 * @param <T> Result type
 */
public interface TypedSupplier<T> extends Supplier<T> {
    Class<T> getType();

    static <T> TypedSupplier<T> fromSupplier(Class<T> tClass, Supplier<T> supplier) {
        return new TypedSupplier<T>() {
            @Override
            public Class<T> getType() {
                return tClass;
            }

            @Override
            public T get() {
                return supplier.get();
            }
        };
    }
}
