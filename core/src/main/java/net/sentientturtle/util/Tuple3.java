package net.sentientturtle.util;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 3-value tuple data class
 * @param <T> Type one
 * @param <U> Type two
 * @param <V> Type three
 */
public class Tuple3<T, U, V> {
    public final @Nullable T one;
    public final @Nullable U two;
    public final @Nullable V three;

    public Tuple3(@Nullable T one, @Nullable U two, @Nullable V three) {
        this.one = one;
        this.two = two;
        this.three = three;
    }

    public static <T, U, V> Tuple3<T, U, V> of(T one, U two, V three) {
        return new Tuple3<>(one, two, three);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
        return Objects.equals(one, tuple3.one) && Objects.equals(two, tuple3.two) && Objects.equals(three, tuple3.three);
    }

    @Override
    public int hashCode() {
        return Objects.hash(one, two, three);
    }
}
