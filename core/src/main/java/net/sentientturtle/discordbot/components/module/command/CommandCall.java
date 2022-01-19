package net.sentientturtle.discordbot.components.module.command;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class CommandCall {
    protected abstract SlashCommandEvent getEvent();
    public abstract UnifiedCommand getCommand();
    public abstract Object[] getParameters();

    public void error(String message) {
        getEvent().reply(message).setEphemeral(true).queue();
    }

    public void reply(String message) {
        reply(message, false);
    }

    public void reply(String message, boolean ephemeral) {
        reply(message, ephemeral, null);
    }

    public void reply(String message, boolean ephemeral, Consumer<? super InteractionHook> onComplete) {
        getEvent().reply(message).setEphemeral(ephemeral).queue(onComplete);
    }

    public void reply(Message message) {
        reply(message, false);
    }

    public void reply(Message message, boolean ephemeral) {
        reply(message, ephemeral, null);
    }

    public void reply(Message message, boolean ephemeral, Consumer<? super InteractionHook> onComplete) {
        getEvent().reply(message).setEphemeral(ephemeral).queue();
    }

    public User getUser() {
        return getEvent().getUser();
    }

    @SuppressWarnings("ConstantConditions")
    public @NotNull Member getMember() {     // EventManager only passes events from within a guild, so we can assert that Member is always non-null here.
        return getEvent().getMember();
    }

    public MessageChannel getChannel() {
        return getEvent().getChannel();
    }

    @SuppressWarnings("ConstantConditions")
    public @NotNull Guild getGuild() {     // EventManager only passes events from within a guild, so we can assert that Guild is always non-null here.
        return getEvent().getGuild();
    }
}
