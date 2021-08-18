package net.sentientturtle.discordbot.botmodules.simple;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.ModuleManager;
import net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand;
import net.sentientturtle.discordbot.helpers.ArgumentHelper;
import net.sentientturtle.discordbot.helpers.ErrorHelper;
import net.sentientturtle.discordbot.helpers.MessageHelper;

import java.util.EnumSet;

/**
 * Module providing in-discord help about commands and modules
 */
public class Help extends BotModule {
    @ATextCommand(syntax = "list commands [page=1]", helpText = "Lists commands available in this channel", discordPermissions = {Permission.MESSAGE_WRITE})
    public static void list_commands(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        try {
            int page = ArgumentHelper.parsePage(args, 1);

            var message = MessageHelper.paginate(
                    "Commands available in this channel",
                    ModuleManager.listCommandsForChannel(event.getChannel().getIdLong())
                            .map(textCommand -> String.format("%-40s %s", textCommand.syntax(), textCommand.helpText()))
                            .sorted(),
                    Math.max(1, page)
            );

            event.getChannel().sendMessage(message).complete();
        } catch (ArgumentHelper.ParseException e) {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", e.channelMessage);
        }
    }

    @ATextCommand(syntax = "list modules [page=1]", helpText = "Lists modules available in this channel", discordPermissions = {Permission.MESSAGE_WRITE})
    public static void list_modules(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        try {
            int page = ArgumentHelper.parsePage(args, 1);

            var message = MessageHelper.paginate(
                    "Modules available in this channel",
                    ModuleManager.listModulesForChannel(event.getChannel().getIdLong())
                            .map(BotModule::getModuleName)
                            .sorted(),
                    Math.max(1, page)
            );

            event.getChannel().sendMessage(message).complete();
        } catch (ArgumentHelper.ParseException e) {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", e.channelMessage);
        }
    }

}
