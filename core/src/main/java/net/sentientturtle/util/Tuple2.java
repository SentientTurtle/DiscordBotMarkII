package net.sentientturtle.util;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 2-value tuple data class
 * @param <T> Type one
 * @param <U> Type two
 */
public class Tuple2<T, U> {
    public final @Nullable T one;
    public final @Nullable U two;

    public Tuple2(@Nullable T one, @Nullable U two) {
        this.one = one;
        this.two = two;
    }

    public static <T, U> Tuple2<T, U> of(T one, U two) {
        return new Tuple2<>(one, two);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
        return Objects.equals(one, tuple2.one) &&
                Objects.equals(two, tuple2.two);
    }

    @Override
    public int hashCode() {
        return Objects.hash(one, two);
    }
}
