package net.sentientturtle.discordbot.components.module.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for Discord slash commands, automatically registered at bot startup
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String NULL = "[NULL]";
    String DERIVE_VALUE = "[DERIVE]";

    String commandName() default DERIVE_VALUE;
    String subcommandGroup() default NULL;
    String subcommandName() default NULL;
    String description();

    boolean canEveryoneUse() default false;

    /**
     * Annotation for command parameters
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Parameter {
        String name();
        String description();
        boolean optional() default false;
    }

    /**
     * Annotation for parameters with fixed choices
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Choices{
        String[] value();
    }
}
