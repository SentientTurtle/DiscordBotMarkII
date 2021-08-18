package net.sentientturtle.discordbot.docker;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.sentientturtle.discordbot.botmodules.simple.Voting;
import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.core.Shutdown;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand;
import net.sentientturtle.discordbot.components.presence.PresenceImportance;
import net.sentientturtle.discordbot.components.presence.PresenceManager;
import net.sentientturtle.discordbot.components.presence.PresenceProvider;
import net.sentientturtle.discordbot.helpers.ArgumentHelper;
import net.sentientturtle.discordbot.helpers.ErrorHelper;
import net.sentientturtle.discordbot.helpers.MessageHelper;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.Permission.MESSAGE_WRITE;

/**
 * Module for running Docker-Compose applications<br>
 * This module requires exclusive control over the docker environment it executes in; It may stop other running containers.<br>
 * Additionally, the module can only run one application at a time.
 */
public class Docker extends BotModule implements StaticLoaded, PresenceProvider {
    private static final Logger logger = LoggerFactory.getLogger(Docker.class);
    private static final Map<String, Path> composeDirectories;
    private static String currentConfiguration;

    static {
        String dockerFolderPath = System.getProperty("net.sentientturtle.discordbot.docker.path");
        if (dockerFolderPath == null) {
            logger.info("Docker folder path system variable (net.sentientturtle.discordbot.docker.path) not set, defaulting to './docker'");
            dockerFolderPath = "./docker";
        }
        File dockerFolder = new File(dockerFolderPath);
        if (!((dockerFolder.exists() && dockerFolder.isDirectory()) || dockerFolder.mkdir())) {
            throw new StaticInitException("Could not create data folder: " + dockerFolder);
        }

        try {
            composeDirectories = new HashMap<>();
            Files.walk(dockerFolder.toPath())
                    .filter(path -> {
                        File file = path.toFile();
                        String name = file.getName();
                        return file.isFile() && (name.equals("docker-compose.yml") || name.equals("docker-compose.yaml"));
                    })
                    .map(Path::getParent)
                    .peek(path -> logger.debug("Discovered docker configuration: " + path))
                    .forEach(path -> {
                        if (composeDirectories.putIfAbsent(path.getFileName().toString(), path) != null) {
                            logger.warn("Skipping configuration [" + path + "] as it is duplicate to: " + composeDirectories.get(path.getFileName().toString()));
                        }
                    });
        } catch (IOException e) {
            throw new StaticInitException("Could not scan for docker compose files!", e);
        }

        try {
            String result = ProcessUtil.runCommand("docker -v", 10, TimeUnit.SECONDS);
            if (!result.startsWith("Docker version")) throw new StaticInitException("Could not call docker command!");

            result = ProcessUtil.runCommand("docker-compose version", 10, TimeUnit.SECONDS);
            if (!result.startsWith("docker-compose version")) throw new StaticInitException("Could not call docker command!");
        } catch (IOException | InterruptedException e) {
            throw new StaticInitException("Could not call docker command!", e);
        }

        try {
            stopAllConfigurations();
        } catch (IOException | InterruptedException e) {
            throw new StaticInitException("Could not call docker command!", e);
        }

        Shutdown.registerHook(() -> {
            try {
                stopAllConfigurations();
            } catch (IOException | InterruptedException e) {
                logger.warn("Could not safely shut down docker!", e);
            }
        });

        logger.info("Module initialised!");
    }

    private static void upConfiguration(Path configuration) throws IOException, InterruptedException {
        stopAllConfigurations();
        ProcessUtil.runCommand("docker-compose up -d", 10, TimeUnit.MINUTES, configuration.toFile());
        currentConfiguration = configuration.getFileName().toString();
    }

    private static void stopAllConfigurations() throws IOException, InterruptedException {
        for (Path path : composeDirectories.values()) {
            ProcessUtil.runCommand("docker-compose stop -t 300", 1, TimeUnit.MINUTES, path.toFile());
        }
        currentConfiguration = null;
    }

