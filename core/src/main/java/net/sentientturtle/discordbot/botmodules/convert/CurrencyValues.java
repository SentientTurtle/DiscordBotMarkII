package net.sentientturtle.discordbot.botmodules.convert;

import java.util.HashMap;

/**
 * Data class to hold currency exchange rate information
 */
public class CurrencyValues {
    public HashMap<String, String> rates = new HashMap<>(); // Key=Currency code, Value=exchange rate. Stored as String for use with BigDecimal and automatic api response -> object parsing
    public String base = "EUR";
    public String date = "01-01-1970";
}
