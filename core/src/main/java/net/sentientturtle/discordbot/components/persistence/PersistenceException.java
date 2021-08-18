package net.sentientturtle.discordbot.components.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

public class PersistenceException extends Exception {
    public PersistenceException() {
    }

    public PersistenceException(Throwable t) {
        super(t);
    }

    public abstract static class ObjectLoadFailed extends PersistenceException {
        public ObjectLoadFailed(Throwable t) {
            super(t);
        }

        public static class ParseError extends ObjectLoadFailed {
            public JsonProcessingException e;
            public ParseError(JsonProcessingException e) {
                super(e);
                this.e = e;
            }
        }

        public static class IOError extends ObjectLoadFailed {
            public IOException e;
            public IOError(IOException e) {
                super(e);
                this.e = e;
            }
        }
    }

    public abstract static class ResourceLoadFailed extends PersistenceException {
        public ResourceLoadFailed() {
        }

        public ResourceLoadFailed(Throwable t) {
            super(t);
        }

        public static class UnSanitizedFilename extends ResourceLoadFailed {
            public final String name;

            public UnSanitizedFilename(String name) {
                this.name = name;
            }
        }

        public static class IOError extends ResourceLoadFailed {
            public Exception e;
            public IOError(Exception e) {
                super(e);
                this.e = e;
            }
        }
    }
}
