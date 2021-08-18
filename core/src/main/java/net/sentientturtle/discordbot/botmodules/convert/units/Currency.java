package net.sentientturtle.discordbot.botmodules.convert.units;

import net.sentientturtle.discordbot.botmodules.convert.Convert;
import net.sentientturtle.discordbot.botmodules.convert.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.BiFunction;

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

    private final BiFunction<Convert, BigDecimal, BigDecimal> toBase;
    private final BiFunction<Convert, BigDecimal, BigDecimal> fromBase;
    private final String[] identifiers;

    Currency(String... identifiers) {
        this.toBase = (convert, value) -> value.divide(new BigDecimal(convert.getCurrencyValues().rates.getOrDefault(this.name(), "0")), 4, RoundingMode.HALF_UP);
        this.fromBase = (convert, value) -> value.multiply(new BigDecimal(convert.getCurrencyValues().rates.getOrDefault(this.name(), "0")));
        this.identifiers = identifiers;
    }

    @Override
    public String[] identifiers() {
        return identifiers;
    }

    @Override
    public BigDecimal convert(Convert convert, BigDecimal value, Unit to) throws ArithmeticException {
        if (to instanceof Currency) {
            return ((Currency) to).fromBase.apply(convert, toBase.apply(convert, value));
        } else {
            throw new IllegalArgumentException("to-Unit is not of equal type!");
        }
    }
}
