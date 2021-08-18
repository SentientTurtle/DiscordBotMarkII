package net.sentientturtle.discordbot.docker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Helper class for starting a process/command and gathering it's output
 */
public class ProcessUtil {
    public static String runCommand(String command, long timeout, TimeUnit timeoutUnit) throws IOException, InterruptedException {
        return runCommand(command, timeout, timeoutUnit, null);
    }

    public static String runCommand(String command, long timeout, TimeUnit timeoutUnit, File workingDirectory) throws IOException, InterruptedException {
        var process = Runtime.getRuntime()
                              .exec(command, null, workingDirectory);

        process.waitFor(timeout, timeoutUnit);
        return new BufferedReader(new InputStreamReader(process.getInputStream()))
                       .lines()
                       .collect(Collectors.joining("\n"));
    }
}
