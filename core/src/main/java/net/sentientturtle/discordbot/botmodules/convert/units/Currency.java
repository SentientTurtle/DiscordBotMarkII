package net.sentientturtle.discordbot.botmodules.convert.units;

import net.sentientturtle.discordbot.botmodules.convert.Convert;
import net.sentientturtle.discordbot.botmodules.convert.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Currency implements Unit {
    EUR("€", "EUR", "euro"),
    USD("$", "USD", "US dollar", "dollar"),
    JPY("¥", "JPY", "yen"),
    BGN("лв", "BGN", "Bulgarian lev", "lev"),
    CZK("Kč", "CZK", "Czech koruna", "koruna"),
    DKK("DKK", "Danish krone"),
    GBP("£", "GBP", "Pound sterling"),
    HUF("Ft", "HUF", "Hungarian forint", "forint"),
    PLN("zł", "PLN", "Polish złoty", "złoty"),
    RON("lei", "RON", "Romanian leu", "leu"),
    SEK("SEK", "Swedish krona"),
    CHF("CHf", "Fr.", "SFr.", "CHF", "Swiss franc", "franc"),
    ISK("ISK", "Icelandic krona"),
    NOK("NOK", "Norwegian krone"),
    HRK("kn", "HRK", "Croatian kuna", "kuna"),
    RUB("₽", "RUB", "Russian ruble", "ruble"),
    TRY("₺", "TRY", "Turkish lira", "lira"),
    AUD("A$", "AU$", "AUD", "Australian dollar"),
    BRL("R$", "BRL", "Brazilian real", "real"),
    CAD("C$", "CA$", "CAD", "Canadian dollar"),
    CNY("元", "CNY", "RMB", "Renminbi", "yuan"),
    HKD("HK$", "HKD", "Hong Kong dollar"),
    IDR("Rp", "IDR", "Indonesian rupiah", "rupiah"),
    ILS("₪", "ILS", "Israeli shekel", "shekel"),
    INR("₹", "INR", "Indonesian rupee", "rupee"),
    KRW("₩", "KRW", "South Korean won", "won"),
    MXN("Mex$", "MXN", "Mexican peso", "peso"),
    MYR("RM", "MYR", "Malaysian ringgit", "ringgit"),
    NZD("NZ$", "NZD", "New Zealand dollar"),
    PHP("₱", "PHP", "Philippine peso"),
    SGD("S$", "SGD", "Singapore dollar"),
    THB("฿", "THB", "Thai baht", "baht"),
    ZAR("ZAR", "South African rand", "rand");

    private final Function<BigDecimal, BigDecimal> toBaseUnit;
    private final Function<BigDecimal, BigDecimal> fromBaseUnit;
    private final String[] identifiers;

    Currency(String... identifiers) {
        this.toBaseUnit = value -> value.divide(new BigDecimal(Convert.getCurrencyValues().rates.getOrDefault(this.name(), "0")), 4, RoundingMode.HALF_UP);
        this.fromBaseUnit = value -> value.multiply(new BigDecimal(Convert.getCurrencyValues().rates.getOrDefault(this.name(), "0")));
        this.identifiers = identifiers;
    }

    @Override
    public String[] identifiers() {
        return identifiers;
    }

    @Override
    public BigDecimal convert(BigDecimal value, Unit toUnit) throws ArithmeticException {
        if (toUnit instanceof Currency) {
            return ((Currency) toUnit).fromBaseUnit.apply(toBaseUnit.apply(value));
        } else {
            throw new IllegalArgumentException("to-Unit is not of equal type!");
        }
    }
}
