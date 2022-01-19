package net.sentientturtle.discordbot.botmodules.convert.units;

import net.sentientturtle.discordbot.botmodules.convert.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Temperature implements Unit {
    yottakelvin(new BigDecimal("1.00E+24"), "YK", "yottakelvin"),
    zettakelvin(new BigDecimal("1.00E+21"), "ZK", "zettakelvin"),
    exakelvin(new BigDecimal("1.00E+18"), "EK", "exakelvin"),
    petakelvin(new BigDecimal("1.00E+15"), "PK", "petakelvin"),
    terakelvin(new BigDecimal("1.00E+12"), "TK", "terakelvin"),
    gigakelvin(new BigDecimal("1.00E+09"), "GK", "gigakelvin"),
    megakelvin(new BigDecimal("1.00E+06"), "MK", "megakelvin"),
    kilokelvin(new BigDecimal("1.00E+03"), "kK", "kilokelvin"),
    hectokelvin(new BigDecimal("1.00E+02"), "hK", "hectokelvin"),
    decakelvin(new BigDecimal("1.00E+01"), "daK", "decakelvin"),
    kelvin(new BigDecimal("1.00E+00"), "K", "kelvin"),
    decikelvin(new BigDecimal("1.00E-01"), "dK", "decikelvin"),
    centikelvin(new BigDecimal("1.00E-02"), "cK", "centikelvin"),
    millikelvin(new BigDecimal("1.00E-03"), "mK", "millikelvin"),
    microkelvin(new BigDecimal("1.00E-06"), "μK", "microkelvin"),
    nanokelvin(new BigDecimal("1.00E-09"), "nK", "nanokelvin"),
    picokelvin(new BigDecimal("1.00E-12"), "pK", "picokelvin"),
    femtokelvin(new BigDecimal("1.00E-15"), "fK", "femtokelvin"),
    attokelvin(new BigDecimal("1.00E-18"), "aK", "attokelvin"),
    zeptokelvin(new BigDecimal("1.00E-21"), "zK", "zeptokelvin"),
    yoctokelvin(new BigDecimal("1.00E-24"), "yK", "yoctokelvin"),

    celcius(val -> val.add(new BigDecimal("273.15")), val -> val.subtract(new BigDecimal("273.15")), "C", "Celsius", "celsius", "°C", "degrees Celsius", "degrees celsius"),
    fahrenheit(
            val -> val.subtract(new BigDecimal("32")).multiply(new BigDecimal("5.0").divide(new BigDecimal("9.0"), 4, RoundingMode.HALF_UP)).add(new BigDecimal("273.15")),
            val -> val.subtract(new BigDecimal("273.15")).multiply(new BigDecimal("9.0").divide(new BigDecimal("5.0"), 4, RoundingMode.HALF_UP)).add(new BigDecimal("32")),
            "F", "Fahrenheit", "fahrenheit", "°F", "degrees Fahrenheit", "degrees fahrenheit"
    ),
    rankine(new BigDecimal("9.0").divide(new BigDecimal("5.0"), RoundingMode.HALF_UP), "R", "Rankine", "rankine", "°R", "degrees Rankine", "degrees rankine");


    private final Function<BigDecimal, BigDecimal> toBaseUnit;
    private final Function<BigDecimal, BigDecimal> fromBaseUnit;
    private final String[] identifiers;

    Temperature(Function<BigDecimal, BigDecimal> toBaseUnit, Function<BigDecimal, BigDecimal> fromBaseUnit, String... identifiers) {
        this.toBaseUnit = toBaseUnit;
        this.fromBaseUnit = fromBaseUnit;
        this.identifiers = identifiers;
    }

    Temperature(BigDecimal multiplier, String... identifiers) {
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
        if (toUnit instanceof Temperature) {
            return ((Temperature) toUnit).fromBaseUnit.apply(toBaseUnit.apply(value));
        } else {
            throw new IllegalArgumentException("to-Unit is not of equal type!");
        }
    }
}
