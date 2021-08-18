package net.sentientturtle.discordbot.components.module;

import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.components.module.command.TextCommand;
import net.sentientturtle.discordbot.components.persistence.Persistence;
import net.sentientturtle.discordbot.components.persistence.PersistenceException;
import net.sentientturtle.discordbot.components.persistence.PersistentObject;
import net.sentientturtle.discordbot.loader.Loader;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import net.sentientturtle.util.Box;
import net.sentientturtle.util.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

// TODO: Check synchronization and concurrent collections
public class ModuleManager implements StaticLoaded {
    private static final Logger logger = LoggerFactory.getLogger(ModuleManager.class);
    public static final Map<String, Class<? extends BotModule>> modules =
            Loader.getModuleClasses(true, ModuleManager.class.getClassLoader())
                    .collect(Collectors.toUnmodifiableMap(
                            aClass -> aClass.getSimpleName().toLowerCase(),
                            Function.identity()
                    ));

    private static final PermittedModules settings;
    private static final ConcurrentHashMap<String, BotModule> moduleCache;
    private static final ConcurrentHashMap<String, String> moduleLoadErrors;

    private static final Box<List<BotModule>> guildModuleCache = new Box<>();
    private static final ConcurrentHashMap<Long, List<BotModule>> channelModuleCache = new ConcurrentHashMap<>();     // Cache of channels and their permitted modules!
    private static final ConcurrentHashMap<Long, CommandNode> channelCommandCache = new ConcurrentHashMap<>();     // Cache of channels and their permitted commands!

    static {
        try {
            settings = Persistence.loadObject(PermittedModules.class, PermittedModules::new);
        } catch (PersistenceException.ObjectLoadFailed objectLoadFailed) {
            throw new StaticInitException(objectLoadFailed);
        }
        moduleCache = new ConcurrentHashMap<>();
        moduleLoadErrors = new ConcurrentHashMap<>();
        HealthCheck.addStatic(
                ModuleManager.class,
                () -> (moduleLoadErrors.size() == 0) ? HealthStatus.READY : HealthStatus.ERROR_NONCRITICAL,
                () -> {
                    if (moduleLoadErrors.size() > 0) {
                        StringBuilder builder = new StringBuilder();
                        for (Map.Entry<String, String> moduleErrorEntry : moduleLoadErrors.entrySet()) {
                            builder.append(moduleErrorEntry.getKey()).append("\t").append(moduleErrorEntry.getValue());
                        }
                        return Optional.of(builder.toString());
                    } else {
                        return Optional.empty();
                    }
                }
        );
        logger.info("Module initialised!");
    }

    public static boolean moduleExists(@NotNull String name) {
        return modules.containsKey(name.toLowerCase());
    }

    public static void preloadCaches(LongStream channelIDs) {
        guildModuleCache.setValue(buildGuildModuleCache());
        channelIDs
                .peek(id -> channelCommandCache.computeIfAbsent(id, ModuleManager::buildChannelCommandCacheEntry))
                .forEach(id -> channelModuleCache.computeIfAbsent(id, ModuleManager::buildChannelModuleCacheEntry));
    }

    public static Optional<Tuple2<TextCommand, String>> getCommandInChannel(long channelID, String message) {
        return channelCommandCache.computeIfAbsent(channelID, ModuleManager::buildChannelCommandCacheEntry)
                       .find(message);
    }

    public static Stream<TextCommand> listCommandsForChannel(long channelID) {
        return channelCommandCache.computeIfAbsent(channelID, ModuleManager::buildChannelCommandCacheEntry).listCommands();
    }

    public static Stream<BotModule> listModulesForChannel(long channelID) {
        return channelModuleCache.computeIfAbsent(channelID, ModuleManager::buildChannelModuleCacheEntry).stream();
    }

    public static Stream<BotModule> listModulesForGuild() {
        return guildModuleCache.computeIfEmpty(
                ModuleManager::buildGuildModuleCache
        ).stream();
    }

    public static synchronized void invalidateCaches(boolean reload) {
        if (reload) {
            long[] channelIDs = Stream.concat(channelModuleCache.keySet().stream(), channelCommandCache.keySet().stream())
                                        .mapToLong(Long::longValue)
                                        .distinct()
                                        .toArray();

            channelModuleCache.clear();
            channelCommandCache.clear();
            guildModuleCache.clear();
            preloadCaches(Arrays.stream(channelIDs));
        } else {
            channelModuleCache.clear();
            channelCommandCache.clear();
            guildModuleCache.clear();
        }
    }

