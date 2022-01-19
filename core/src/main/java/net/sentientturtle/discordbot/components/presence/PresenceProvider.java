package net.sentientturtle.discordbot.components.presence;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

/**
 * Interface for objects which may provide a Discord presence.
 */
public interface PresenceProvider {
    Activity getActivity();

    default OnlineStatus getOnlineStatus() {
        return OnlineStatus.ONLINE;
    }

    default boolean getIdle() {
        return false;
    }
}
