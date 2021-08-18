package net.sentientturtle.discordbot.components.module.command.annotation;

import net.dv8tion.jda.api.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically implement {@link net.sentientturtle.discordbot.components.module.command.TextCommand}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ATextCommand {
    String defaultCommand = "[Default command is method name]";
    String defaultSyntax = "[Default syntax is command with no args]";
    String defaultCommandPermission = "[Default permission is `moduleName.methodname`]";

    String command() default defaultCommand;
    String syntax() default defaultSyntax;
    String helpText();
    String commandPermission() default defaultCommandPermission;
    boolean requireNSFW() default false;
    Permission[] discordPermissions();
}
