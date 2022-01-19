package net.sentientturtle.discordbot.components.core;

import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.self.SelfUpdateAvatarEvent;
import net.dv8tion.jda.api.events.self.SelfUpdateNameEvent;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.sentientturtle.discordbot.components.interaction.ButtonManager;
import net.sentientturtle.discordbot.components.interaction.SelectionMenuManager;
import net.sentientturtle.discordbot.components.interaction.SlashCommandManager;
import net.sentientturtle.discordbot.components.module.ModuleManager;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * JDA Event manager, translates JDA events into appropriate module & command calls
 */
public class EventManager implements StaticLoaded, IEventManager {
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);

    public EventManager() {}

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
            } else if (event instanceof SlashCommandEvent slashCommandEvent) {
                SlashCommandManager.handleEvent(slashCommandEvent);
            } else if (event instanceof SelectionMenuEvent selectionMenuEvent) {
                SelectionMenuManager.handleEvent(selectionMenuEvent);
            } else if (event instanceof ButtonClickEvent buttonClickEvent) {
                ButtonManager.handleEvent(buttonClickEvent);
            } else if (event instanceof PrivateMessageReceivedEvent privateMessageReceivedEvent) {
                privateMessageReceivedEvent.getChannel()
                        .sendMessage("Commands may not be issued through private messages, please issue commands in the Guild!").queue();
            } else if (event instanceof SelfUpdateAvatarEvent) {
                logger.info("Updated avatar!");
            } else if (event instanceof SelfUpdateNameEvent) {
                logger.info("Updated name from: [" + ((SelfUpdateNameEvent) event).getOldName() + "] to: [" + ((SelfUpdateNameEvent) event).getNewName() + "]");
            }
            ModuleManager.onEvent(event);
        } catch (Throwable t) {
            logger.error("Throwable in EventManager!", t);
        }
    }

    @NotNull
    @Override
    public List<Object> getRegisteredListeners() {
        throw new UnsupportedOperationException();
    }
}
