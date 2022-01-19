package net.sentientturtle.discordbot.components.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.core.Shutdown;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static net.sentientturtle.discordbot.components.persistence.PersistenceException.ObjectLoadFailed;
import static net.sentientturtle.discordbot.components.persistence.PersistenceException.ResourceLoadFailed;

/**
 * Class to handle data persistence.<br>
 * Dumps and loads data to yaml files, for more robust storage use Database module.
 */
public class Persistence implements StaticLoaded {
    private static final Logger logger = LoggerFactory.getLogger(Persistence.class);
    private static final File dataFolder;
    private static final File resourceFolder;

    private static final ObjectMapper yamlMapper;
    private static final HashMap<Class<? extends PersistentObject>, PersistentObject> objectCache = new HashMap<>();
    private static final ScheduledFuture<?> saveDaemon;
    private static boolean failedSave = false;

    static {
        String dataFolderPath = System.getProperty("net.sentientturtle.discordbot.datafolderpath");
        if (dataFolderPath == null) {
            logger.info("Persistence data folder path system variable (net.sentientturtle.discordbot.datafolderpath) not set, defaulting to './data'");
            dataFolderPath = "./data";
        }
        dataFolder = new File(dataFolderPath);
        if (!((dataFolder.exists() && dataFolder.isDirectory()) || dataFolder.mkdir())) {
            throw new StaticInitException("Could not create data folder: " + dataFolder);
        }

        String resourcePath = System.getProperty("net.sentientturtle.discordbot.resourcefolderpath");
        if (resourcePath == null) {
            logger.info("Persistence resource folder path system variable (net.sentientturtle.discordbot.resourcefolderpath) not set, defaulting to './resource'");
            resourcePath = "./resource";
        }
        resourceFolder = new File(resourcePath);
        if (!((resourceFolder.exists() && resourceFolder.isDirectory()) || resourceFolder.mkdir())) {
            throw new StaticInitException("Could not create resource folder: " + resourceFolder);
        }

        yamlMapper = new ObjectMapper(
                new YAMLFactory()
                        .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                        .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
        );
        saveDaemon = Scheduling.scheduleAtFixedRate(Persistence::save, 0, 1, TimeUnit.HOURS);

        Shutdown.registerHook(() -> saveDaemon.cancel(true));
        Shutdown.registerHook(Persistence::save);

        HealthCheck.addStatic(Persistence.class,
                Persistence::getHealthStatus,
                () -> {
                    if (saveDaemon.isDone() && !saveDaemon.isCancelled()) {
                        return Optional.of("Save Daemon stopped by exception");
                    } else if (failedSave) {
                        return Optional.of("Error saving; Check log");
                    } else {
                        return Optional.empty();
                    }
                }
        );

        logger.info("Module initialised!");
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends PersistentObject> T loadObject(Class<T> objectClass, Supplier<T> defaultSupplier) throws ObjectLoadFailed {
        var cachedObject = objectCache.get(objectClass);
        if (cachedObject != null) {
            return (T) cachedObject;      // We know cachedObject is assignable to T because of the parameter bounds; All cache entries are created from either `tClass` or `defaultSupplier`
        }

        var filename = objectClass.getSimpleName() + ".yaml";
        try {
            var datafile = new File(dataFolder, filename);
            T dataObject;
            if (datafile.exists()) {
                dataObject = yamlMapper.readValue(datafile, objectClass);
            } else {
                dataObject = defaultSupplier.get();
                yamlMapper.writeValue(datafile, dataObject);
            }
            objectCache.put(objectClass, dataObject);
            return dataObject;
        } catch (JsonProcessingException e) {
            throw new ObjectLoadFailed.ParseError(e);
        } catch (IOException e) {
            throw new ObjectLoadFailed.IOError(e);
        }
    }

    public static synchronized byte[] loadResource(@NotNull String filename) throws ResourceLoadFailed {
        if (!isFilenameSanitized(filename)) {
            throw new ResourceLoadFailed.UnSanitizedFilename(filename);
        }

        File resourceFile = new File(resourceFolder, filename);
        try {
            return Files.readAllBytes(resourceFile.toPath());
        } catch (IOException | InvalidPathException e) {
            throw new ResourceLoadFailed.IOError(e);
        }
    }

    public static synchronized void save() {
        for (Map.Entry<Class<? extends PersistentObject>, PersistentObject> entry : objectCache.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            var filename = key.getSimpleName() + ".yaml";
            var datafile = new File(dataFolder, filename);
            try {
                yamlMapper.writeValue(datafile, value);
            } catch (IOException e) {
                logger.error("Unable to save [" + key.getCanonicalName() + "]", e);
                failedSave = true;
            }
        }
    }

    private static boolean isFilenameSanitized(String string) {
        return string.codePoints().allMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '.');
    }

    private static HealthStatus getHealthStatus() {
        if (saveDaemon.isDone()) {
            if (saveDaemon.isCancelled()) {
                return HealthStatus.SHUTTING_DOWN;
            } else {
                return HealthStatus.ERROR_CRITICAL;
            }
        } else {
            return HealthStatus.RUNNING;
        }
    }
}