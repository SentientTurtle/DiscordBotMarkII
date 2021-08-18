package net.sentientturtle.discordbot.components.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.AccountManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.components.module.ModuleManager;
import net.sentientturtle.discordbot.components.persistence.Persistence;
import net.sentientturtle.discordbot.components.persistence.PersistenceException;
import net.sentientturtle.discordbot.components.persistence.PersistentObject;
import net.sentientturtle.discordbot.loader.Loader;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Optional;

/**
 * Main class of bot; Stitches together various components
 */
public class Core implements StaticLoaded {
    private static final Logger logger = LoggerFactory.getLogger(Core.class);
    private static JDA jda;
    private static boolean isInitialised = false;
    private static final CoreSettings settings;
    static {
        try {
            settings = Persistence.loadObject(CoreSettings.class, CoreSettings::new);
        } catch (PersistenceException e) {
            throw new StaticInitException(e);
        }
    }

    static {
        HealthCheck.addStatic(
                Core.class,
                () -> {
                    if (!isInitialised) return HealthStatus.UNINITIALISED;
                    return switch (jda.getStatus()) {
                        case INITIALIZING, INITIALIZED, LOGGING_IN, CONNECTING_TO_WEBSOCKET, IDENTIFYING_SESSION, AWAITING_LOGIN_CONFIRMATION, LOADING_SUBSYSTEMS -> HealthStatus.INITIALISING;
                        case CONNECTED -> HealthStatus.RUNNING;
                        case DISCONNECTED, RECONNECT_QUEUED, WAITING_TO_RECONNECT, ATTEMPTING_TO_RECONNECT -> HealthStatus.RECOVERING;
                        case SHUTTING_DOWN -> HealthStatus.SHUTTING_DOWN;
                        case SHUTDOWN -> HealthStatus.STOPPED;
                        case FAILED_TO_LOGIN -> HealthStatus.ERROR_CRITICAL;
                    };
                },
                () -> Optional.of(jda.getStatus().name())
        );
    }

    public static void init() throws PersistenceException, InterruptedException, LoginException {
        if (isInitialised) return;
        try {
            EventManager eventManager = new EventManager(settings.targetGuild);

            Loader.loadStatic(Core.class.getClassLoader());

            jda = JDABuilder
                          .create(
                                  GatewayIntent.GUILD_EMOJIS,
                                  GatewayIntent.GUILD_VOICE_STATES,
                                  GatewayIntent.GUILD_MESSAGES,
                                  GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                                  GatewayIntent.DIRECT_MESSAGES,
                                  GatewayIntent.DIRECT_MESSAGE_REACTIONS
                          )
                          .setChunkingFilter(ChunkingFilter.NONE)
                          .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
                          .setToken(settings.token)
                          .setAutoReconnect(true)
                          .setEventManager(eventManager)
                          .setIdle(false)
                          .setStatus(OnlineStatus.ONLINE)
                          .build();

            jda.awaitReady();

            AccountManager manager = jda.getSelfUser().getManager();
            if (!manager.getSelfUser().getName().equals(settings.username)) {
                manager.setName(settings.username).complete();
            }

            for (Guild guild : jda.getGuilds()) {
                if (guild.getIdLong() != settings.targetGuild) {
                    logger.error("Bot is in non-target guild: " + guild.getName());
                } else {
                    logger.info("Bot is connected to target guild: " + guild.getName());

                    // This has the intentional side effect of statically initializing ModuleManager
                    ModuleManager.preloadCaches(
                            guild.getTextChannels()
                                    .stream()
                                    .mapToLong(TextChannel::getIdLong)
                    );
                }
            }

            Shutdown.registerHook(jda::shutdown);
            isInitialised = true;
            logger.info("Module initialised!");
            System.out.println("Bot initialised!");
        } catch (Exception e) {
            logger.error("Unable to initialize core!", e);
            Shutdown.shutdownAll(true, -1);
            throw e;
        }
    }

    public static @Nullable JDA getJDA() {
        return jda;
    }

    public static long targetGuildId() {
        return settings.targetGuild;
    }

    private static class CoreSettings implements PersistentObject {
        public String token = "[NO TOKEN]";
        public String username = "Generic Bot Username";
        public Long targetGuild = (long) -1;
    }
}
