package net.sentientturtle.discordbot.components;

/**
 * Exception to signify errors in static initialization of classes.
 */
public class StaticInitException extends RuntimeException {
    public StaticInitException() {
    }

    public StaticInitException(String message) {
        super(message);
    }

    public StaticInitException(String message, Throwable cause) {
        super(message, cause);
    }

    public StaticInitException(Throwable cause) {
        super(cause);
    }

    public StaticInitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
