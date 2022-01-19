package net.sentientturtle.discordbot.components.module;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.sentientturtle.discordbot.components.core.Core;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.components.module.command.UnifiedCommand;
import net.sentientturtle.discordbot.components.permission.BotPermission;
import net.sentientturtle.discordbot.components.persistence.Persistence;
import net.sentientturtle.discordbot.components.persistence.PersistentObject;
import net.sentientturtle.discordbot.loader.Loader;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import net.sentientturtle.util.Box;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manager for module, handles loading/enabling of modules and command registration
 */
public class ModuleManager implements StaticLoaded {
    private static final Logger logger = LoggerFactory.getLogger(ModuleManager.class);
    public static final Map<String, Class<? extends BotModule>> modules =
            Loader.getModuleClasses(true)
                    .collect(Collectors.toUnmodifiableMap(
                            aClass -> aClass.getSimpleName().toLowerCase(),
                            Function.identity()
                    ));

    private static final EnabledModules settings;
    private static final ConcurrentHashMap<String, BotModule> moduleCache;
    private static final ConcurrentHashMap<String, String> moduleLoadErrors;
    private static final List<EventListener> eventListenerModules;

    static {
        settings = Persistence.loadObject(EnabledModules.class, EnabledModules::new);
        moduleCache = new ConcurrentHashMap<>();
        moduleLoadErrors = new ConcurrentHashMap<>();
        HealthCheck.addStatic(
                ModuleManager.class,
                () -> (moduleLoadErrors.size() == 0) ? HealthStatus.RUNNING : HealthStatus.ERROR_NONCRITICAL,
                () -> {
                    if (moduleLoadErrors.size() > 0) {
                        return Optional.of("Error loading " + moduleLoadErrors.size() + " modules");
                    } else {
                        return Optional.empty();
                    }
                }
        );

        modules.keySet()
                .stream()
                .filter(settings.enabledModules::contains)
                .forEach(ModuleManager::loadModule);

        eventListenerModules = moduleCache.values().stream()
                .filter(EventListener.class::isInstance)
                .map(module -> (EventListener) module)
                .collect(Collectors.toList());

        logger.info("ModuleManager initialised!");
    }

    private static final HashMap<String, String> topLevelDescriptions = new HashMap<>();
    private static final HashMap<String, String> midLevelDescriptions = new HashMap<>();

    // Bit clumsy API-wise; Consequence of erasing the hierarchy discord applies to subcommands, from our command API
    static void addTopLevelCommandDescription(String commandName, String description) throws IllegalArgumentException {
        if (commandName.length() < 1 || commandName.length() > 32) throw new IllegalArgumentException("CommandName must be 1-32 characters in length");
        if (description.length() < 1 || description.length() > 100) throw new IllegalArgumentException("Description must be 1-100 characters in length");
        if (topLevelDescriptions.putIfAbsent(commandName, description) != null) {
            throw new IllegalArgumentException("Description for this command grouping was already set!");
        }
    }

    static void addMidLevelCommandDescription(String commandName, String description) throws IllegalArgumentException {
        if (commandName.length() < 1 || commandName.length() > 32) throw new IllegalArgumentException("CommandName must be 1-32 characters in length");
        if (description.length() < 1 || description.length() > 100) throw new IllegalArgumentException("Description must be 1-100 characters in length");
        if (midLevelDescriptions.putIfAbsent(commandName, description) != null) {
            throw new IllegalArgumentException("Description for this command grouping was already set!");
        }
    }

