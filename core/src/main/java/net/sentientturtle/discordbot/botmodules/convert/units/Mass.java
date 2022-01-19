package net.sentientturtle.discordbot.botmodules.convert.units;

import net.sentientturtle.discordbot.botmodules.convert.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Mass implements Unit {
    yottagrams(new BigDecimal("1.00E+24"), "Yg", "yottagram", "yottagramme", "yottagrams", "yottagrammes"),
    zettagrams(new BigDecimal("1.00E+21"), "Zg", "zettagram", "zettagramme", "zettagrams", "zettagrammes"),
    exagrams(new BigDecimal("1.00E+18"), "Eg", "exagram", "exagramme", "exagrams", "exagrammes"),
    petagrams(new BigDecimal("1.00E+15"), "Pg", "petagram", "petagramme", "petagrams", "petagrammes"),
    teragrams(new BigDecimal("1.00E+12"), "Tg", "teragram", "teragramme", "teragrams", "teragrammes"),
    gigagrams(new BigDecimal("1.00E+09"), "Gg", "gigagram", "gigagramme", "gigagrams", "gigagrammes"),
    megagrams(new BigDecimal("1.00E+06"), "Mg", "megagram", "megagramme", "megagrams", "megagrammes"),
    kilograms(new BigDecimal("1.00E+03"), "kg", "kilogram", "kilogramme", "kilograms", "kilogrammes"),
    hectograms(new BigDecimal("1.00E+02"), "hg", "hectogram", "hectogramme", "hectograms", "hectogrammes"),
    decagrams(new BigDecimal("1.00E+01"), "dag", "decagram", "decagramme", "decagrams", "decagrammes"),
    grams(new BigDecimal("1.00E+00"), "g", "gram", "gramme", "grams", "grammes"),
    decigrams(new BigDecimal("1.00E-01"), "dg", "decigram", "decigramme", "decigrams", "decigrammes"),
    centigrams(new BigDecimal("1.00E-02"), "cg", "centigram", "centigramme", "centigrams", "centigrammes"),
    milligrams(new BigDecimal("1.00E-03"), "mg", "milligram", "milligramme", "milligrams", "milligrammes"),
    micrograms(new BigDecimal("1.00E-06"), "μg", "microgram", "microgramme", "micrograms", "microgrammes"),
    nanograms(new BigDecimal("1.00E-09"), "ng", "nanogram", "nanogramme", "nanograms", "nanogrammes"),
    picograms(new BigDecimal("1.00E-12"), "pg", "picogram", "picogramme", "picograms", "picogrammes"),
    femtograms(new BigDecimal("1.00E-15"), "fg", "femtogram", "femtogramme", "femtograms", "femtogrammes"),
    attograms(new BigDecimal("1.00E-18"), "ag", "attogram", "attogramme", "attograms", "attogrammes"),
    zeptograms(new BigDecimal("1.00E-21"), "zg", "zeptogram", "zeptogramme", "zeptograms", "zeptogrammes"),
    yoctograms(new BigDecimal("1.00E-24"), "yg", "yoctogram", "yoctogramme", "yoctograms", "yoctogrammes"),

    //US Customary units
    grains(new BigDecimal("64.79891").multiply(milligrams.multiplier), "gr", "grain", "grains"),
    ounces(new BigDecimal("28.349523125").multiply(grams.multiplier), "oz", "ounce", "ounces"),
    pounds(new BigDecimal("453.59237").multiply(grams.multiplier), "lb", "lbs", "pound", "pounds"),

    // Troy
    troy_ounces(new BigDecimal("31.1034768").multiply(grams.multiplier), "oz t", "troy ounce", "troy ounces"),
    troy_pounds(new BigDecimal("373.2417216").multiply(grams.multiplier), "lb t", "lbs t", "troy pound", "troy pounds");

    private final Function<BigDecimal, BigDecimal> toBaseUnit;
    private final Function<BigDecimal, BigDecimal> fromBaseUnit;
    private final BigDecimal multiplier;
    private final String[] identifiers;

    Mass(BigDecimal multiplier, String... identifiers) {
        this.toBaseUnit = val -> val.multiply(multiplier);
        this.fromBaseUnit = val -> val.divide(multiplier, 4, RoundingMode.HALF_UP);
        this.multiplier = multiplier;
        this.identifiers = identifiers;
    }

    @Override
    public String[] identifiers() {
        return identifiers;
    }

    @Override
    public BigDecimal convert(BigDecimal value, Unit toUnit) {
        if (toUnit instanceof Mass) {
            return ((Mass) toUnit).fromBaseUnit.apply(toBaseUnit.apply(value));
        } else {
            throw new IllegalArgumentException("to-Unit is not of equal type!");
        }
    }
}
