package net.sentientturtle.discordbot.botmodules.convert.units;

import net.sentientturtle.discordbot.botmodules.convert.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Energy implements Unit {
    yottajoules(new BigDecimal("1.00E+24"), "YJ", "yottajoule", "yottajoules"),
    zettajoules(new BigDecimal("1.00E+21"), "ZJ", "zettajoule", "zettajoules"),
    exajoules(new BigDecimal("1.00E+18"), "EJ", "exajoule", "exajoules"),
    petajoules(new BigDecimal("1.00E+15"), "PJ", "petajoule", "petajoules"),
    terajoules(new BigDecimal("1.00E+12"), "TJ", "terajoule", "terajoules"),
    gigajoules(new BigDecimal("1.00E+09"), "GJ", "gigajoule", "gigajoules"),
    megajoules(new BigDecimal("1.00E+06"), "MJ", "megajoule", "megajoules"),
    kilojoules(new BigDecimal("1.00E+03"), "kJ", "kilojoule", "kilojoules"),
    hectojoules(new BigDecimal("1.00E+02"), "hJ", "hectojoule", "hectojoules"),
    decajoules(new BigDecimal("1.00E+01"), "daJ", "decajoule", "decajoules"),
    joules(new BigDecimal("1.00E+00"), "J", "joule", "joules"),
    decijoules(new BigDecimal("1.00E-01"), "dJ", "decijoule", "decijoules"),
    centijoules(new BigDecimal("1.00E-02"), "cJ", "centijoule", "centijoules"),
    millijoules(new BigDecimal("1.00E-03"), "mJ", "millijoule", "millijoules"),
    microjoules(new BigDecimal("1.00E-06"), "μJ", "microjoule", "microjoules"),
    nanojoules(new BigDecimal("1.00E-09"), "nJ", "nanojoule", "nanojoules"),
    picojoules(new BigDecimal("1.00E-12"), "pJ", "picojoule", "picojoules"),
    femtojoules(new BigDecimal("1.00E-15"), "fJ", "femtojoule", "femtojoules"),
    attojoules(new BigDecimal("1.00E-18"), "aJ", "attojoule", "attojoules"),
    zeptojoules(new BigDecimal("1.00E-21"), "zJ", "zeptojoule", "zeptojoules"),
    yoctojoules(new BigDecimal("1.00E-24"), "yJ", "yoctojoule", "yoctojoules"),

    yottaWatthours(new BigDecimal("1.00E+24").multiply(new BigDecimal("3600")), "YWh", "yottaWatt-hour", "yottaWatt-hours", "yottaWatthour", "yottaWatthours"),
    zettaWatthours(new BigDecimal("1.00E+21").multiply(new BigDecimal("3600")), "ZWh", "zettaWatt-hour", "zettaWatt-hours", "zettaWatthour", "zettaWatthours"),
    exaWatthours(new BigDecimal("1.00E+18").multiply(new BigDecimal("3600")), "EWh", "exaWatt-hour", "exaWatt-hours", "exaWatthour", "exaWatthours"),
    petaWatthours(new BigDecimal("1.00E+15").multiply(new BigDecimal("3600")), "PWh", "petaWatt-hour", "petaWatt-hours", "petaWatthour", "petaWatthours"),
    teraWatthours(new BigDecimal("1.00E+12").multiply(new BigDecimal("3600")), "TWh", "teraWatt-hour", "teraWatt-hours", "teraWatthour", "teraWatthours"),
    gigaWatthours(new BigDecimal("1.00E+09").multiply(new BigDecimal("3600")), "GWh", "gigaWatt-hour", "gigaWatt-hours", "gigaWatthour", "gigaWatthours"),
    megaWatthours(new BigDecimal("1.00E+06").multiply(new BigDecimal("3600")), "MWh", "megaWatt-hour", "megaWatt-hours", "megaWatthour", "megaWatthours"),
    kiloWatthours(new BigDecimal("1.00E+03").multiply(new BigDecimal("3600")), "kWh", "kiloWatt-hour", "kiloWatt-hours", "kiloWatthour", "kiloWatthours"),
    hectoWatthours(new BigDecimal("1.00E+02").multiply(new BigDecimal("3600")), "hWh", "hectoWatt-hour", "hectoWatt-hours", "hectoWatthour", "hectoWatthours"),
    decaWatthours(new BigDecimal("1.00E+01").multiply(new BigDecimal("3600")), "daWh", "decaWatt-hour", "decaWatt-hours", "decaWatthour", "decaWatthours"),
    Watthours(new BigDecimal("1.00E+00").multiply(new BigDecimal("3600")), "Wh", "Watt-hour", "Watt-hours", "Watthour", "Watthours"),
    deciWatthours(new BigDecimal("1.00E-01").multiply(new BigDecimal("3600")), "dWh", "deciWatt-hour", "deciWatt-hours", "deciWatthour", "deciWatthours"),
    centiWatthours(new BigDecimal("1.00E-02").multiply(new BigDecimal("3600")), "cWh", "centiWatt-hour", "centiWatt-hours", "centiWatthour", "centiWatthours"),
    milliWatthours(new BigDecimal("1.00E-03").multiply(new BigDecimal("3600")), "mWh", "milliWatt-hour", "milliWatt-hours", "milliWatthour", "milliWatthours"),
    microWatthours(new BigDecimal("1.00E-06").multiply(new BigDecimal("3600")), "μWh", "microWatt-hour", "microWatt-hours", "microWatthour", "microWatthours"),
    nanoWatthours(new BigDecimal("1.00E-09").multiply(new BigDecimal("3600")), "nWh", "nanoWatt-hour", "nanoWatt-hours", "nanoWatthour", "nanoWatthours"),
    picoWatthours(new BigDecimal("1.00E-12").multiply(new BigDecimal("3600")), "pWh", "picoWatt-hour", "picoWatt-hours", "picoWatthour", "picoWatthours"),
    femtoWatthours(new BigDecimal("1.00E-15").multiply(new BigDecimal("3600")), "fWh", "femtoWatt-hour", "femtoWatt-hours", "femtoWatthour", "femtoWatthours"),
    attoWatthours(new BigDecimal("1.00E-18").multiply(new BigDecimal("3600")), "aWh", "attoWatt-hour", "attoWatt-hours", "attoWatthour", "attoWatthours"),
    zeptoWatthours(new BigDecimal("1.00E-21").multiply(new BigDecimal("3600")), "zWh", "zeptoWatt-hour", "zeptoWatt-hours", "zeptoWatthour", "zeptoWatthours"),
    yoctoWatthours(new BigDecimal("1.00E-24").multiply(new BigDecimal("3600")), "yWh", "yoctoWatt-hour", "yoctoWatt-hours", "yoctoWatthour", "yoctoWatthours");

    private final Function<BigDecimal, BigDecimal> toBaseUnit;
    private final Function<BigDecimal, BigDecimal> fromBaseUnit;
    private final String[] identifiers;

    Energy(BigDecimal multiplier, String... identifiers) {
        this.toBaseUnit = val -> val.multiply(multiplier);
        this.fromBaseUnit = val -> val.divide(multiplier, 4, RoundingMode.HALF_UP);
        this.identifiers = identifiers;
    }

    @Override
    public String[] identifiers() {
        return identifiers;
    }

    @Override
    public BigDecimal convert(BigDecimal value, Unit toUnit) {
        if (toUnit instanceof Energy) {
            return ((Energy) toUnit).fromBaseUnit.apply(toBaseUnit.apply(value));
        } else {
            throw new IllegalArgumentException("to-Unit is not of equal type!");
        }
    }
}
