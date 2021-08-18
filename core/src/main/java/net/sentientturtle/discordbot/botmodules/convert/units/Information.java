package net.sentientturtle.discordbot.botmodules.convert.units;

import net.sentientturtle.discordbot.botmodules.convert.Convert;
import net.sentientturtle.discordbot.botmodules.convert.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Information implements Unit {
    yottabits(new BigDecimal("1.00E+24"), "Yb", "yottabit"),
    zettabits(new BigDecimal("1.00E+21"), "Zb", "zettabit"),
    exabits(new BigDecimal("1.00E+18"), "Eb", "exabit"),
    petabits(new BigDecimal("1.00E+15"), "Pb", "petabit"),
    terabits(new BigDecimal("1.00E+12"), "Tb", "terabit"),
    gigabits(new BigDecimal("1.00E+09"), "Gb", "gigabit"),
    megabits(new BigDecimal("1.00E+06"), "Mb", "megabit"),
    kilobits(new BigDecimal("1.00E+03"), "kb", "kilobit"),
    bits(new BigDecimal("1.00E+00"), "b", "bit"),
    kibibits(new BigDecimal("1024"), "Kib", "kibibit"),
    mebibits(new BigDecimal("1048576.00"), "Mib", "mebibit"),
    gibibits(new BigDecimal("1073741824.00"), "Gib", "gibibit"),
    tebibits(new BigDecimal("1099511627776.00"), "Tib", "tebibit"),
    pebibits(new BigDecimal("1125899906842620.00"), "Pib", "pebibit"),
    exibits(new BigDecimal("1152921504606850000.00"), "Eib", "exibit"),
    zebibits(new BigDecimal("1180591620717410000000.00"), "Zib", "zebibit"),
    yobibits(new BigDecimal("1208925819614630000000000.00"), "Yib", "yobibit"),

    yottabytes(new BigDecimal("1.00E+24").multiply(new BigDecimal("8")), "YB", "yottabyte"),
    zettabytes(new BigDecimal("1.00E+21").multiply(new BigDecimal("8")), "ZB", "zettabyte"),
    exabytes(new BigDecimal("1.00E+18").multiply(new BigDecimal("8")), "EB", "exabyte"),
    petabytes(new BigDecimal("1.00E+15").multiply(new BigDecimal("8")), "PB", "petabyte"),
    terabytes(new BigDecimal("1.00E+12").multiply(new BigDecimal("8")), "TB", "terabyte"),
    gigabytes(new BigDecimal("1.00E+09").multiply(new BigDecimal("8")), "GB", "gigabyte"),
    megabytes(new BigDecimal("1.00E+06").multiply(new BigDecimal("8")), "MB", "megabyte"),
    kilobytes(new BigDecimal("1.00E+03").multiply(new BigDecimal("8")), "kB", "kilobyte"),
    bytes(new BigDecimal("1.00E+00").multiply(new BigDecimal("8")), "B", "byte"),
    kibibytes(new BigDecimal("1024").multiply(new BigDecimal("8")), "KiB", "kibibyte"),
    mebibytes(new BigDecimal("1048576.00").multiply(new BigDecimal("8")), "MiB", "mebibyte"),
    gibibytes(new BigDecimal("1073741824.00").multiply(new BigDecimal("8")), "GiB", "gibibyte"),
    tebibytes(new BigDecimal("1099511627776.00").multiply(new BigDecimal("8")), "TiB", "tebibyte"),
    pebibytes(new BigDecimal("1125899906842620.00").multiply(new BigDecimal("8")), "PiB", "pebibyte"),
    exibytes(new BigDecimal("1152921504606850000.00").multiply(new BigDecimal("8")), "EiB", "exibyte"),
    zebibytes(new BigDecimal("1180591620717410000000.00").multiply(new BigDecimal("8")), "ZiB", "zebibyte"),
    yobibytes(new BigDecimal("1208925819614630000000000.00").multiply(new BigDecimal("8")), "YiB", "yobibyte");

    private final Function<BigDecimal, BigDecimal> toBase;
    private final Function<BigDecimal, BigDecimal> fromBase;
    private final String[] identifiers;

    Information(BigDecimal multiplier, String... identifiers) {
        this.toBase = val -> val.multiply(multiplier);
        this.fromBase = val -> val.divide(multiplier, 4, RoundingMode.HALF_UP);
        this.identifiers = identifiers;
    }

    @Override
    public String[] identifiers() {
        return identifiers;
    }

    @Override
    public BigDecimal convert(Convert convert, BigDecimal value, Unit to) {
        if (to instanceof Information) {
            return ((Information) to).fromBase.apply(toBase.apply(value));
        } else {
            throw new IllegalArgumentException("to-Unit is not of equal type!");
        }
    }
}
