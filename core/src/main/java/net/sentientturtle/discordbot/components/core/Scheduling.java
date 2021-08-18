package net.sentientturtle.discordbot.components.core;

import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global ExecutorService for the entire bot, ensures that shutdown happens properly.
 */
public class Scheduling implements StaticLoaded {
    private static final ScheduledThreadPoolExecutor executorService;
    private static final Logger logger = LoggerFactory.getLogger(Scheduling.class);

    static {
        executorService = new ScheduledThreadPoolExecutor(3, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(0);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r, "scheduling-" + threadNumber.getAndIncrement());
                thread.setDaemon(false);
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        });
        executorService.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executorService.setRemoveOnCancelPolicy(true);
        Shutdown.registerHook(executorService::shutdown);
        logger.info("Module initialised!");

        HealthCheck.addStatic(Scheduling.class, () -> {
                if (executorService.isTerminating()) {
                    return HealthStatus.SHUTTING_DOWN;
                } else if (executorService.isShutdown()) {
                    return HealthStatus.STOPPED;
                } else {
                    return HealthStatus.RUNNING;
                }
        }, () -> Optional.of(executorService.getQueue().size() + " tasks pending"));
    }

    public static ScheduledFuture<?> schedule(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
        return executorService.schedule(command, delay, unit);
    }

    public static <V> ScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit) {
        return executorService.schedule(callable, delay, unit);
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
        return executorService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public static ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay, long delay, @NotNull TimeUnit unit) {
        return executorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public static <T> Future<T> submit(@NotNull Callable<T> task) {
        return executorService.submit(task);
    }

    public static <T> Future<T> submit(@NotNull Runnable task, T result) {
        return executorService.submit(task, result);
    }

    public static Future<?> submit(@NotNull Runnable task) {
        return executorService.submit(task);
    }
}
