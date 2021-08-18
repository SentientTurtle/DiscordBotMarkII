package net.sentientturtle.discordbot.components.healthcheck;

public enum HealthStatus {
    READY {              // Ready; Uninitialized but not requiring initialization
        public String asIcon() {
            return "✅";
        }
    },
    UNINITIALISED {
        public String asIcon() {
            return "⏸";
        }
    },
    INITIALISING {
        public String asIcon() {
            return "⏩";
        }
    },
    RUNNING {
        public String asIcon() {
            return "▶";
        }
    },
    RECOVERING {
        public String asIcon() {
            return "🔄";
        }
    },
    SHUTTING_DOWN {
        public String asIcon() {
            return "⏪";
        }
    },
    STOPPED {
        public String asIcon() {
            return "⏹";
        }
    },
    ERROR_NONCRITICAL {
        public String asIcon() {
            return "⚠";
        }
    },
    ERROR_CRITICAL {
        public String asIcon() {
            return "🆘";
        }
    };

    public abstract String asIcon();
}
