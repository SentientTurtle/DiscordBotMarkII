package net.sentientturtle.discordbot.helpers;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.persistence.Persistence;
import net.sentientturtle.discordbot.components.persistence.PersistenceException;
import net.sentientturtle.discordbot.components.persistence.PersistentObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;

public class ErrorHelper {
    private static final ErrorReportingSettings errorReportingSettings;

    static {
        try {
            errorReportingSettings = Persistence.loadObject(ErrorReportingSettings.class, ErrorReportingSettings::new);
        } catch (PersistenceException.ObjectLoadFailed objectLoadFailed) {
            throw new StaticInitException(objectLoadFailed);
        }
    }

    public static void reportFromGuildEvent(@NotNull GuildMessageReceivedEvent guildEvent, EnumSet<Permission> selfPermissions, @NotNull String emote, @NotNull String channelMessage) {
        reportFromGuildEvent(guildEvent, selfPermissions, emote, channelMessage, null);
    }

    public static void reportFromGuildEvent(@NotNull GuildMessageReceivedEvent guildEvent, EnumSet<Permission> selfPermissions, @NotNull String emote, @NotNull String channelMessage, @Nullable String directMessage) {
        Objects.requireNonNull(guildEvent);
        Objects.requireNonNull(selfPermissions);
        Objects.requireNonNull(emote);
        Objects.requireNonNull(channelMessage);

        var canWrite = guildEvent.getChannel().canTalk();
        var canEmote = selfPermissions.contains(Permission.MESSAGE_ADD_REACTION);

        if (canWrite && errorReportingSettings.enableInChannelErrors) {
            guildEvent.getChannel()
                    .sendMessage(emote + " " + channelMessage)
                    .queue();
        } else if (errorReportingSettings.enableInDMErrors) {
            if (directMessage != null) {
                guildEvent.getAuthor()
                        .openPrivateChannel()
                        .flatMap(channel -> channel.sendMessage(emote + " " + directMessage))
                        .queue();
            } else {
                guildEvent.getAuthor()
                        .openPrivateChannel()
                        .flatMap(channel -> channel.sendMessage(emote + " " + channelMessage))
                        .queue();
            }
        } else if (canEmote && errorReportingSettings.enableInEmoteErrors) {
            guildEvent.getMessage().addReaction(emote).queue();
        }  // else: Do nothing
    }

    private static class ErrorReportingSettings implements PersistentObject {
        public boolean enableInChannelErrors = true;
        public boolean enableInDMErrors = true;
        public boolean enableInEmoteErrors = true;
    }
}
