package net.sentientturtle.util;

/**
 * Exception to signal an implementation was either not yet made or removed pending re-implementation.<br>
 * Differs from {@link UnsupportedOperationException} in that the operation is <i>intended</i>, just not currently available.
 */
public class NotYetImplementedException extends RuntimeException {
    public NotYetImplementedException() {
        super();
    }

    public NotYetImplementedException(String message) {
        super(message);
    }

    public NotYetImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotYetImplementedException(Throwable cause) {
        super(cause);
    }

    protected NotYetImplementedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
