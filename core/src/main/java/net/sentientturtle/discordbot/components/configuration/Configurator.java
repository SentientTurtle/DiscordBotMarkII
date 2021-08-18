package net.sentientturtle.discordbot.components.configuration;

import net.sentientturtle.discordbot.loader.StaticLoaded;
import net.sentientturtle.util.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * Component for providing dynamic runtime configuration of values<br>
 * Currently a stub that provides no functionality
 */
public class Configurator implements StaticLoaded {
    private static final HashMap<String, Tuple2<TypedSupplier<?>, TypedConsumer<?>>> configurables = new HashMap<>();

    public static <R, W> void addConfigurable(@NotNull String name, @Nullable TypedSupplier<R> reader, @Nullable TypedConsumer<W> setter) throws IllegalArgumentException {
        if (configurables.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate registered configurable: " + name);
        }
        configurables.put(name, new Tuple2<>(reader, setter));
    }
}