    private static synchronized @Nullable BotModule getModuleInstance(@NotNull String name) {
        return moduleCache.computeIfAbsent(name.toLowerCase(), moduleName -> {
            if (!moduleLoadErrors.containsKey(moduleName)) {
                try {
                    final Class<? extends BotModule> moduleClass = modules.get(moduleName);
                    if (moduleClass != null) {
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

    private static synchronized List<BotModule> buildChannelModuleCacheEntry(Long channelID) {
        var permittedEverywhere = settings.permittedInGuild;
        @Nullable var permittedInChannel = settings.permittedInChannel.get(channelID);
        @Nullable var blockedInChannel = settings.blockedInChannel.get(channelID);

        var moduleList = new ArrayList<BotModule>();
        if (blockedInChannel == null) { // Add all permitted modules directly
            for (String name : permittedEverywhere) {
                var module = getModuleInstance(name);
                if (module != null) moduleList.add(module);
            }
            if (permittedInChannel != null) {
                for (String name : permittedInChannel) {
                    var module = getModuleInstance(name);
                    if (module != null) moduleList.add(module);
                }
            }
        } else {    // Check if modules are blocked before adding
            for (String name : permittedEverywhere) {
                if (!blockedInChannel.contains(name)) {
                    var module = getModuleInstance(name);
                    if (module != null) moduleList.add(module);
                }
            }
            if (permittedInChannel != null) {
                for (String name : permittedInChannel) {
                    if (!blockedInChannel.contains(name)) {
                        var module = getModuleInstance(name);
                        if (module != null) moduleList.add(module);
                    } else {
                        logger.warn("Module on both permit-in-channel and block-in-channel lists: [" + name + "]! Module has been removed from permit-in-channel list!");
                        permittedInChannel.remove(name);
                    }
                }
            }
        }

        return moduleList;
    }

    private static CommandNode buildChannelCommandCacheEntry(Long channelID) {
        CommandNode root = new CommandNode();
        listModulesForChannel(channelID)
                .map(BotModule::getCommands)
                .flatMap(List::stream)
                .forEach(root::insert);
        return root;
    }

    public static synchronized boolean permitInGuild(String moduleName) {
        boolean modified = settings.permittedInGuild.add(moduleName);
        for (HashSet<String> permittedModules : settings.permittedInChannel.values()) {
            permittedModules.remove(moduleName);
        }
        if (modified) {
            invalidateCaches(true);
            logger.info("Globally permitted module: [" + moduleName + "]");
        }
        return modified;
    }

    public static synchronized boolean blockInGuild(String moduleName) {
        boolean modified = settings.permittedInGuild.remove(moduleName);
        if (modified) {
            invalidateCaches(true);
            logger.info("Globally blocked module: [" + moduleName + "]");
        }
        return modified;
    }

    private static List<BotModule> buildGuildModuleCache() {
        List<BotModule> list = new ArrayList<>();
        for (String s : settings.permittedInGuild) {
            BotModule moduleInstance = getModuleInstance(s);
            if (moduleInstance != null) {
                list.add(moduleInstance);
            }
        }
        return list;
    }

    public enum PermitInChannelResult {
        PERMITTED_IN_CHANNEL,
        ALREADY_PERMITTED_IN_CHANNEL,
        PERMITTED_IN_GUILD_REMOVED_FROM_BLOCKLIST,
        ALREADY_PERMITTED_IN_GUILD
    }

    public static synchronized PermitInChannelResult permitInChannel(long channelID, String moduleName) {
        @NotNull var permittedSet = settings.permittedInChannel.computeIfAbsent(channelID, s -> new HashSet<>());
        @Nullable var blockedSet = settings.blockedInChannel.get(channelID);

        PermitInChannelResult result;
        if (!settings.permittedInGuild.contains(moduleName)) {
            if (permittedSet.add(moduleName)) {
                result = PermitInChannelResult.PERMITTED_IN_CHANNEL;
            } else {
                result = PermitInChannelResult.ALREADY_PERMITTED_IN_CHANNEL;
            }
            if (blockedSet != null) blockedSet.remove(moduleName);
        } else {
            if (blockedSet != null) {
                if (blockedSet.remove(moduleName)) {
                    result = PermitInChannelResult.PERMITTED_IN_GUILD_REMOVED_FROM_BLOCKLIST;
                } else {
                    result = PermitInChannelResult.ALREADY_PERMITTED_IN_GUILD;
                }
            } else {
                result = PermitInChannelResult.ALREADY_PERMITTED_IN_GUILD;
            }
        }
        invalidateCaches(true);
        logger.info("Permitted module [" + moduleName + "] in channel: " + channelID);
        return result;
    }

    public enum BlockInChannelResult {
        PERMITTED_IN_CHANNEL_NOW_BLOCKED_IN_CHANNEL,
        PERMITTED_IN_GUILD_NOW_BLOCKED_IN_CHANNEL,
        ALREADY_NOT_PERMITTED_IN_CHANNEL_OR_GUILD
    }

    public static synchronized BlockInChannelResult blockInChannel(long channelID, String moduleName) {
        @Nullable var blockedSet = settings.blockedInChannel.get(channelID);
        @Nullable var permittedSet = settings.permittedInChannel.get(channelID);

        if (settings.permittedInGuild.contains(moduleName) && (blockedSet == null || !blockedSet.contains(moduleName))) {
            settings.blockedInChannel.computeIfAbsent(channelID, l -> new HashSet<>()).add(moduleName);
            if (permittedSet != null) permittedSet.remove(moduleName);
            logger.info("Blocked module [" + moduleName + "] in channel: " + channelID);
            invalidateCaches(true);
            return BlockInChannelResult.PERMITTED_IN_GUILD_NOW_BLOCKED_IN_CHANNEL;
        } else if (permittedSet != null && permittedSet.contains(moduleName)) {
            permittedSet.remove(moduleName);
            logger.info("Blocked module [" + moduleName + "] in channel: " + channelID);
            invalidateCaches(true);
            return BlockInChannelResult.PERMITTED_IN_CHANNEL_NOW_BLOCKED_IN_CHANNEL;
        } else {
            return BlockInChannelResult.ALREADY_NOT_PERMITTED_IN_CHANNEL_OR_GUILD;
        }
    }

    public static synchronized void trim() {
        for (HashSet<String> blockedSet : settings.blockedInChannel.values()) {
            blockedSet.retainAll(settings.permittedInGuild);
        }
        for (HashSet<String> permittedSet : settings.permittedInChannel.values()) {
            permittedSet.removeAll(settings.permittedInGuild);
        }
    }

    private static class PermittedModules implements PersistentObject {
        public HashSet<String> permittedInGuild = new HashSet<>();
        public HashMap<Long, HashSet<String>> permittedInChannel = new HashMap<>();
        public HashMap<Long, HashSet<String>> blockedInChannel = new HashMap<>();
    }

    private static class CommandNode {
        private final HashMap<String, CommandNode> nodes;
        private final HashMap<String, TextCommand> commands;

        public CommandNode() {
            nodes = new HashMap<>();
            commands = new HashMap<>();
        }


        /**
         * For use in Map#computeIfAbsent; Ignores the key passed
         *
         * @param ignored Key when used in Map#computeIfAbsent; Not used.
         */
        public CommandNode(Object ignored) {
            this();
        }

        public void insert(TextCommand textCommand) throws IllegalArgumentException {
            String[] commandString = textCommand.command().split(" ");
            if (commandString.length == 0) {
                throw new IllegalArgumentException("Command name is of length 0! " + textCommand);
            }
            insert(textCommand, commandString, 0);
        }

        private void insert(TextCommand textCommand, String[] commandString, int index) {
            assert commandString.length > 0;
            if (index == commandString.length - 1) {
                final TextCommand previous = this.commands.put(commandString[index], textCommand);
                if (previous != null) logger.error("Duplicate TextCommands: " + previous + " shadowed by " + textCommand);
            } else {
                this.nodes
                        .computeIfAbsent(commandString[index], CommandNode::new)
                        .insert(textCommand, commandString, index + 1);
            }
        }

        public Optional<Tuple2<TextCommand, String>> find(String command) {
            String[] commandString = command.split(" ");
            if (commandString.length == 0) {
                return Optional.empty();
            } else {
                return find(command, commandString, 0, 0);
            }
        }

        private Optional<Tuple2<TextCommand, String>> find(String commandString, String[] split, int startFrom, int argIndex) {
            assert split.length > 0;
            if (startFrom >= split.length) {
                return Optional.empty();
            } else {
                if (split[startFrom].length() == 0) {
                    return this.find(commandString, split, startFrom + 1, argIndex + 1);
                }
                TextCommand textCommand;
                CommandNode node;
                Optional<Tuple2<TextCommand, String>> downTree;
                if ((node = nodes.get(split[startFrom])) != null) {
                    downTree = node.find(commandString, split, startFrom + 1, argIndex + 1 + split[startFrom].length());
                } else {
                    downTree = Optional.empty();
                }
                if (downTree.isPresent()) {
                    return downTree;
                } else {
                    if ((textCommand = commands.get(split[startFrom])) != null) {
                        argIndex += 1 + split[startFrom].length();
                        String parameters;
                        if (argIndex < commandString.length()) {
                            parameters = commandString.substring(argIndex);
                        } else {
                            parameters = "";
                        }
                        return Optional.of(Tuple2.of(textCommand, parameters));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }

        public Stream<TextCommand> listCommands() {
            return Stream.concat(commands.values().stream(), nodes.values().stream().flatMap(CommandNode::listCommands));
        }
    }
}
