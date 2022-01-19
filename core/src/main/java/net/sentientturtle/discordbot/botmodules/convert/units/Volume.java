package net.sentientturtle.discordbot.botmodules.convert.units;

import net.sentientturtle.discordbot.botmodules.convert.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Volume implements Unit {
    yottalitres(new BigDecimal("1.00E+24"), "Yl", "YL", "yottalitre", "yottaliter", "yottalitres", "yottaliters"),
    zettalitres(new BigDecimal("1.00E+21"), "Zl", "ZL", "zettalitre", "zettaliter", "zettalitres", "zettaliters"),
    exalitres(new BigDecimal("1.00E+18"), "El", "EL", "exalitre", "exaliter", "exalitres", "exaliters"),
    petalitres(new BigDecimal("1.00E+15"), "Pl", "PL", "petalitre", "petaliter", "petalitres", "petaliters"),
    teralitres(new BigDecimal("1.00E+12"), "Tl", "TL", "teralitre", "teraliter", "teralitres", "teraliters"),
    gigalitres(new BigDecimal("1.00E+09"), "Gl", "GL", "gigalitre", "gigaliter", "gigalitres", "gigaliters"),
    megalitres(new BigDecimal("1.00E+06"), "Ml", "ML", "megalitre", "megaliter", "megalitres", "megaliters"),
    kilolitres(new BigDecimal("1.00E+03"), "kl", "kL", "kilolitre", "kiloliter", "kilolitres", "kiloliters"),
    hectolitres(new BigDecimal("1.00E+02"), "hl", "hL", "hectolitre", "hectoliter", "hectolitres", "hectoliters"),
    decalitres(new BigDecimal("1.00E+01"), "dal", "daL", "decalitre", "decaliter", "decalitres", "decaliters"),
    litres(new BigDecimal("1.00E+00"), "l", "L", "litre", "liter", "litres", "liters"),
    decilitres(new BigDecimal("1.00E-01"), "dl", "dL", "decilitre", "deciliter", "decilitres", "deciliters"),
    centilitres(new BigDecimal("1.00E-02"), "cl", "cL", "centilitre", "centiliter", "centilitres", "centiliters"),
    millilitres(new BigDecimal("1.00E-03"), "ml", "mL", "millilitre", "milliliter", "millilitres", "milliliters"),
    microlitres(new BigDecimal("1.00E-06"), "μl", "μL", "microlitre", "microliter", "microlitres", "microliters"),
    nanolitres(new BigDecimal("1.00E-09"), "nl", "nL", "nanolitre", "nanoliter", "nanolitres", "nanoliters"),
    picolitres(new BigDecimal("1.00E-12"), "pl", "pL", "picolitre", "picoliter", "picolitres", "picoliters"),
    femtolitres(new BigDecimal("1.00E-15"), "fl", "fL", "femtolitre", "femtoliter", "femtolitres", "femtoliters"),
    attolitres(new BigDecimal("1.00E-18"), "al", "aL", "attolitre", "attoliter", "attolitres", "attoliters"),
    zeptolitres(new BigDecimal("1.00E-21"), "zl", "zL", "zeptolitre", "zeptoliter", "zeptolitres", "zeptoliters"),
    yoctolitres(new BigDecimal("1.00E-24"), "yl", "yL", "yoctolitre", "yoctoliter", "yoctolitres", "yoctoliters"),

    // US customary
    teaspoons(new BigDecimal("4.92892159375").multiply(millilitres.multiplier), "tsp", "teaspoon", "teaspoons"),
    tablespoons(new BigDecimal("14.78676478125").multiply(millilitres.multiplier), "tbsp", "Tbsp", "tablespoon", "tablespoons"),
    fluid_ounces(new BigDecimal("29.5735295625").multiply(millilitres.multiplier), "fl oz", "fluid ounce", "fluid ounces"),
    cups(new BigDecimal("236.5882365").multiply(millilitres.multiplier), "cp", "cup", "cups"),
    pints(new BigDecimal("473.176473").multiply(millilitres.multiplier), "pt", "pint", "pints"),
    quarts(new BigDecimal("0.946352946").multiply(litres.multiplier), "qt", "quart", "quarts"),
    gallons(new BigDecimal("3.785411784").multiply(litres.multiplier), "gal", "gallon", "gallons"),
    barrels(new BigDecimal("119.240471196").multiply(litres.multiplier), "bbl", "barrel", "barrels"),

    cubic_yottametres(new BigDecimal("1.00E+24").multiply(new BigDecimal("1.00E+24")).multiply(new BigDecimal("1.00E+24")).multiply(kilolitres.multiplier), "Ym3", "Ym³", "cubic yottametre", "cubic yottameter", "cubic yottametres", "cubic yottameters"),
    cubic_zettametres(new BigDecimal("1.00E+21").multiply(new BigDecimal("1.00E+21")).multiply(new BigDecimal("1.00E+21")).multiply(kilolitres.multiplier), "Zm3", "Zm³", "cubic zettametre", "cubic zettameter", "cubic zettametres", "cubic zettameters"),
    cubic_exametres(new BigDecimal("1.00E+18").multiply(new BigDecimal("1.00E+18")).multiply(new BigDecimal("1.00E+18")).multiply(kilolitres.multiplier), "Em3", "Em³", "cubic exametre", "cubic exameter", "cubic exametres", "cubic exameters"),
    cubic_petametres(new BigDecimal("1.00E+15").multiply(new BigDecimal("1.00E+15")).multiply(new BigDecimal("1.00E+15")).multiply(kilolitres.multiplier), "Pm3", "Pm³", "cubic petametre", "cubic petameter", "cubic petametres", "cubic petameters"),
    cubic_terametres(new BigDecimal("1.00E+12").multiply(new BigDecimal("1.00E+12")).multiply(new BigDecimal("1.00E+12")).multiply(kilolitres.multiplier), "Tm3", "Tm³", "cubic terametre", "cubic terameter", "cubic terametres", "cubic terameters"),
    cubic_gigametres(new BigDecimal("1.00E+09").multiply(new BigDecimal("1.00E+09")).multiply(new BigDecimal("1.00E+09")).multiply(kilolitres.multiplier), "Gm3", "Gm³", "cubic gigametre", "cubic gigameter", "cubic gigametres", "cubic gigameters"),
    cubic_megametres(new BigDecimal("1.00E+06").multiply(new BigDecimal("1.00E+06")).multiply(new BigDecimal("1.00E+06")).multiply(kilolitres.multiplier), "Mm3", "Mm³", "cubic megametre", "cubic megameter", "cubic megametres", "cubic megameters"),
    cubic_kilometres(new BigDecimal("1.00E+03").multiply(new BigDecimal("1.00E+03")).multiply(new BigDecimal("1.00E+03")).multiply(kilolitres.multiplier), "km3", "km³", "cubic kilometre", "cubic kilometer", "cubic kilometres", "cubic kilometers"),
    cubic_hectometres(new BigDecimal("1.00E+02").multiply(new BigDecimal("1.00E+02")).multiply(new BigDecimal("1.00E+02")).multiply(kilolitres.multiplier), "hm3", "hm³", "cubic hectometre", "cubic hectometer", "cubic hectometres", "cubic hectometers"),
    cubic_decametres(new BigDecimal("1.00E+01").multiply(new BigDecimal("1.00E+01")).multiply(new BigDecimal("1.00E+01")).multiply(kilolitres.multiplier), "dam3", "dam³", "cubic decametre", "cubic decameter", "cubic decametres", "cubic decameters"),
    cubic_metres(new BigDecimal("1.00E+00").multiply(new BigDecimal("1.00E+00")).multiply(new BigDecimal("1.00E+00")).multiply(kilolitres.multiplier), "m3", "m³", "cubic metre", "cubic meter", "cubic metres", "cubic meters"),
    cubic_decimetres(new BigDecimal("1.00E-01").multiply(new BigDecimal("1.00E-01")).multiply(new BigDecimal("1.00E-01")).multiply(kilolitres.multiplier), "dm3", "dm³", "cubic decimetre", "cubic decimeter", "cubic decimetres", "cubic decimeters"),
    cubic_centimetres(new BigDecimal("1.00E-02").multiply(new BigDecimal("1.00E-02")).multiply(new BigDecimal("1.00E-02")).multiply(kilolitres.multiplier), "cm3", "cm³", "cubic centimetre", "cubic centimeter", "cubic centimetres", "cubic centimeters"),
    cubic_millimetres(new BigDecimal("1.00E-03").multiply(new BigDecimal("1.00E-03")).multiply(new BigDecimal("1.00E-03")).multiply(kilolitres.multiplier), "mm3", "mm³", "cubic millimetre", "cubic millimeter", "cubic millimetres", "cubic millimeters"),
    cubic_micrometres(new BigDecimal("1.00E-06").multiply(new BigDecimal("1.00E-06")).multiply(new BigDecimal("1.00E-06")).multiply(kilolitres.multiplier), "μm3", "μm³", "cubic micrometre", "cubic micrometer", "cubic micrometres", "cubic micrometers"),
    cubic_nanometres(new BigDecimal("1.00E-09").multiply(new BigDecimal("1.00E-09")).multiply(new BigDecimal("1.00E-09")).multiply(kilolitres.multiplier), "nm3", "nm³", "cubic nanometre", "cubic nanometer", "cubic nanometres", "cubic nanometers"),
    cubic_picometres(new BigDecimal("1.00E-12").multiply(new BigDecimal("1.00E-12")).multiply(new BigDecimal("1.00E-12")).multiply(kilolitres.multiplier), "pm3", "pm³", "cubic picometre", "cubic picometer", "cubic picometres", "cubic picometers"),
    cubic_femtometres(new BigDecimal("1.00E-15").multiply(new BigDecimal("1.00E-15")).multiply(new BigDecimal("1.00E-15")).multiply(kilolitres.multiplier), "fm3", "fm³", "cubic femtometre", "cubic femtometer", "cubic femtometres", "cubic femtometers"),
    cubic_attometres(new BigDecimal("1.00E-18").multiply(new BigDecimal("1.00E-18")).multiply(new BigDecimal("1.00E-18")).multiply(kilolitres.multiplier), "am3", "am³", "cubic attometre", "cubic attometer", "cubic attometres", "cubic attometers"),
    cubic_zeptometres(new BigDecimal("1.00E-21").multiply(new BigDecimal("1.00E-21")).multiply(new BigDecimal("1.00E-21")).multiply(kilolitres.multiplier), "zm3", "zm³", "cubic zeptometre", "cubic zeptometer", "cubic zeptometres", "cubic zeptometers"),
    cubic_yoctometres(new BigDecimal("1.00E-24").multiply(new BigDecimal("1.00E-24")).multiply(new BigDecimal("1.00E-24")).multiply(kilolitres.multiplier), "ym3", "ym³", "cubic yoctometre", "cubic yoctometer", "cubic yoctometres", "cubic yoctometers"),

    // US-Customary
    cubic_points((new BigDecimal("127.0").divide(new BigDecimal("360.0"), RoundingMode.HALF_UP)).multiply((new BigDecimal("127.0").divide(new BigDecimal("360.0"), RoundingMode.HALF_UP)).multiply((new BigDecimal("127.0").divide(new BigDecimal("360.0"), RoundingMode.HALF_UP)).multiply(cubic_millimetres.multiplier))), "cu p", "p3", "p³", "cubic point", "cubic points"),
    cubic_picas((new BigDecimal("127.0").divide(new BigDecimal("30.0"), RoundingMode.HALF_UP)).multiply((new BigDecimal("127.0").divide(new BigDecimal("30.0"), RoundingMode.HALF_UP)).multiply((new BigDecimal("127.0").divide(new BigDecimal("30.0"), RoundingMode.HALF_UP)).multiply(cubic_millimetres.multiplier))), "cu pc", "pc3", "pc³", "cubic pica", "cubic picas"),
    cubic_inches(new BigDecimal("25.4").multiply(new BigDecimal("25.4")).multiply(new BigDecimal("25.4")).multiply(cubic_millimetres.multiplier), "cu in", "cubic inch", "cubic inches"),
    cubic_feet(new BigDecimal("0.3048").multiply(new BigDecimal("0.3048")).multiply(new BigDecimal("0.3048")).multiply(cubic_metres.multiplier), "cu ft", "cubic foot", "cubic feet"),
    cubic_yards(new BigDecimal("0.9144").multiply(new BigDecimal("0.9144")).multiply(new BigDecimal("0.9144")).multiply(cubic_metres.multiplier), "cu yd", "cubic yard", "cubic yards"),
    cubic_miles(new BigDecimal("1.609344").multiply(new BigDecimal("1.609344")).multiply(new BigDecimal("1.609344")).multiply(cubic_kilometres.multiplier), "cu mi", "cubic mile", "cubic miles"),

    // Nautical
    cubic_nautical_miles(new BigDecimal("1852.0").multiply(new BigDecimal("1852.0")).multiply(new BigDecimal("1852.0")).multiply(cubic_metres.multiplier), "cu NM", "cu nmi", "cubic nautical mile", "cubic nautical miles");

    private final Function<BigDecimal, BigDecimal> toBaseUnit;
    private final Function<BigDecimal, BigDecimal> fromBaseUnit;
    private final BigDecimal multiplier;
    private final String[] identifiers;

    Volume(BigDecimal multiplier, String... identifiers) {
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
        if (toUnit instanceof Volume) {
            return ((Volume) toUnit).fromBaseUnit.apply(toBaseUnit.apply(value));
        } else {
            throw new IllegalArgumentException("to-Unit is not of equal type!");
        }
    }
}
