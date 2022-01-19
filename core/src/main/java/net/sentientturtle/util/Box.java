package net.sentientturtle.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * Mutable single-element collection <br>
 * Intended primary for memoization and interior mutability
 * @param <E> Element type
 */
public class Box<E> implements Collection<E>, RandomAccess {
    private @Nullable E value;

    /**
     * Constructs an empty box
     */
    public Box() {
        this.value = null;
    }

    /**
     * Static method equivalent of {@link #Box()}
     * @param <E> Element type
     * @return A new empty box
     */
    public static <E> Box<E> empty() {
        return new Box<>();
    }

    /**
     * Constructs a box with the specified value, or empty if the value is null
     * @param value Value for box
     */
    public Box(@Nullable E value) {
        this.value = value;
    }

    /**
     * Static method equivalent for {@link #Box(E)}
     * @param value Value inside box
     * @param <E> Element type
     * @return New box containing the specified value, or empty if the value is null
     */
    public static <E> Box<E> of(@Nullable E value) {
        return new Box<>(value);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Box(Optional<E> value) {
        this.value = value.orElse(null);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <E> Box<E> fromOptional(Optional<E> value) {
        return new Box<>(value);
    }

    public @Nullable E get() {
        return value;
    }

    public Optional<E> asOptional() {
        return Optional.ofNullable(value);
    }

    public void setValue(@Nullable E value) {
        this.value = value;
    }

    public boolean setIfEmpty(@Nullable E value) {
        if (this.value != null) {
            return false;
        } else {
            this.value = value;
            return true;
        }
    }
    public E computeIfEmpty(Supplier<E> valueSupplier) {
        if (value != null) {
            return value;
        } else {
            E newValue = valueSupplier.get();
            value = Objects.requireNonNull(newValue);
            return newValue;
        }
    }

    /* Collection methods */

    @Override
    public int size() {
        return value == null ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public boolean contains(Object o) {
        Objects.requireNonNull(o);
        return value != null && value.equals(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            boolean hasNext = value != null;
            @Nullable E retrievedValue = null;

            @Override
            public boolean hasNext() {
                return hasNext && value != null;
            }

            @Override
            public E next() {
                if (!hasNext) throw new NoSuchElementException();
                hasNext = false;
                if (value == null) throw new ConcurrentModificationException();
                retrievedValue = value;
                return value;
            }

            @Override
            public void remove() {
                if (retrievedValue == null || value == null) throw new IllegalStateException();
                value = null;
            }
        };
    }

    @NotNull
    @Override
    public Object[] toArray() {
        if (value != null) {
            return new Object[]{value};
        } else {
            return new Object[0];
        }
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(@NotNull T[] a) {
        if (value != null) {
            if (a.length > 0) {
                ((Object[]) a)[0] = value;
                return a;
            } else {
                return (T[]) Arrays.copyOf(new Object[]{value}, 1, a.getClass());
            }
        } else {
            return a;
        }
    }

    @Override
    public boolean add(E e) {
        if (value != null) throw new IllegalStateException("Box already contains a value");
        this.value = Objects.requireNonNull(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        Objects.requireNonNull(o);
        if (value != null && this.contains(o)) {
            this.value = null;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        if (value != null || c.size() > 1) throw new IllegalStateException();
        return c.stream().map(this::add).reduce(false, Boolean::logicalOr);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return c.stream().map(this::remove).reduce(false, Boolean::logicalOr);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        if (value != null) {
            if (c.contains(value)) {
                return false;
            } else {
                value = null;
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        value = null;
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(
                this,
                Spliterator.DISTINCT |
                Spliterator.ORDERED |
                Spliterator.SORTED |
                Spliterator.SIZED |
                Spliterator.SUBSIZED |
                Spliterator.NONNULL
        );
    }
}
