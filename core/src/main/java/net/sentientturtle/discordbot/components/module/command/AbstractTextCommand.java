package net.sentientturtle.discordbot.components.module.command;

import net.dv8tion.jda.api.Permission;

import java.util.EnumSet;

/**
 * Data class stub implementation for {@link TextCommand}; Used as instance to implement {@link net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand} functionality
 */
public abstract class AbstractTextCommand implements TextCommand {
    private final String command;
    private final String syntax;
    private final String helpText;
    private final String commandPermission;
    private final EnumSet<Permission> botPermissions;

    public AbstractTextCommand(String command, String syntax, String helpText, String commandPermission, EnumSet<Permission> botPermissions) {
        this.command = command;
        this.syntax = syntax;
        this.helpText = helpText;
        this.commandPermission = commandPermission;
        this.botPermissions = botPermissions;
    }

    @Override
    public String command() {
        return command;
    }

    @Override
    public String syntax() {
        return syntax;
    }

    @Override
    public String helpText() {
        return helpText;
    }

    @Override
    public String commandPermission() {
        return commandPermission;
    }

    @Override
    public EnumSet<Permission> botPermissions() {
        return botPermissions;
    }
}
