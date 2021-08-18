package net.sentientturtle.discordbot.components.healthcheck;

import net.sentientturtle.discordbot.loader.StaticLoaded;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Bot health subsystem; Enables classes/objects to register a health-check function
 */
public class HealthCheck implements StaticLoaded {
    private static final HashMap<Class<?>, HealthUpdateSupplier> static_map = new HashMap<>();
    private static final HashMap<Object, HealthUpdateSupplier> instance_map = new HashMap<>();

    public static void addStatic(Class<?> clazz, Supplier<HealthStatus> statusSupplier) {
        addStatic(clazz, statusSupplier, Optional::empty);
    }

    public static void addStatic(Class<?> clazz, Supplier<HealthStatus> statusSupplier, Supplier<Optional<String>> statusMessageSupplier) {
        static_map.put(clazz, new HealthUpdateSupplier(statusSupplier, statusMessageSupplier));
    }
    public static void addInstance(Object object, Supplier<HealthStatus> statusSupplier) {
        instance_map.put(object, new HealthUpdateSupplier(statusSupplier, Optional::empty));
    }
    public static void addInstance(Object object, Supplier<HealthStatus> statusSupplier, Supplier<Optional<String>> statusMessageSupplier) {
        instance_map.put(object, new HealthUpdateSupplier(statusSupplier, statusMessageSupplier));
    }

    private static class HealthUpdateSupplier {
        public Supplier<HealthStatus> statusSupplier;
        public Supplier<Optional<String>> statusMessageSupplier;

        public HealthUpdateSupplier(Supplier<HealthStatus> statusSupplier, Supplier<Optional<String>> statusMessageSupplier) {
            this.statusSupplier = statusSupplier;
            this.statusMessageSupplier = statusMessageSupplier;
        }
    }
}
