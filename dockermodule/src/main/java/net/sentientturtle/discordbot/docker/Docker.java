package net.sentientturtle.discordbot.docker;


///**
// * Module for running Docker-Compose applications<br>
// * This module requires exclusive control over the docker environment it executes in; It may stop other running containers.<br>
// * Additionally, the module can only run one application at a time.
// */


/**
 * Scrapped, pending re-valuation of Docker's "proper" API.
 */
public class Docker {//} extends BotModule implements StaticLoaded, PresenceProvider {
//    private static final Logger logger = LoggerFactory.getLogger(Docker.class);
//    private static final Map<String, Path> composeDirectories;
//    private static String currentConfiguration;
//
//    static {
//        String dockerFolderPath = System.getProperty("net.sentientturtle.discordbot.docker.path");
//        if (dockerFolderPath == null) {
//            logger.info("Docker folder path system variable (net.sentientturtle.discordbot.docker.path) not set, defaulting to './docker'");
//            dockerFolderPath = "./docker";
//        }
//        File dockerFolder = new File(dockerFolderPath);
//        if (!((dockerFolder.exists() && dockerFolder.isDirectory()) || dockerFolder.mkdir())) {
//            throw new StaticInitException("Could not create data folder: " + dockerFolder);
//        }
//
//        try {
//            composeDirectories = new HashMap<>();
//            Files.walk(dockerFolder.toPath())
//                    .filter(path -> {
//                        File file = path.toFile();
//                        String name = file.getName();
//                        return file.isFile() && (name.equals("docker-compose.yml") || name.equals("docker-compose.yaml"));
//                    })
//                    .map(Path::getParent)
//                    .peek(path -> logger.debug("Discovered docker configuration: " + path))
//                    .forEach(path -> {
//                        if (composeDirectories.putIfAbsent(path.getFileName().toString(), path) != null) {
//                            logger.warn("Skipping configuration [" + path + "] as it is duplicate to: " + composeDirectories.get(path.getFileName().toString()));
//                        }
//                    });
//        } catch (IOException e) {
//            throw new StaticInitException("Could not scan for docker compose files!", e);
//        }
//
//        try {
//            String result = ProcessUtil.runCommand("docker -v", 10, TimeUnit.SECONDS);
//            if (!result.startsWith("Docker version")) throw new StaticInitException("Docker version call failed!");
//
//            result = ProcessUtil.runCommand("docker-compose version", 10, TimeUnit.SECONDS);
//            if (!result.startsWith("docker-compose version")) throw new StaticInitException("Docker compose version call failed!");
//        } catch (IOException | InterruptedException e) {
//            throw new StaticInitException("Could not call docker command!", e);
//        }
//
//        try {
//            stopAllConfigurations();
//        } catch (IOException | InterruptedException e) {
//            throw new StaticInitException("Could not call docker command!", e);
//        }
//
//        Shutdown.registerHook(() -> {
//            try {
//                stopAllConfigurations();
//            } catch (IOException | InterruptedException e) {
//                logger.warn("Could not safely shut down docker!", e);
//            }
//        });
//
//        logger.info("Module initialised!");
//    }
//
//    private static void upConfiguration(Path configuration) throws IOException, InterruptedException {
//        stopAllConfigurations();
//        ProcessUtil.runCommand("docker-compose up -d", 10, TimeUnit.MINUTES, configuration.toFile());
//        currentConfiguration = configuration.getFileName().toString();
//    }
//
//    private static void stopAllConfigurations() throws IOException, InterruptedException {
//        for (Path path : composeDirectories.values()) {
//            ProcessUtil.runCommand("docker-compose stop -t 300", 1, TimeUnit.MINUTES, path.toFile());
//        }
//        currentConfiguration = null;
//    }
//
//    private static void downAllConfigurations() throws IOException, InterruptedException {
//        for (Path path : composeDirectories.values()) {
//            ProcessUtil.runCommand("docker-compose down", 1, TimeUnit.MINUTES, path.toFile());
//        }
//        currentConfiguration = null;
//    }
//
//    public Docker() {
//        PresenceManager.registerProvider(this, PresenceImportance.BACKGROUND);
//    }
//
//    @Override
//    public Activity getActivity() {
//        if (currentConfiguration != null) {
//            return Activity.playing(currentConfiguration);
//        } else {
//            return null;
//        }
//    }
//
//    @Command(commandName = "docker", subcommandName = "list", description = "Lists available docker configurations")
//    public void docker_list(CommandCall commandCall, @Command.Parameter(name = "page", description = "page", optional = true) Long page) {
//        commandCall.reply(
//                MessageHelper.paginate(
//                        "Docker configurations available",
//                        composeDirectories.keySet().stream().sorted().toList(),
//                        Math.max(1, page == null ? 1 : page)
//                ),
//                true
//        );
//    }
//
//    @Command(commandName = "docker", subcommandName = "stop", description = "Stops all docker configurations")
//    public void docker_stop(CommandCall commandCall) {
//        commandCall.reply("🔄 Stopping all docker configurations...", false, interactionHook -> {
//            Scheduling.submit(() -> {
//                try {
//                    stopAllConfigurations();
//                    interactionHook.editOriginal("✅ Stop complete").queue();
//                } catch (IOException | InterruptedException e) {
//                    interactionHook.editOriginal("⚠ Error during stop: " + e.getMessage()).queue();
//                }
//            });
//        });
//    }
//
//    @Command(commandName = "docker", subcommandName = "down", description = "Downs all docker configurations")
//    public void docker_down(CommandCall commandCall) {
//        commandCall.reply("🔄 Down-ing all docker configurations...", false, interactionHook -> {
//            Scheduling.submit(() -> {
//                try {
//                    downAllConfigurations();
//                    interactionHook.editOriginal("✅ Down complete").queue();
//                } catch (IOException | InterruptedException e) {
//                    interactionHook.editOriginal("⚠ Error during down: " + e.getMessage()).queue();
//                }
//            });
//        });
//    }
//
//    @Command(commandName = "docker", subcommandName = "start", description = "Start a specific configuration configurations")
//    public void docker_start(CommandCall commandCall, @Command.Parameter(name = "configuration", description = "Configuration to start") String configurationString) {
//        var configOption = composeDirectories.entrySet()
//                .stream()
//                .filter(entry -> entry.getKey().equalsIgnoreCase(configurationString))
//                .findFirst();
//
//        if (configOption.isPresent()) {
//            String configuration = configOption.get().getKey();
//            Path configPath = configOption.get().getValue();
//
//            if (configuration.equals(currentConfiguration)) {
//                commandCall.reply("✅ Configuration " + configuration + " is already running");
//            } else {
//                commandCall.reply("🔄 Starting docker configuration " + configuration + "...", false, interactionHook -> Scheduling.submit(() -> {
//                    try {
//                        upConfiguration(configPath);
//                        interactionHook.editOriginal("✅ Start complete").queue();
//                    } catch (IOException | InterruptedException e) {
//                        interactionHook.editOriginal("⚠ Error during start: " + e.getMessage()).queue();
//                    }
//                }));
//            }
//        } else {
//            commandCall.error("⚠ Could not find configuration: " + configurationString);
//        }
//    }
//
//    @Command(commandName = "docker", subcommandName = "vote", description = "Hold a vote on which docker configuration to start")
//    public void docker_vote(CommandCall commandCall) {
//        if (composeDirectories.size() > 0) {
//            commandCall.reply("Holding vote...", true);
//            Voting.runVote(
//                    "Docker vote",
//                    commandCall.getChannel(),
//                    new ArrayList<>(composeDirectories.keySet()),
//                    configuration -> commandCall.getChannel()
//                            .sendMessage("🔄 Starting docker configuration " + configuration + "...")
//                            .queue(message -> Scheduling.submit(() -> {
//                                try {
//                                    upConfiguration(composeDirectories.get(configuration));
//                                    message.editMessage("✅ Start complete").queue();
//                                } catch (IOException | InterruptedException e) {
//                                    message.editMessage("⚠ Error during start: " + e.getMessage()).queue();
//                                }
//                            })),
//                    10, TimeUnit.MINUTES,
//                    BotPermission.EVERYONE()    // TODO: Make configurable
//            );
//        } else {
//            commandCall.error("⚠ No configurations are currently available.");
//        }
//    }
}
