package net.sentientturtle.discordbot.components.presence;

import net.dv8tion.jda.api.JDA;
import net.sentientturtle.discordbot.components.core.Core;
import net.sentientturtle.discordbot.components.core.FeatureLock;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PresenceManager implements StaticLoaded {
    private static final Map<PresenceImportance, LinkedList<WeakReference<PresenceProvider>>> providers;

    static {
        FeatureLock.lockOrThrow(PresenceManager.class, FeatureLock.PRESENCE);
        providers = Arrays.stream(PresenceImportance.values())
                            .collect(Collectors.toMap(
                                    Function.identity(),    // keys
                                    i -> new LinkedList<>() // values
                            ));

        Scheduling.scheduleAtFixedRate(PresenceManager::update, 10, 10, TimeUnit.SECONDS);

        HealthCheck.addStatic(PresenceManager.class, () -> Core.getJDA() != null ? HealthStatus.RUNNING : HealthStatus.STARTING);
    }

    /**
     * Registers a provider<br>
     * Providers are only weakly referenced to avoid leaks
     * @param provider Provider to register
     * @param importance Importance of the provider
     */
    public static synchronized void registerProvider(@NotNull PresenceProvider provider, @NotNull PresenceImportance importance) {
        providers.get(importance).addLast(new WeakReference<>(provider));
    }

    public static synchronized void removeProvider(@NotNull PresenceProvider provider) {
        for (LinkedList<WeakReference<PresenceProvider>> list : providers.values()) {
            list.removeIf(reference -> reference.get() == provider || reference.get() == null); // Remove targeted provider, and clean up dead references while we're iterating anyway.
        }
    }

    private static synchronized void update() {
        JDA jda = Core.getJDA();
        if (jda != null) {
            for (PresenceImportance importance : PresenceImportance.values()) {
                var list = providers.get(importance);
                for (int i = 0; i < list.size(); i++) {
                    var reference = list.removeFirst();
                    PresenceProvider provider = reference.get();
                    if (provider != null) { // If reference is dead, forget about it and try the next loop
                        var activity = provider.getActivity();
                        if (activity != null) {
                            jda.getPresence().setPresence(
                                    provider.getOnlineStatus(),
                                    activity,
                                    provider.getIdle()
                            );
                            list.addLast(reference);
                            return; // Exit early after first valid update
                        } else {
                            list.addLast(reference);
                        }
                    }
                }
            }
            // If no activity is found
            if (jda.getPresence().getActivity() != null) {
                jda.getPresence().setActivity(null);
            }
        }
    }

}
