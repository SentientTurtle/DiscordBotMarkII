package net.sentientturtle.discordbot.components.core;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.self.SelfUpdateAvatarEvent;
import net.dv8tion.jda.api.events.self.SelfUpdateNameEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.module.ModuleManager;
import net.sentientturtle.discordbot.components.module.command.TextCommand;
import net.sentientturtle.discordbot.components.persistence.Persistence;
import net.sentientturtle.discordbot.components.persistence.PersistenceException;
import net.sentientturtle.discordbot.components.persistence.PersistentObject;
import net.sentientturtle.discordbot.helpers.ErrorHelper;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import net.sentientturtle.util.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * JDA Event manager, translates JDA events into appropriate module & command calls
 */
public class EventManager implements StaticLoaded, IEventManager {
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);
    private final long targetGuild;

    private static final PrefixSettings prefixSettings;
    static {
        try {
            prefixSettings = Persistence.loadObject(PrefixSettings.class, PrefixSettings::new);
        } catch (PersistenceException.ObjectLoadFailed objectLoadFailed) {
            throw new StaticInitException(objectLoadFailed);
        }
    }

    public EventManager(long targetGuild) {
        this.targetGuild = targetGuild;
    }

    public static String getApplicablePrefix(long guildID, long channelID) {
        return prefixSettings.resolvePrefix(guildID, channelID);
    }

    @Override
    public void register(@NotNull Object listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregister(@NotNull Object listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handle(@NotNull GenericEvent event) {
        try {
            //JDA Events
            if (event instanceof ReadyEvent) {
                logger.info("JDA ready!");
            } else if (event instanceof ResumedEvent) {
                logger.info("JDA resumed!");
            } else if (event instanceof ReconnectedEvent) {
                logger.info("JDA reconnected!");
            } else if (event instanceof DisconnectEvent) {
                logger.info("JDA disconnected!");
            } else if (event instanceof ShutdownEvent) {
                logger.info("JDA shutdown!");
            } else if (event instanceof GuildMessageReceivedEvent) {
                var guildEvent = (GuildMessageReceivedEvent) event;
                var guildID = guildEvent.getGuild().getIdLong();
                if (guildID == targetGuild && !guildEvent.isWebhookMessage()) {
                    var channelID = guildEvent.getChannel().getIdLong();
                    String applicablePrefix = prefixSettings.resolvePrefix(guildID, channelID);
                    String messageContent = guildEvent.getMessage().getContentStripped();
                    if (messageContent.startsWith(applicablePrefix)) {
                        String commandString = messageContent.substring(applicablePrefix.length());

                        Optional<Tuple2<TextCommand, String>> commandOptional = ModuleManager.getCommandInChannel(channelID, commandString);
                        if (commandOptional.isPresent()) {
                            EnumSet<Permission> selfPermissions = guildEvent.getGuild()
                                    .getSelfMember()
                                    .getPermissions(guildEvent.getChannel());

                            Tuple2<TextCommand, String> commandTuple = commandOptional.get();
                            var command = commandTuple.one;
                            var args = commandTuple.two;

                            //noinspection ConstantConditions
                            if (PermissionManager.checkUserPermission(
                                    command.commandPermission(),
                                    guildID,
                                    guildEvent.getGuild().getOwnerIdLong(),
                                    channelID,
                                    guildEvent.getAuthor().getIdLong(),
                                    guildEvent.getMember().getRoles()       // We check that this message is not a webhook-message, so guildEvent.getMember may not be null
                            )) {
                                if (selfPermissions.containsAll(command.botPermissions())) {
                                    try {
                                        command.handle(args, guildEvent, selfPermissions);
                                    } catch (Throwable t) {
                                        logger.error("Command [" + command.getClass().getSimpleName() + "] execution failed!", t);
                                        ErrorHelper.reportFromGuildEvent(guildEvent, selfPermissions, "⚠", "Command 「" + command.command() + "」 resulted in an error: " + t.getClass().getSimpleName());
                                    }
                                } else {
                                    var missingPermissions = command.botPermissions().clone();
                                    missingPermissions.removeAll(selfPermissions);
                                    ErrorHelper.reportFromGuildEvent(guildEvent, selfPermissions, "⛔", "Command 「" + command.command() + "」 requires me to have discord permissions: " + missingPermissions);
                                }
                            } else {
                                ErrorHelper.reportFromGuildEvent(guildEvent, selfPermissions, "🚫", "You must have bot-permission `" + command.commandPermission() + "` to use Command 「" + command.command() + "」!");
                            }
                        }

//                    Stream<Module> modules = ModuleManager.getModulesForChannel(channelID);
                    }
                }
            } else if (event instanceof PrivateMessageReceivedEvent) {
                ((PrivateMessageReceivedEvent) event).getChannel()
                        .sendMessage("Commands may not be issued through private messages, please issue commands in the Guild!").queue();
            } else if (event instanceof SelfUpdateAvatarEvent) {
                logger.info("Updated avatar!");
            } else if (event instanceof SelfUpdateNameEvent) {
                logger.info("Updated name from: [" + ((SelfUpdateNameEvent) event).getOldName() + "] to: [" + ((SelfUpdateNameEvent) event).getNewName() + "]");
            }
        } catch (Throwable t) {
            logger.error("Throwable in EventManager!", t);
        }

        Long guildID = null;
        Long channelID = null;

        if (event instanceof GenericGuildEvent) guildID = ((GenericGuildEvent) event).getGuild().getOwnerIdLong();
        if (event instanceof GenericGuildMessageEvent) channelID = ((GenericGuildMessageEvent) event).getChannel().getIdLong();

        if (guildID != null && guildID == Core.targetGuildId()) {
            (channelID != null ? ModuleManager.listModulesForChannel(channelID) : ModuleManager.listModulesForGuild())
                    .filter(module -> module instanceof EventListener)
                    .forEach(eventListener -> {
                        try {
                            ((EventListener) eventListener).onEvent(event);
                        } catch (Throwable t) {
                            logger.warn("Error in module eventListener " + eventListener.getModuleName(), t);
                        }
                    });
        }
    }

    @NotNull
    @Override
    public List<Object> getRegisteredListeners() {
        throw new UnsupportedOperationException();
    }

    private static class PrefixSettings implements PersistentObject {
        public String globalPrefix = "!";
        public HashMap<Long, String> guildPrefixMap = new HashMap<>();
        public HashMap<Long, String> channelPrefixMap = new HashMap<>();

        public String resolvePrefix(long guildID, long channelID) {
            String channelPrefix = channelPrefixMap.get(channelID);
            if (channelPrefix != null) return channelPrefix;
            String guildPrefix = guildPrefixMap.get(guildID);
            if (guildPrefix != null) return guildPrefix;
            if (globalPrefix != null) return globalPrefix;
            logger.error("Global command prefix is null! Resetting to exclamation mark!");
            return globalPrefix = "!";
        }
    }
}
