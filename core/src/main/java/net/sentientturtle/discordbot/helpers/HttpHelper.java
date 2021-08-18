package net.sentientturtle.discordbot.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sentientturtle.discordbot.components.core.Shutdown;
import okhttp3.OkHttpClient;

public class HttpHelper {
    public static final OkHttpClient httpClient = new OkHttpClient();
    public static final ObjectMapper jsonMapper = new ObjectMapper();

    static {
        Shutdown.registerHook(() -> httpClient.dispatcher().executorService().shutdown());
    }
}
