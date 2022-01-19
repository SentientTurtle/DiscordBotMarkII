package net.sentientturtle.discordbot.loader;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.module.BotModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dynamically discovers, loads and initializes classes.
 */
public class Loader {
    private static final Logger logger = LoggerFactory.getLogger(Loader.class);
    private static final AtomicBoolean isLoading = new AtomicBoolean(false);
    private static boolean hasScanned = false;
    private static List<String> staticLoaded;
    private static List<String> modules;

    static {
        ensureStaticLoad();
    }

    private static void scan() {
        try (
                ScanResult result = new ClassGraph()
                                            .enableClassInfo()
                                            .scan()
        ) {
            staticLoaded = result.getClassesImplementing(StaticLoaded.class.getCanonicalName())
                                   .stream()
                                   .filter(classInfo -> Arrays.asList(classInfo.loadClass().getInterfaces()).contains(StaticLoaded.class))
                                   .map(ClassInfo::getName)
                                   .peek(name -> logger.debug("Discovered StaticLoaded class " + name))
                                   .collect(Collectors.toList());

            modules = result.getSubclasses(BotModule.class.getCanonicalName())
                              .stream()
                              .map(ClassInfo::getName)
                              .peek(name -> logger.debug("Discovered Module " + name))
                              .collect(Collectors.toList());

            hasScanned = true;
        }
    }

    public static void ensureStaticLoad() throws StaticInitException {
        if (isLoading.compareAndSet(false, true)) {
            logger.debug("Now loading StaticLoaded classes...");
            var classLoader = Loader.class.getClassLoader();
            if (!hasScanned) scan();
            for (String name : staticLoaded) {
                try {
                    logger.debug("Attempting static initialization of " + name);
                    Class.forName(name, true, classLoader);
                } catch (Throwable t) {
                    throw new StaticInitException("Could not load class: " + name, t);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Stream<Class<? extends BotModule>> getModuleClasses(boolean initialize) throws StaticInitException {
        if (!hasScanned) scan();
        var classLoader = Loader.class.getClassLoader();
        return modules.stream()
                       .map(name -> {
                           try {
                               return (Class<? extends BotModule>) Class.forName(name, initialize, classLoader);
                           } catch (ClassNotFoundException | ClassCastException | ExceptionInInitializerError e) {
                               logger.info("Could not load class", e);
                               return (Class<? extends BotModule>) null;
                           }
                       })
                       .filter(Objects::nonNull);
    }
}