    private static OptionData parseParameter(UnifiedCommand.Parameter parameter) {
        var option = new OptionData(
                switch (parameter.parameterType()) {
                    case STRING -> OptionType.STRING;
                    case LONG -> OptionType.INTEGER;
                    case BOOLEAN -> OptionType.BOOLEAN;
                    case USER -> OptionType.USER;
                    case CHANNEL -> OptionType.CHANNEL;
                    case ROLE -> OptionType.ROLE;
                    case MENTIONABLE -> OptionType.MENTIONABLE;
                },
                parameter.name(),
                parameter.description()
        );
        if (parameter.choices().length > 0) {
            option.addChoices(
                    Arrays.stream(parameter.choices()).map(choice -> {
                        if (choice.value() instanceof String value) {
                            return new Command.Choice(choice.name(), value);
                        } else if (choice.value() instanceof Integer value) {
                            return new Command.Choice(choice.name(), value);
                        } else {
                            throw new IllegalStateException("Choice may not contain a value that is neither a String nor an Integer!");
                        }
                    }).toArray(Command.Choice[]::new)
            );
        }
        return option;
    }

    public static void onEvent(GenericEvent event) {
        for (EventListener eventListenerModule : eventListenerModules) {
            try {
                eventListenerModule.onEvent(event);
            } catch (Throwable t) {
                logger.error("Error in module event listener: " + eventListenerModule, t);
            }
        }
    }

    public enum ModuleStatus {
        ENABLED,
        PENDING_ENABLED,
        DISABLED,
        PENDING_DISABLED
    }

    public static ModuleStatus moduleStatus(String module) {
        if (settings.enabledModules.contains(module)) {
            if (moduleCache.containsKey(module)) {
                return ModuleStatus.ENABLED;
            } else {
                return ModuleStatus.PENDING_ENABLED;
            }
        } else {
            if (moduleCache.containsKey(module)) {
                return ModuleStatus.PENDING_DISABLED;
            } else {
                return ModuleStatus.DISABLED;
            }
        }
    }

    private record CommandNode(Box<UnifiedCommand> command, HashMap<String, CommandGroupNode> groups) {}

    private record CommandGroupNode(Box<UnifiedCommand> command, HashMap<String, UnifiedCommand> leafCommands) {}

    private static HashMap<String, UnifiedCommand> commands = new HashMap<>();

    public static HashMap<String, UnifiedCommand> getCommandMap() {
        return commands;
    }

