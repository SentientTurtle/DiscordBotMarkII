package net.sentientturtle.discordbot;

import net.sentientturtle.discordbot.components.core.Core;
import net.sentientturtle.discordbot.components.core.Shutdown;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Default entrypoint<br>
 * Sets various system properties<br>
 * For main implementation of bot, see {@link Core}
 */
public class Main {
    static {
        if (System.getProperty("org.slf4j.simpleLogger.logFile") == null) {
            System.setProperty("org.slf4j.simpleLogger.logFile", "./log.txt");
        }
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public static void main(String[] args) {
        try {
            Core.init();
        } catch (Throwable t) {
            t.printStackTrace();
            Shutdown.shutdownAll(true, 0);
        }
    }
}
