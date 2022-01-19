package net.sentientturtle.discordbot.components.module;

import net.dv8tion.jda.api.entities.AbstractChannel;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.sentientturtle.discordbot.components.module.command.Command;
import net.sentientturtle.discordbot.components.module.command.CommandCall;
import net.sentientturtle.discordbot.components.module.command.UnifiedCommand;
import net.sentientturtle.discordbot.components.permission.BotPermission;
import net.sentientturtle.discordbot.loader.Loader;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import net.sentientturtle.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * Top level class for bot modules
 */
public abstract class BotModule implements StaticLoaded {
    private static final HashMap<Class<? extends BotModule>, BotModule> instances = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(BotModule.class);
    private static final Method commandMethod = null;

    private final List<UnifiedCommand> commands;

    protected BotModule() {
        var methods = this.getClass().getDeclaredMethods();
        Loader.ensureStaticLoad();  // Ensure static loading has happened in order to make sure all listener-annotations have been registered.
        commands = new ArrayList<>();
        methodloop:
        for (Method method : methods) {
            var commandAnnotation = method.getAnnotation(Command.class);
            if (commandAnnotation != null) {
                var commandName = commandAnnotation.commandName().equals(Command.DERIVE_VALUE) ? method.getName() : commandAnnotation.commandName();
                var subcommandGroup = commandAnnotation.subcommandGroup().equals(Command.NULL) ? null : commandAnnotation.subcommandGroup();
                var subcommandName = commandAnnotation.subcommandName().equals(Command.NULL) ? null : commandAnnotation.subcommandName();

                List<UnifiedCommand.Parameter> requiredParameters = new ArrayList<>(method.getParameterCount());
                List<UnifiedCommand.Parameter> optionalParameters = new ArrayList<>(method.getParameterCount());

                boolean commandCallFirstParameterFound = false;

                for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                    if (!commandCallFirstParameterFound) {
                        if (parameter.getType().isAssignableFrom(CommandCall.class)) {
                            commandCallFirstParameterFound = true;
                            continue;
                        } else {
                            logger.error("First parameter for Command method is not CommandCall @ " + method);
                            continue methodloop;
                        }
                    }
                    var parameterAnnotation = parameter.getAnnotation(Command.Parameter.class);
                    if (parameterAnnotation == null) {
                        logger.error("Parameter missing annotation on " + method + "; parameter: " + parameter);
                        continue methodloop;
                    }

                    var choiceAnnotation = parameter.getAnnotation(Command.Choices.class);
                    UnifiedCommand.Parameter.Choice[] choices = new UnifiedCommand.Parameter.Choice[0];

                    var type = parameter.getType();
                    UnifiedCommand.Parameter.ParameterType parameterType;
                    if (type.isAssignableFrom(String.class)) {
                        parameterType = UnifiedCommand.Parameter.ParameterType.STRING;

                        if (choiceAnnotation != null) {
                            String[] names = choiceAnnotation.value();
                            if (names.length == 0) {
                                logger.error("Parameter choices annotation was used without choices specified on " + method + "; parameter: " + parameter);
                                continue methodloop;
                            } else {
                                choices = new UnifiedCommand.Parameter.Choice[names.length];
                                for (int i = 0; i < names.length; i++) {
                                    choices[i] = new UnifiedCommand.Parameter.Choice(names[i], names[i]);
                                }
                            }
                        }
                    } else if (type.isAssignableFrom(long.class) || type.isAssignableFrom(Long.class)) {
                        if (parameterAnnotation.optional() && type.isAssignableFrom(long.class)) {
                            logger.error("Optional parameter may not be of primitive type for Command method " + method + "; parameter: " + parameter);
                            continue methodloop;
                        }
                        if (choiceAnnotation != null) {
                                logger.error("Parameter choices annotation may only be used on String parameters! " + method + "; parameter: " + parameter);
                                continue methodloop;
                        }
                        parameterType = UnifiedCommand.Parameter.ParameterType.LONG;
                    } else if (type.isAssignableFrom(boolean.class)) {
                        if (parameterAnnotation.optional()) {
                            logger.error("Optional parameter may not be of primitive type for Command method " + method + "; parameter: " + parameter);
                            continue methodloop;
                        }
                        if (choiceAnnotation != null) {
                            logger.error("Parameter choices annotation may only be used on String parameters! " + method + "; parameter: " + parameter);
                            continue methodloop;
                        }
                        parameterType = UnifiedCommand.Parameter.ParameterType.BOOLEAN;
                    } else if (type.isAssignableFrom(Boolean.class)) {
                        if (choiceAnnotation != null) {
                            logger.error("Parameter choices annotation may only be used on String parameters! " + method + "; parameter: " + parameter);
                            continue methodloop;
                        }
                        parameterType = UnifiedCommand.Parameter.ParameterType.BOOLEAN;
                    } else if (type.isAssignableFrom(User.class)) {
                        if (choiceAnnotation != null) {
                            logger.error("Parameter choices annotation may only be used on String parameters! " + method + "; parameter: " + parameter);
                            continue methodloop;
                        }
                        parameterType = UnifiedCommand.Parameter.ParameterType.USER;
                    } else if (type.isAssignableFrom(AbstractChannel.class)) {
                        if (choiceAnnotation != null) {
                            logger.error("Parameter choices annotation may only be used on String parameters! " + method + "; parameter: " + parameter);
                            continue methodloop;
                        }
                        parameterType = UnifiedCommand.Parameter.ParameterType.CHANNEL;
                    } else if (type.isAssignableFrom(Role.class)) {
                        if (choiceAnnotation != null) {
                            logger.error("Parameter choices annotation may only be used on String parameters! " + method + "; parameter: " + parameter);
                            continue methodloop;
                        }
                        parameterType = UnifiedCommand.Parameter.ParameterType.ROLE;
                    } else if (type.isAssignableFrom(IMentionable.class)) {
                        if (choiceAnnotation != null) {
                            logger.error("Parameter choices annotation may only be used on String parameters! " + method + "; parameter: " + parameter);
                            continue methodloop;
                        }
                        parameterType = UnifiedCommand.Parameter.ParameterType.MENTIONABLE;
                    } else {
                        logger.error("Invalid parameter type for Command method " + method + "; parameter: " + parameter);
                        continue methodloop;
                    }

                    var commandParameter = new UnifiedCommand.Parameter(
                            parameterType,
                            parameterAnnotation.name(),
                            parameterAnnotation.description(),
                            choices
                    );

                    if (parameterAnnotation.optional()) {
                        optionalParameters.add(commandParameter);
                    } else {
                        requiredParameters.add(commandParameter);
                    }
                }

                commands.add(
                        new UnifiedCommand(
                                commandName,
                                subcommandGroup,
                                subcommandName,
                                commandAnnotation.description(),
                                requiredParameters.toArray(UnifiedCommand.Parameter[]::new),
                                optionalParameters.toArray(UnifiedCommand.Parameter[]::new),
                                commandAnnotation.canEveryoneUse() ? BotPermission.EVERYONE() : BotPermission.STRING("cmd:" + commandName),
                                commandCall -> {
                                    Object[] callParameters = commandCall.getParameters();
                                    Object[] parameters = new Object[callParameters.length + 1];
                                    parameters[0] = commandCall;
                                    System.arraycopy(callParameters, 0, parameters, 1, callParameters.length);
                                    method.invoke(BotModule.this, parameters);
                                }
                        )
                );
            }
        }
    }

    protected void addTopLevelCommandDescription(String commandName, String description) throws IllegalArgumentException {
        ModuleManager.addTopLevelCommandDescription(commandName, description);
    }

    protected void addMidLevelCommandDescription(String commandName, String description) throws IllegalArgumentException {
        ModuleManager.addMidLevelCommandDescription(commandName, description);
    }

    @SuppressWarnings("unchecked")  // Instance cache is indexed by class, so we know casting is safe.
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

    public final Stream<UnifiedCommand> getCommands() {
        return commands.stream();
    }
}