package net.sentientturtle.discordbot.components.healthcheck;

/**
 * Health status enum
 */
public enum HealthStatus {
    PAUSED {
        public String asIcon() {
            return "⏸";
        }
    },
    STARTING {
        public String asIcon() {
            return "🔜";
        }
    },
    RUNNING {
        public String asIcon() {
            return "🆙";
        }
    },
    SHUTTING_DOWN {
        public String asIcon() {
            return "🔙";
        }
    },
    RECOVERING {
        public String asIcon() {
            return "🔄";
        }
    },
    STOPPED {
        public String asIcon() {
            return "⏹";
        }
    },
    ERROR_NONCRITICAL {     // Continued (partial) functionality available
        public String asIcon() {
            return "⚠";
        }
    },
    ERROR_CRITICAL {        // Functionality no longer available
        public String asIcon() {
            return "🆘";
        }
    };

    public abstract String asIcon();
}
