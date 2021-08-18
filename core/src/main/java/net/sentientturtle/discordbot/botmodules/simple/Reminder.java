package net.sentientturtle.discordbot.botmodules.simple;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.sentientturtle.discordbot.components.core.Core;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand;
import net.sentientturtle.discordbot.components.persistence.PersistentObject;
import net.sentientturtle.discordbot.helpers.ErrorHelper;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import net.sentientturtle.util.TimeFormat;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.Permission.MESSAGE_WRITE;

/**
 * Module providing reminder commands
 */
public class Reminder extends BotModule implements StaticLoaded {
    private static final Map<String, TimeUnit> unitNames = new HashMap<>();

    static {
        unitNames.put("seconds", TimeUnit.SECONDS);
        unitNames.put("second", TimeUnit.SECONDS);
        unitNames.put("s", TimeUnit.SECONDS);
        unitNames.put("minutes", TimeUnit.MINUTES);
        unitNames.put("minute", TimeUnit.MINUTES);
        unitNames.put("m", TimeUnit.MINUTES);
        unitNames.put("hours", TimeUnit.HOURS);
        unitNames.put("hour", TimeUnit.HOURS);
        unitNames.put("h", TimeUnit.HOURS);
        unitNames.put("days", TimeUnit.DAYS);
        unitNames.put("day", TimeUnit.DAYS);
        unitNames.put("d", TimeUnit.DAYS);
    }

    @ATextCommand(syntax = "remindme in <duration> <unit>", helpText = "Reminds the user after the specified duration", discordPermissions = {MESSAGE_WRITE})
    public static void remindme_in(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        if (args.length() > 0) {
            String[] split = args.split(" ");
            if (split.length > 1 && split.length % 2 == 0) {
                long durationTotal = 0;
                for (int i = 0; i < split.length; i += 2) {
                    try {
                        long duration = Long.parseLong(split[i]);
                        TimeUnit unit = unitNames.get(split[i + 1].toLowerCase());
                        durationTotal += unit.toMillis(duration);
                    } catch (NumberFormatException | NullPointerException ignored) {
                        ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Invalid duration syntax!");
                        return;
                    }
                }
                long channelID = event.getChannel().getIdLong();
                long userID = Objects.requireNonNull(event.getMember()).getUser().getIdLong();
                long finalDurationTotal = durationTotal;
                Scheduling.schedule(
                        () -> {
                            var channel = Objects.requireNonNull(Core.getJDA())
                                                  .getTextChannelById(channelID);
                            var user = Objects.requireNonNull(Core.getJDA()).getUserById(userID);
                            if (channel != null && user != null) {  // If channel has been deleted since or user no longer exists, do nothing
                                channel.sendMessage(
                                        new MessageBuilder()
                                                .append(user)
                                                .mention(user)
                                                .append(" reminder from ")
                                                .append(TimeFormat.formatWDHMS(finalDurationTotal, TimeUnit.MILLISECONDS))
                                                .append(" ago.")
                                                .build()
                                ).queue();
                            }
                        },
                        durationTotal,
                        TimeUnit.MILLISECONDS
                );
            } else {
                ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Invalid duration syntax!");
            }
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Invalid duration syntax!");
        }
    }

    private static class ReminderSettings implements PersistentObject {
        // TODO: Make reminders persistent
    }
}