    public static void registerCommands() throws IllegalStateException {
        Guild guild = Core.getGuild();
        if (guild == null) throw new IllegalStateException("Not connected to target Guild; Cannot register commands!");

        HashMap<String, UnifiedCommand> commandPathMap = new HashMap<>();

        HashMap<String, CommandNode> commandTree = new HashMap<>();
        moduleCache.values()
                .stream()
                .flatMap(BotModule::getCommands)
                .forEach(unifiedCommand -> {
                    if (unifiedCommand.subcommandName() == null && unifiedCommand.subcommandGroup() == null) {  // Top level command
                        var node = commandTree.computeIfAbsent(unifiedCommand.commandName(), s -> new CommandNode(Box.empty(), new HashMap<>()));
                        if (node.groups.size() == 0) {
                            if (node.command.setIfEmpty(unifiedCommand)) {
                                commandPathMap.put(unifiedCommand.path(), unifiedCommand);
                            } else {
                                logger.error("Duplicate top-level command: " + unifiedCommand);
                            }
                        } else {
                            logger.error("Subcommands shadow top-level command: " + unifiedCommand);
                        }
                    } else if (unifiedCommand.subcommandGroup() == null) { // unifiedCommand.subcommandName() is known to be not null here   // Mid-level command
                        var topNode = commandTree.computeIfAbsent(unifiedCommand.commandName(), s -> new CommandNode(Box.empty(), new HashMap<>()));
                        if (!topNode.command.isEmpty()) {
                            logger.error("Subcommands shadow top-level command: " + topNode.command.get());
                            topNode.command.setValue(null);
                        }
                        var midNode = topNode.groups.computeIfAbsent(unifiedCommand.subcommandName(), s -> new CommandGroupNode(Box.empty(), new HashMap<>()));
                        if (midNode.leafCommands.size() == 0) {
                            if (midNode.command.setIfEmpty(unifiedCommand)) {
                                commandPathMap.put(unifiedCommand.path(), unifiedCommand);
                            } else {
                                logger.error("Duplicate mid-level command: " + unifiedCommand);
                            }
                        } else {
                            logger.error("Subcommands shadow mid-level command: " + unifiedCommand);
                        }
                    } else if (unifiedCommand.subcommandName() != null) { // && unifiedCommand.subcommandGroup() is known to be not null here   // Bottom-level command
                        var topNode = commandTree.computeIfAbsent(unifiedCommand.commandName(), s -> new CommandNode(Box.empty(), new HashMap<>()));
                        if (!topNode.command.isEmpty()) {
                            logger.error("Subcommands shadow top-level command: " + topNode.command.get());
                            topNode.command.setValue(null);
                        }
                        var midNode = topNode.groups.computeIfAbsent(unifiedCommand.subcommandGroup(), s -> new CommandGroupNode(Box.empty(), new HashMap<>()));
                        if (!midNode.command.isEmpty()) {
                            logger.error("Subcommands shadow mid-level command: " + topNode.command.get());
                            midNode.command.setValue(null);
                        }
                        if (midNode.leafCommands.putIfAbsent(unifiedCommand.subcommandName(), unifiedCommand) == null) {
                            commandPathMap.put(unifiedCommand.path(), unifiedCommand);
                        } else {
                            logger.error("Duplicate bottom-level command: " + unifiedCommand);
                        }
                    } else {
                        throw new RuntimeException("Unreachable; UnifiedCommand may not have a subcommandgroup set without a subcommand set");
                    }
                });

        ArrayList<CommandData> commands = new ArrayList<>();
        commandTree.forEach((commandName, commandNode) -> {
            var commandData = new CommandData(commandName, commandName);
            var command = commandNode.command.get();
            if (command == null) {
                AtomicBoolean defaultEnabled = new AtomicBoolean(true);
                commandData.setDescription(topLevelDescriptions.getOrDefault(commandName, commandName));
                var subcommandGroups = new ArrayList<SubcommandGroupData>();
                var subcommands = new ArrayList<SubcommandData>();
                commandNode.groups.forEach((commandgroupName, commandGroupNode) -> {
                    var commandGroup = commandGroupNode.command.get();
                    if (commandGroup == null) {
                        var subcommandGroupData = new SubcommandGroupData(commandgroupName, midLevelDescriptions.getOrDefault(commandgroupName, commandgroupName));
                        var subcommandDatas = commandGroupNode.leafCommands.values()
                                .stream()
                                .peek(unifiedCommand -> defaultEnabled.compareAndSet(true, unifiedCommand.commandUserPermission().isEveryone()))
                                .map(subcommand -> {
                                    assert subcommand.subcommandName() != null; // Subcommands are mapped to their name, so it cannot be null and must be unique here
                                    var subcommandData = new SubcommandData(subcommand.subcommandName(), subcommand.description());
                                    var options = Stream.concat(
                                            Arrays.stream(subcommand.requiredParameters())
                                                    .map(ModuleManager::parseParameter)
                                                    .map(option -> option.setRequired(true)),
                                            Arrays.stream(subcommand.optionalParameters())
                                                    .map(ModuleManager::parseParameter)
                                    ).toArray(OptionData[]::new);
                                    subcommandData.addOptions(options);
                                    return subcommandData;
                                })
                                .toArray(SubcommandData[]::new);
                        subcommandGroupData.addSubcommands(subcommandDatas);
                        subcommandGroups.add(subcommandGroupData);
                    } else {
                        defaultEnabled.compareAndSet(true, commandGroup.commandUserPermission().isEveryone());
                        assert commandgroupName.equals(commandGroup.commandName());
                        var subcommand = new SubcommandData(commandgroupName, commandGroup.description());
                        var options = Stream.concat(
                                Arrays.stream(commandGroup.requiredParameters())
                                        .map(ModuleManager::parseParameter)
                                        .map(option -> option.setRequired(true)),
                                Arrays.stream(commandGroup.optionalParameters())
                                        .map(ModuleManager::parseParameter)
                        ).toArray(OptionData[]::new);
                        subcommand.addOptions(options);
                        subcommands.add(subcommand);
                    }
                });
                commandData.addSubcommandGroups(subcommandGroups.toArray(SubcommandGroupData[]::new));
                commandData.addSubcommands(subcommands.toArray(SubcommandData[]::new));
                commandData.setDefaultEnabled(defaultEnabled.get());
            } else {
                assert commandName.equals(command.commandName());
                commandData.setDescription(command.description());
                var options = Stream.concat(
                        Arrays.stream(command.requiredParameters())
                                .map(ModuleManager::parseParameter)
                                .map(option -> option.setRequired(true)),
                        Arrays.stream(command.optionalParameters())
                                .map(ModuleManager::parseParameter)
                ).toArray(OptionData[]::new);
                commandData.addOptions(options);
                commandData.setDefaultEnabled(command.commandUserPermission().isEveryone());
            }
            commands.add(commandData);
        });

        logger.info("Registering commands: " + commands);

        guild.updateCommands()
                .addCommands(commands)
                .queue(commandList -> {
                    logger.info("Registered commands: " + commandList);
                    logger.info("Updating command permissions...");
                    Map<String, Collection<CommandPrivilege>> privileges = new HashMap<>();
                    for (Command command : commandList) {
                        if (!command.isDefaultEnabled()) {
                            var commandPrivileges = privileges.computeIfAbsent(command.getId(), id -> new ArrayList<>());
                            var permission = BotPermission.STRING("cmd:" + command.getName());

                            permission.getTargetedUsers().map(CommandPrivilege::enableUser).forEach(commandPrivileges::add);
                            permission.getTargetedRoles().map(CommandPrivilege::enableRole).forEach(commandPrivileges::add);
                            privileges.put(command.getId(), commandPrivileges);
                        }
                    }
                    Core.getGuild().updateCommandPrivileges(Collections.unmodifiableMap(privileges)).queue(map -> logger.info("Updated command permissions: " + map.keySet()));
                });

        ModuleManager.commands = commandPathMap;
    }

