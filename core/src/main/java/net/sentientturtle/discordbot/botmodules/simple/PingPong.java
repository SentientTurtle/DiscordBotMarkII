package net.sentientturtle.discordbot.botmodules.simple;

import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.Command;
import net.sentientturtle.discordbot.components.module.command.CommandCall;

/**
 * Minimal test-module providing a 'ping' command
 */
public class PingPong extends BotModule {
    @Command(description = "Ping-pong")
    public static void ping(
            CommandCall commandCall,
            @Command.Parameter(name = "message", description = "Message to pong back", optional = true) String message
    ) {
        commandCall.reply(message != null ? message : "Pong!", true);
    }
}