    private static void downAllConfigurations() throws IOException, InterruptedException {
        for (Path path : composeDirectories.values()) {
            ProcessUtil.runCommand("docker-compose down", 1, TimeUnit.MINUTES, path.toFile());
        }
        currentConfiguration = null;
    }

    public Docker() {
        PresenceManager.registerProvider(this, PresenceImportance.BACKGROUND);
    }

    @Override
    public Activity getActivity() {
        if (currentConfiguration != null) {
            return Activity.playing(currentConfiguration);
        } else {
            return null;
        }
    }

    @ATextCommand(syntax = "docker list [page=1]", helpText = "Lists available docker configurations", discordPermissions = {MESSAGE_WRITE})
    public void docker_list(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        try {
            int page = ArgumentHelper.parsePage(args, 1);

            var message = MessageHelper.paginate(
                    "Docker configurations available",
                    composeDirectories.keySet().stream().sorted(),
                    Math.max(1, page)
            );

            event.getChannel().sendMessage(message).complete();
        } catch (ArgumentHelper.ParseException e) {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", e.channelMessage);
        }
    }

    @ATextCommand(helpText = "Stops all docker configurations", discordPermissions = {MESSAGE_WRITE})
    public void docker_stop(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        event.getChannel()
                .sendMessage("🔄 Stopping all docker configurations...")
                .queue(message -> Scheduling.submit(() -> {
                    try {
                        stopAllConfigurations();
                        message.editMessage("✅ Stop complete").queue();
                    } catch (IOException | InterruptedException e) {
                        message.editMessage("⚠ Error during stop: " + e.getMessage()).queue();
                    }
                }));
    }

    @ATextCommand(helpText = "Downs all docker configurations", discordPermissions = {MESSAGE_WRITE})
    public void docker_down(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        event.getChannel()
                .sendMessage("🔄 Down-ing all docker configurations...")
                .queue(message -> Scheduling.submit(() -> {
                    try {
                        downAllConfigurations();
                        message.editMessage("✅ Down complete").queue();
                    } catch (IOException | InterruptedException e) {
                        message.editMessage("⚠ Error during down: " + e.getMessage()).queue();
                    }
                }));
    }

    @ATextCommand(syntax = "docker start <configuration>", helpText = "Starts docker configuration (and shuts down others)", discordPermissions = {MESSAGE_WRITE})
    public void docker_start(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        var configOption = composeDirectories.entrySet()
                                   .stream()
                                   .filter(entry -> entry.getKey().equalsIgnoreCase(args))
                                   .findFirst();

        if (configOption.isPresent()) {
            String configuration = configOption.get().getKey();
            Path configPath = configOption.get().getValue();

            if (configuration.equals(currentConfiguration)) {
                event.getChannel()
                        .sendMessage("✅ Configuration " + configuration + " is already running")
                        .queue();
            } else {
                event.getChannel()
                        .sendMessage("🔄 Starting docker configuration " + configuration + "...")
                        .queue(message -> Scheduling.submit(() -> {
                            try {
                                upConfiguration(configPath);
                                message.editMessage("✅ Start complete").queue();
                            } catch (IOException | InterruptedException e) {
                                message.editMessage("⚠ Error during start: " + e.getMessage()).queue();
                            }
                        }));
            }
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Could not find configuration: " + args);
        }
    }

    @ATextCommand(helpText = "Allows users to vote on which docker service to start", discordPermissions = {MESSAGE_WRITE})
    public void docker_vote(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        Voting.runVote(
                "Docker vote",
                event.getChannel(),
                new ArrayList<>(composeDirectories.keySet()),
                configuration -> {
                    event.getChannel()
                            .sendMessage("🔄 Starting docker configuration " + configuration + "...")
                            .queue(message -> Scheduling.submit(() -> {
                                try {
                                    upConfiguration(composeDirectories.get(configuration));
                                    message.editMessage("✅ Start complete").queue();
                                } catch (IOException | InterruptedException e) {
                                    message.editMessage("⚠ Error during start: " + e.getMessage()).queue();
                                }
                            }));
                },
                10, TimeUnit.MINUTES
        );
    }
}
