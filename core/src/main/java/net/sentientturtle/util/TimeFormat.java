package net.sentientturtle.util;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for consistent time formatting
 */
public class TimeFormat {
    public static String formatWDHMS(long time, TimeUnit unit) {
        long seconds = unit.toSeconds(time) % 60;
        long minutes = unit.toMinutes(time) % 60;
        long hours = unit.toHours(time) % 24;
        long days = unit.toDays(time) % 7;
        long weeks = unit.toDays(time) / 7;

        if (weeks > 0) {
            return weeks + (weeks == 1 ? "week " : "weeks ") + days + (days == 1 ? "day " : "days ") + hours + "h " + minutes + "m " + seconds + "s";
        } else if (days > 0) {
            return days + (days == 1 ? "day " : "days ") + hours + "h " + minutes + "m " + seconds + "s";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }

    public static String formatHMS(long time, TimeUnit timeUnit) {
        return timeUnit.toHours(time) + ":" + String.format("%02d", timeUnit.toMinutes(time) % 60) + ":" + String.format("%02d", timeUnit.toSeconds(time) % 60);
    }
}
