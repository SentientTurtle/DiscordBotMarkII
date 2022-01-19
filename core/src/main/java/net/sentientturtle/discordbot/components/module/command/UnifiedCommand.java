package net.sentientturtle.discordbot.components.module.command;

import net.sentientturtle.discordbot.components.permission.BotPermission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data record for calls; Automatically generated from {@link Command} annotated methods
 */
public record UnifiedCommand(
        @NotNull String commandName,
        @Nullable String subcommandGroup,
        @Nullable String subcommandName,
        @NotNull String description,
        @NotNull Parameter[] requiredParameters,
        @NotNull Parameter[] optionalParameters,
        @NotNull BotPermission commandUserPermission,
        @NotNull CommandCallable command
) {
    public UnifiedCommand {
        if (commandName.length() < 1 || commandName.length() > 32) throw new IllegalArgumentException("CommandName must be 1-32 characters in length");
        if (!commandName.matches("^[\\w-]+$")) throw new IllegalArgumentException("CommandName may only contain alphanumeric characters, hyphens, and underscores");
        if (description.length() < 1 || description.length() > 100) throw new IllegalArgumentException("Description must be 1-100 characters in length");
        if (subcommandGroup != null && (subcommandGroup.length() < 1 || subcommandGroup.length() > 32)) throw new IllegalArgumentException("SubcommandGroup must be 1-32 characters in length");
        if (subcommandGroup != null && !subcommandGroup.matches("^[\\w-]+$")) throw new IllegalArgumentException("SubcommandGroup may only contain alphanumeric characters and underscores");
        if (subcommandName != null && (subcommandName.length() < 1 || subcommandName.length() > 32)) throw new IllegalArgumentException("SubcommandName must be 1-32 characters in length");
        if (subcommandName != null && !subcommandName.matches("^[\\w-]+$")) throw new IllegalArgumentException("SubcommandName may only contain alphanumeric characters and underscores");
        if (subcommandGroup != null && subcommandName == null) throw new IllegalArgumentException("SubcommandGroup may not be specified without subcommandName");
        if (requiredParameters.length + optionalParameters.length > 25) throw new IllegalArgumentException("Too many parameters (Must be <= 25, was: " + (requiredParameters.length + optionalParameters.length) + ")");
    }

    public String path() {
        if (subcommandGroup != null) {
            return subcommandName + '/' + subcommandGroup + '/' + subcommandName;
        } else if (subcommandName != null) {
            return commandName + '/' + subcommandName;
        } else {
            return commandName;
        }
    }

    public record Parameter(@NotNull ParameterType parameterType, @NotNull String name, @NotNull String description, Choice... choices) {
        public Parameter {
            if (name.length() < 1 || name.length() > 32) throw new IllegalArgumentException("Name must be 1-32 characters in length");
            if (description.length() < 1 || description.length() > 100) throw new IllegalArgumentException("Description must be 1-100 characters in length");
            if (!name.matches("^[\\w-]+$")) throw new IllegalArgumentException("Parameter name may only contain alphanumeric characters and underscores");
            if (choices == null) throw new NullPointerException("Choices may not be null, and must be an empty array!");
            if (choices.length > 0) {
                switch (parameterType) {
                    case STRING -> {
                        for (int i = 0; i < choices.length; i++) {
                            if (!(choices[i].value instanceof String)) {
                                throw new IllegalArgumentException("String parameter may only have String choices! (choice #" + i + " was " + choices[i].getClass().getName() + ")");
                            }
                        }
                    }
                    case LONG -> {
                        for (int i = 0; i < choices.length; i++) {
                            if (!(choices[i].value instanceof Long)) {
                                throw new IllegalArgumentException("Integer parameter may only have Long choices! (choice #" + i + " was " + choices[i].getClass().getName() + ")");
                            }
                        }
                    }
                    case BOOLEAN, USER, CHANNEL, ROLE, MENTIONABLE -> throw new IllegalArgumentException("Fixed choices may not be used with " + parameterType + " type parameters!");
                }
            }
        }

        public enum ParameterType {
            STRING,
            LONG,
            BOOLEAN,
            USER,
            CHANNEL,
            ROLE,
            MENTIONABLE,
        }

        public record Choice(String name, Object value) {
            public Choice {
                if (!(value instanceof String || value instanceof Integer)) {
                    throw new IllegalArgumentException("Choice may not contain a value that is neither a String nor an Integer!");
                }
            }
        }
    }

    @FunctionalInterface
    public interface CommandCallable {
        void call(CommandCall commandCall) throws Throwable;
    }
}