    public static boolean moduleExists(@NotNull String name) {
        return modules.containsKey(name.toLowerCase());
    }

    private static synchronized void loadModule(@NotNull String name) {
        moduleCache.computeIfAbsent(name.toLowerCase(), moduleName -> {
            if (!moduleLoadErrors.containsKey(moduleName)) {
                try {
                    final Class<? extends BotModule> moduleClass = modules.get(moduleName);
                    if (moduleClass != null) {
                        logger.info("Loading module: [" + moduleName + "]");
                        return BotModule.getInstance(moduleClass);
                    } else {
                        logger.error("Attempt to load non-existent module: [" + moduleName + "]");
                        moduleLoadErrors.put(moduleName, "Attempt to load non-existent module");
                        return null;
                    }
                } catch (Throwable t) {
                    logger.error("Unable to load permitted module: [" + moduleName + "]", t);
                    moduleLoadErrors.put(moduleName, "Error during load: " + t.getClass().getSimpleName());
                    return null;
                }
            } else {
                return null;
            }
        });
    }

    public static synchronized boolean enableModule(String moduleName) {
        if (!moduleExists(moduleName)) return false;
        boolean modified = settings.enabledModules.add(moduleName);
        if (modified) {
            logger.info("Enable module: [" + moduleName + "]");
        }
        return modified;
    }

    public static synchronized boolean disableModule(String moduleName) {
        if (!moduleExists(moduleName)) return false;
        boolean modified = settings.enabledModules.remove(moduleName);
        if (modified) {
            logger.info("Disabled module: [" + moduleName + "]");
        }
        return modified;
    }

    private static class EnabledModules implements PersistentObject {
        public HashSet<String> enabledModules = new HashSet<>() {{ add("admin"); }};    // Double-bracket initialization here is ugly, but we need a mutable map here. Admin module is enabled by default to ensure bot can be used when cold-started without configs.
    }
}
