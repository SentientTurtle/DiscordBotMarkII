package net.sentientturtle.discordbot.components.core;

import net.sentientturtle.discordbot.Main;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import net.sentientturtle.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Component to manage Shutdown; Provides convenience APIs on top of {@link System}/{@link Runtime} APIs
 * Shutdown hooks added through this class run before and separate to {@link Runtime}'s shutdown hooks, in newest-first order.
 */
public class Shutdown implements StaticLoaded {
    private static final int SHUTDOWN_CHECKS = 20;
    private static final Logger logger = LoggerFactory.getLogger(Shutdown.class);
    private static final List<Runnable> hooks = new ArrayList<>();
    private static boolean isShutdown = false;

    // Shutdown does not have its own health-check; This functionality is (implicitly) provided by the 'Core' module health-check

    /**
     * Register a Runnable to be called upon "safe-shutdown"; Hooks should leave the static module in a safe state for JVM shutdown
     * After hook has been called, modules may be in an invalid state.
     * Hooks are called in reverse order; Newest first. Module dependencies are generally still available during the shutdown hook
     *
     * If already shutting down, {@code hook} is simply called immediately in the current thread
     * @param hook Runnable to be called upon shutdown
     */
    public static synchronized void registerHook(Runnable hook) {
        if (!isShutdown) {
            hooks.add(hook);
        } else {
            hook.run();
        }
    }

    /**
     * Initiates shutdown
     * If hard shutdowns are disabled, will only call shutdown hooks. Application will remain alive if a non-daemon thread does not exit after shutdown hooks are called.
     * If hard shutdowns are enabled, will asynchronously monitor thread shutdown. If a non-daemon thread does not exit, a hard shutdown through System.exit occurs.
     *
     * Only acts once; Does nothing if {@link Shutdown#isShutdown()} is true.
     * @param enableHardShutdown If enabled, calls System.exit with the supplied exit code after a timeout.
     * @param exitCode Exit code to exit with, if hard-shutdowns are enabled
     */
    public static synchronized void shutdownAll(boolean enableHardShutdown, int exitCode) {
        if (isShutdown) return;
        logger.info("Shutdown requested");
        System.out.println("Shutdown requested");
        for (int i = hooks.size() - 1; i >= 0; i--) {
            try {
                hooks.get(i).run();
            } catch (Throwable t) {
                logger.error("Exception during shutdown hook!", t);
            }
        }
        hooks.clear();
        isShutdown = true;

        if (enableHardShutdown) {
            var shutdownThread = new Thread(() -> {
                logger.info("Monitoring shutdown progress...");
                System.out.println("Monitoring shutdown progress...");

                for (int i = 0; i < SHUTDOWN_CHECKS; i++) {
                    Thread[] allThreads = Util.getAllThreads();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                    System.out.println("Checking threads...");
                    boolean allThreadsShutdownOrDaemon = true;
                    for (Thread thread : allThreads) {
                        if (thread == null) continue;
                        if (thread == Thread.currentThread()) continue;
                        if (!thread.isAlive()) continue;
                        if (thread.isDaemon()) continue;
                        if (thread.getName().equals("DestroyJavaVM")) continue;
                        System.out.println("\tFound non-shutdown thread: " + thread.getName());
                        allThreadsShutdownOrDaemon = false;
                    }

                    if (allThreadsShutdownOrDaemon) {
                        logger.info("All non-daemon threads stopped, shutting down gracefully!");
                        System.out.println("\tAll non-daemon threads stopped, shutting down gracefully!");
                        System.exit(exitCode);
                        return;
                    } else {
                        System.out.println("\tRetrying " + (SHUTDOWN_CHECKS - i) + " more times");
                    }
                }

                Thread[] allThreads = Util.getAllThreads();
                for (Thread thread : allThreads) {
                    if (thread == null) continue;
                    if (thread == Thread.currentThread()) continue;
                    if (!thread.isAlive()) continue;
                    if (thread.isDaemon()) continue;
                    if (thread.getName().equals("DestroyJavaVM")) continue;
                    logger.warn("Thread [" + thread.getName() + "] has not shut down gracefully!");
                    System.out.println("Thread [" + thread.getName() + "] has not shut down gracefully!");
                }
                logger.warn("Not all threads have stopped. Shutting down non-gracefully!");
                System.out.println("Not all threads have stopped. Shutting down non-gracefully!");
                System.exit(exitCode);
            }, "ShutdownMonitor");
            shutdownThread.setDaemon(false);
            shutdownThread.start();
        }
    }

    /**
     * @return True if {@link Shutdown#shutdownAll} has been called, false otherwise
     */
    public static boolean isShutdown() {
        return isShutdown;
    }

    /**
     * Initiates restart.<br>
     * Shuts down with hard shut-down enabled
     */
    public static synchronized void restart() throws IOException {
        System.out.println("Restart requested");
        Runtime.getRuntime().addShutdownHook(createRestartHook());
        shutdownAll(true, 0);
    }

    private static Thread createRestartHook() throws IOException {
        if (!Core.isRebootEnabled()) throw new IllegalStateException("Restart is disabled!");

        final List<String> command = new ArrayList<>();

        var path = Path.of("/proc/self/cmdline");   // First try procfs
        if (Files.isReadable(path)) {
            command.addAll(List.of(Files.readString(path).split("\u0000")));    // AddAll used so that cmdline is effectively-final and can be used as a capture later.
        } else {    // If procfs is unavailable, attempt to reconstruct arguments; This is fragile and may not work on non-hotspot JVMs
            String JRE;     // Attempt to use the same java install
            String javaHome = System.getProperty("java.home");
            if (javaHome == null) {
                JRE = "java";   // Fallback to PATH if java.home is unset
            } else {
                JRE = "\"" + Path.of(javaHome, "bin", "java") + "\"";
            }
            var runtimeBean = ManagementFactory.getRuntimeMXBean();
            String codeSource = new File(Shutdown.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getPath();   // The wrapping in File prevents an issue; Direct URL -> Path conversion introduces a leading slash.
            if (codeSource.endsWith(".jar")) {
                command.addAll(List.of(JRE, "-jar", "\"" + codeSource + "\"")); // This project is deployed as a fatjar, so no classpath argument is required
            } else {
                command.addAll(List.of(JRE, "-cp", '"' + runtimeBean.getClassPath() + '"', Main.class.getCanonicalName()));
            }
            command.addAll(runtimeBean.getInputArguments()); // This may not include jvm arguments on non-hotspot JVMs.
        }

        return new Thread(() -> {
            try {
                System.out.println("Commencing restart...");
                logger.info("Commencing restart with command: " + command);
                new ProcessBuilder(command).inheritIO().start();
            } catch (IOException e) {
                throw new RuntimeException("Could not launch restart process!", e);
            }
        });
    }
}
