package net.sentientturtle.discordbot.components.presence;

/**
 * Enum for the importance of a Discord Presence.<br>
 * Status messages with the highest importance (first in enum order) will be shown instead of lower importance.
 * If there are multiple statuses with equal importance, their display will be cycled in round-robin fashion.
 */
public enum PresenceImportance {
    INFO,
    FOREGROUND,
    BACKGROUND,
    IDLE
}
