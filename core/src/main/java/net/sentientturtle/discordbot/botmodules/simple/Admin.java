package net.sentientturtle.discordbot.botmodules.simple;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.sentientturtle.discordbot.components.core.Shutdown;
import net.sentientturtle.discordbot.helpers.ErrorHelper;
import net.sentientturtle.discordbot.components.core.EventManager;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.ModuleManager;
import net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand;
import net.sentientturtle.util.NotImplementedException;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.dv8tion.jda.api.Permission.MESSAGE_WRITE;

/**
 * Module providing various administrative commands
 */
public class Admin extends BotModule {
    private final Pattern permitBlockPattern = Pattern.compile("([a-zA-Z]+?)\\s+in\\s+(?:<#(\\d+)>|(server|guild))");

    @ATextCommand(helpText = "Shuts down the bot", discordPermissions = {MESSAGE_WRITE})
    public void shutdown(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        event.getChannel().sendMessage("Shutting down...").complete();
        Shutdown.shutdownAll(true, 0);
    }

    @ATextCommand(syntax = "permit <module> in {#<channel>|guild}", helpText = "Permits a module in a channel or the entire guild", discordPermissions = {MESSAGE_WRITE})
    public void permit(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        String rawMessage = MarkdownSanitizer.sanitize(event.getMessage().getContentRaw());
        int argIndex = Math.min(rawMessage.length(), EventManager.getApplicablePrefix(event.getGuild().getIdLong(), event.getChannel().getIdLong()).length() + "permit".length() + 1);
        String rawArgs = rawMessage.substring(argIndex);

        Matcher matcher = permitBlockPattern.matcher(rawArgs);
        if (matcher.matches()) {
            var module = matcher.group(1);
            if (!ModuleManager.moduleExists(module)) {
                ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Module does not exist!");
            } else {
                var channelID = matcher.group(2);
                if (channelID != null) {
                    GuildChannel targetChannel = event.getGuild().getGuildChannelById(channelID);
                    if (targetChannel instanceof Category) {
                        ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Target channel is a Category instead of a Text Channel!");
                    } else if (targetChannel instanceof StoreChannel) {
                        ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Target channel is a Store Channel instead of a Text Channel!");
                    } else if (targetChannel instanceof VoiceChannel) {
                        ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Target channel is a Voice Channel instead of a Text Channel!");
                    } else if (targetChannel instanceof TextChannel) {
                        var textChannel = (TextChannel) targetChannel;
                        var builder = new MessageBuilder();
                        var message = switch (ModuleManager.permitInChannel(Long.parseLong(channelID), module)) {
                            case PERMITTED_IN_CHANNEL -> builder.append("Module ").appendCodeLine(module).append(" now permitted in ").append(textChannel).build();
                            case ALREADY_PERMITTED_IN_CHANNEL -> builder.append("Module ").appendCodeLine(module).append(" already permitted in ").append(textChannel).build();
                            case PERMITTED_IN_GUILD_REMOVED_FROM_BLOCKLIST -> builder.append("Module ").appendCodeLine(module).append(" already permitted in server, removed from ").append(textChannel).append(" blocklist").build();
                            case ALREADY_PERMITTED_IN_GUILD -> builder.append("Module ").appendCodeLine(module).append(" already permitted server-wide").build();
                        };
                        event.getChannel().sendMessage(message).queue();
                    }
                } else {
                    var guild = matcher.group(3);
                    if (guild.equals("server") || guild.equals("guild")) {
                        boolean permissionsChanged = ModuleManager.permitInGuild(module);
                        if (permissionsChanged) {
                            event.getChannel().sendMessage(new MessageBuilder("Module ").appendCodeLine(module).append(" now permitted server-wide!").build()).queue();
                        } else {
                            event.getChannel().sendMessage(new MessageBuilder("Module ").appendCodeLine(module).append(" was already permitted server-wide!").build()).queue();
                        }
                    } else { // Unreachable per regex
                        throw new IllegalStateException("Fault in permit regex!");
                    }
                }
            }
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Invalid format!");
        }
    }

    @ATextCommand(syntax = "block <module> in {#<channel>|guild}", helpText = "Blocks a module in a channel or the entire guild", discordPermissions = {MESSAGE_WRITE})
    public void block(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        String rawMessage = MarkdownSanitizer.sanitize(event.getMessage().getContentRaw());
        int argIndex = Math.min(rawMessage.length(), EventManager.getApplicablePrefix(event.getGuild().getIdLong(), event.getChannel().getIdLong()).length() + "permit".length() + 1);
        String rawArgs = rawMessage.substring(argIndex);

        Matcher matcher = permitBlockPattern.matcher(rawArgs);
        if (matcher.matches()) {
            var module = matcher.group(1);
            if (!ModuleManager.moduleExists(module)) {
                ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Module does not exist!");
            } else {
                var channelID = matcher.group(2);
                if (channelID != null) {
                    GuildChannel targetChannel = event.getGuild().getGuildChannelById(channelID);
                    if (targetChannel instanceof Category) {
                        ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Target channel is a Category instead of a Text Channel!");
                    } else if (targetChannel instanceof StoreChannel) {
                        ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Target channel is a Store Channel instead of a Text Channel!");
                    } else if (targetChannel instanceof VoiceChannel) {
                        ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Target channel is a Voice Channel instead of a Text Channel!");
                    } else if (targetChannel instanceof TextChannel) {
                        var textChannel = (TextChannel) targetChannel;
                        var builder = new MessageBuilder();
                        var message = switch (ModuleManager.blockInChannel(Long.parseLong(channelID), module)) {
                            case PERMITTED_IN_CHANNEL_NOW_BLOCKED_IN_CHANNEL -> builder.append("Module ").appendCodeLine(module).append(" was channel-permitted in ").append(textChannel).append(", now blocked").build();
                            case PERMITTED_IN_GUILD_NOW_BLOCKED_IN_CHANNEL -> builder.append("Module ").appendCodeLine(module).append(" was server-permitted, now blocked in ").append(textChannel).build();
                            case ALREADY_NOT_PERMITTED_IN_CHANNEL_OR_GUILD -> builder.append("Module ").appendCodeLine(module).append(" was already blocked in ").append(textChannel).build();
                        };
                        event.getChannel().sendMessage(message).queue();
                    }
                } else {
                    var guild = matcher.group(3);
                    if (guild.equals("server") || guild.equals("guild")) {
                        boolean permissionsChanged = ModuleManager.blockInGuild(module);
                        if (permissionsChanged) {
                            event.getChannel().sendMessage(new MessageBuilder("Module ").appendCodeLine(module).append(" now blocked server-wide!").build()).queue();
                        } else {
                            event.getChannel().sendMessage(new MessageBuilder("Module ").appendCodeLine(module).append(" was already blocked server-wide!").build()).queue();
                        }
                    } else { // Unreachable per regex
                        throw new IllegalStateException("Fault in permit regex!");
                    }
                }
            }
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Invalid format!");
        }
    }

    @ATextCommand(syntax = "trim permissions", helpText = "Trims permissions", discordPermissions = {MESSAGE_WRITE})
    public void trim_permission(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        ModuleManager.trim();
        event.getChannel().sendMessage("Trimmed module permissions!").complete();
    }

    @ATextCommand(syntax = "invalidate caches [reload=true]", helpText = "Invalidates caches", discordPermissions = {MESSAGE_WRITE})
    public void invalidate_caches(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        throw new NotImplementedException();    // TODO: Implement or remove command
    }
}
