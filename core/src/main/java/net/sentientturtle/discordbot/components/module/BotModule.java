package net.sentientturtle.discordbot.components.module;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand;
import net.sentientturtle.discordbot.components.module.command.AbstractTextCommand;
import net.sentientturtle.discordbot.components.module.command.TextCommand;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import net.sentientturtle.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Top level class for bot modules<br>
 * Invisibly implements {@link StaticLoaded} for this class only; Subclasses must implement it themselves if needed.
 */
public abstract class BotModule {
    private static final HashMap<Class<? extends BotModule>, BotModule> instances = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(BotModule.class);
    private static Method commandMethod = null;
    private List<TextCommand> commandCache = null;

    static {
        for (Method method : TextCommand.class.getMethods()) {
            if (method.getName().equals("handle")) {
                if (commandMethod != null) throw new StaticInitException("ITextCommand has multiple #handle methods!");
                commandMethod = method;
            }
        }
        if (commandMethod == null) throw new StaticInitException("Unable to ascertain ITextCommand 'handle' method!");
    }

    /*
     * A somewhat dirty hack; Were BotModule to implement StaticLoaded directly, all modules extending it would also be statically initialised.
     * To avoid this behavior, BotModule is statically initialised by a hidden inner class
     */
    private static void staticallyInitialise() {};
    private static final class StaticInitializerForBotModule implements StaticLoaded {
        static {
            staticallyInitialise();
        }
    }

    @SuppressWarnings("unchecked")  // Instance cache is indexed by
    public static <T extends BotModule> T getInstance(Class<T> moduleClass) {
        return (T) instances.computeIfAbsent(moduleClass, aClass -> {
            try {
                var constructor = aClass.getConstructor();
                return constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                return Util.sneakyThrow(e);
            }
        });
    }

    public final String getModuleName() {
        return this.getClass().getSimpleName().toLowerCase();
    }

    public String getPermissionPrefix() {
        return this.getClass().getSimpleName().toLowerCase();
    }

    public final List<TextCommand> getCommands() {
        if (commandCache != null) {
            return commandCache;
        } else {
            return commandCache = buildCommandCache();
        }
    }

    // Evil reflection wizardry to parse CommandMethod annotations
    protected List<TextCommand> buildCommandCache() {
        List<TextCommand> textCommandList = new ArrayList<>();
        for (Method method : this.getClass().getDeclaredMethods()) {
            var annotation = method.getAnnotation(ATextCommand.class);
            if (annotation != null) {
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers)) {
                    Class<?>[] methodParameters = method.getParameterTypes();
                    if (Arrays.equals(methodParameters, commandMethod.getParameterTypes())) {
                        String command;
                        if (annotation.command().equals(ATextCommand.defaultCommand)) {
                            command = method.getName().toLowerCase().replace('_', ' ');
                        } else {
                            command = annotation.command().toLowerCase();
                        }
                        String syntax;
                        if (annotation.syntax().equals(ATextCommand.defaultSyntax)) {
                            syntax = command;
                        } else {
                            syntax = annotation.syntax().toLowerCase();
                        }
                        String commandPermission;
                        if (annotation.commandPermission().equals(ATextCommand.defaultCommandPermission)) {
                            commandPermission = getModuleName().toLowerCase() + "." + method.getName().toLowerCase();
                        } else {
                            commandPermission = annotation.commandPermission().toLowerCase();
                        }

                        textCommandList.add(
                                new AbstractTextCommand(
                                        command,
                                        syntax,
                                        annotation.helpText(),
                                        commandPermission,
                                        EnumSet.of(annotation.discordPermissions()[0], annotation.discordPermissions())
                                ) {
                                    private final BotModule module = BotModule.this;
                                    private final Method targetMethod = method;

                                    @Override
                                    public void handle(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
                                        try {
                                            targetMethod.invoke(module, args, event, selfPermissions);
                                        } catch (IllegalAccessException e) {
                                            logger.error("Command invocation is illegal!", e);
                                        } catch (InvocationTargetException e) {
                                            if (e.getCause() != null) {
                                                Util.sneakyThrow(e.getCause()); // Command#handle throws `Throwable`, so we can sneakyThrow the InvocationTargetException's cause
                                            } else {
                                                logger.warn("InvocationTargetException thrown with 'null' cause! This should normally not happen, but permitted by stdlib.", e);
                                                Util.sneakyThrow(e);
                                            }
                                        }
                                    }

                                    @Override
                                    public String toString() {
                                        return targetMethod.toString();
                                    }
                                }
                        );
                    } else {
                        logger.error("Command [" + this.getClass().getCanonicalName() + "#" + method.getName() + "] does not have parameters that match TextCommand#handle!");
                    }
                } else {
                    logger.error("Command [" + this.getClass().getCanonicalName() + "#" + method.getName() + "] is not public and cannot be called!");
                }
            }
        }
        return textCommandList;
    }
}