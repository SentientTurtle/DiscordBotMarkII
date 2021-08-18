package net.sentientturtle.discordbot.components.module.command;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.EnumSet;

public interface TextCommand {
    String command();
    String syntax();
    String helpText();
    String commandPermission();
    EnumSet<Permission> botPermissions();

    void handle(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) throws Throwable;
}
