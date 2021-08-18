package net.sentientturtle.discordbot.database;

import com.almworks.sqlite4java.SQLite;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;
import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.core.Shutdown;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Module for SQLite database access<br>
 * Automatically unpacks required native libraries from java resources
 */
public class Database implements StaticLoaded {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static final File databasefile;
    private static final SQLiteQueue sqLiteQueue;

    private static final String[] NATIVE_LIBRARY_FILENAMES = {
            "libsqlite4java-linux-amd64.so",
            "libsqlite4java-linux-i386.so",
            "libsqlite4java-osx.dylib",
            "sqlite4java-win32-x64.dll",
            "sqlite4java-win32-x86.dll"
    };

    static {
        String dbFilePath = System.getProperty("net.sentientturtle.discordbot.databasefile");
        if (dbFilePath == null) {
            logger.info("Database path system variable (net.sentientturtle.discordbot.databasefile) not set, defaulting to './data/database.db'");
            dbFilePath = "./data/database.db";
        }
        databasefile = new File(dbFilePath);
        var databaseFolder = databasefile.getParentFile();
        if (!((databaseFolder.exists() && databaseFolder.isDirectory()) || databaseFolder.mkdir())) {
            throw new StaticInitException("Could not create database parent folder: " + databasefile);
        }

        File nativeLibFolder;
        if (System.getProperty("sqlite4java.library.path") == null) {
            String nativelibFolderPath = System.getProperty("net.sentientturtle.discordbot.nativelibs");
            if (nativelibFolderPath == null) {
                logger.info("Native library folder system variable (net.sentientturtle.discordbot.nativelibs) not set, defaulting to './nativelibs'");
                nativelibFolderPath = "./nativelibs";
            }
            nativeLibFolder = new File(nativelibFolderPath);
            if (!((nativeLibFolder.exists() && nativeLibFolder.isDirectory()) || nativeLibFolder.mkdir())) {
                throw new StaticInitException("Could not create native library folder: " + nativeLibFolder);
            }

            for (String filename : NATIVE_LIBRARY_FILENAMES) {
                var path = Path.of(nativelibFolderPath, filename);
                if (!Files.exists(path)) {
                    try (InputStream library = Database.class.getResourceAsStream("/nativelibs/" + filename)) {
                        Files.copy(library, path);
                    } catch (IOException e) {
                        throw new StaticInitException("Could not copy native libraries to folder", e);
                    }
                }
            }

            SQLite.setLibraryPath(nativelibFolderPath);
        }

        try {
            SQLite.loadLibrary();
        } catch (SQLiteException e) {
            throw new StaticInitException("Cannot find SQLite native libraries!", e);
        }


        sqLiteQueue = new SQLiteQueue(databasefile);
        sqLiteQueue.start();
        Shutdown.registerHook(() -> {
            sqLiteQueue.stop(true);
            logger.info("Database stopped");
        });

        HealthCheck.addStatic(Database.class, () -> {
                    if (sqLiteQueue.isStopped()) {
                        return HealthStatus.ERROR_CRITICAL;
                    } else {
                        return HealthStatus.RUNNING;
                    }
                }
        );

        logger.info("Database opened");
    }

    public static <T, J extends SQLiteJob<T>> J executeSqlite(J job) {
        return sqLiteQueue.execute(job);
    }
}
