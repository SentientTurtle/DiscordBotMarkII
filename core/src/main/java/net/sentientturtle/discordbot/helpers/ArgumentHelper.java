package net.sentientturtle.discordbot.helpers;

import org.jetbrains.annotations.Nullable;

public class ArgumentHelper {
    public static int parsePage(@Nullable String args, int defaultValue) throws ParseException {
        if (args == null || args.length() == 0) return defaultValue;
        try {
            return Integer.parseInt(args);
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid option, expected page number!");
        }
    }

    public static class ParseException extends Throwable {
        public final String channelMessage;

        public ParseException(String channelMessage) {
            this.channelMessage = channelMessage;
        }
    }
}
