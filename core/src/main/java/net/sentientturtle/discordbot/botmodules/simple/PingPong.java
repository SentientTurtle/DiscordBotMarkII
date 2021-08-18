package net.sentientturtle.discordbot.botmodules.simple;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand;

import java.util.EnumSet;

import static net.dv8tion.jda.api.Permission.*;

/**
 * Minimal test-module providing a 'ping' command
 */
public class PingPong extends BotModule {
    @ATextCommand(helpText = "Replies with 'Pong!'", discordPermissions = {MESSAGE_WRITE})
    public static void ping(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        event.getChannel().sendMessage("Pong!").queue();
    }
}
