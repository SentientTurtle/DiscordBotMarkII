package net.sentientturtle.discordbot.botmodules.convert.units;

import net.sentientturtle.discordbot.botmodules.convert.Convert;
import net.sentientturtle.discordbot.botmodules.convert.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Length implements Unit {
    yottametres(new BigDecimal("1.00E+24"), "Ym", "yottametre", "yottameter", "yottametres", "yottameters"),
    zettametres(new BigDecimal("1.00E+21"), "Zm", "zettametre", "zettameter", "zettametres", "zettameters"),
    exametres(new BigDecimal("1.00E+18"), "Em", "exametre", "exameter", "exametres", "exameters"),
    petametres(new BigDecimal("1.00E+15"), "Pm", "petametre", "petameter", "petametres", "petameters"),
    terametres(new BigDecimal("1.00E+12"), "Tm", "terametre", "terameter", "terametres", "terameters"),
    gigametres(new BigDecimal("1.00E+09"), "Gm", "gigametre", "gigameter", "gigametres", "gigameters"),
    megametres(new BigDecimal("1.00E+06"), "Mm", "megametre", "megameter", "megametres", "megameters"),
    kilometres(new BigDecimal("1.00E+03"), "km", "kilometre", "kilometer", "kilometres", "kilometers"),
    hectometres(new BigDecimal("1.00E+02"), "hm", "hectometre", "hectometer", "hectometres", "hectometers"),
    decametres(new BigDecimal("1.00E+01"), "dam", "decametre", "decameter", "decametres", "decameters"),
    metres(new BigDecimal("1.00E+00"), "m", "metre", "meter", "metres", "meters"),
    decimetres(new BigDecimal("1.00E-01"), "dm", "decimetre", "decimeter", "decimetres", "decimeters"),
    centimetres(new BigDecimal("1.00E-02"), "cm", "centimetre", "centimeter", "centimetres", "centimeters"),
    millimetres(new BigDecimal("1.00E-03"), "mm", "millimetre", "millimeter", "millimetres", "millimeters"),
    micrometres(new BigDecimal("1.00E-06"), "μm", "micrometre", "micrometer", "micrometres", "micrometers"),
    nanometres(new BigDecimal("1.00E-09"), "nm", "nanometre", "nanometer", "nanometres", "nanometers"),
    picometres(new BigDecimal("1.00E-12"), "pm", "picometre", "picometer", "picometres", "picometers"),
    femtometres(new BigDecimal("1.00E-15"), "fm", "femtometre", "femtometer", "femtometres", "femtometers"),
    attometres(new BigDecimal("1.00E-18"), "am", "attometre", "attometer", "attometres", "attometers"),
    zeptometres(new BigDecimal("1.00E-21"), "zm", "zeptometre", "zeptometer", "zeptometres", "zeptometers"),
    yoctometres(new BigDecimal("1.00E-24"), "ym", "yoctometre", "yoctometer", "yoctometres", "yoctometers"),

    // US-Customary
    points(new BigDecimal("127.0").divide(new BigDecimal("360.0"), RoundingMode.HALF_UP).multiply(millimetres.multiplier), "p", "point", "points"),
    picas(new BigDecimal("127.0").divide(new BigDecimal("30.0"), RoundingMode.HALF_UP).multiply(millimetres.multiplier), "pc", "pica", "picas"),
    inches(new BigDecimal("25.4").multiply(millimetres.multiplier), "in", "\"", "inch", "inches"),
    feet(new BigDecimal("0.3048").multiply(metres.multiplier), "ft", "'", "foot", "feet"),
    yards(new BigDecimal("0.9144").multiply(metres.multiplier), "yd", "yard", "yards"),
    miles(new BigDecimal("1.609344").multiply(kilometres.multiplier), "mi", "mile", "miles"),

    // Nautical
    nautical_miles(new BigDecimal("1852.0").multiply(metres.multiplier), "NM", "nmi", "nautical mile", "nautical miles");


    private final Function<BigDecimal, BigDecimal> toBase;
    private final Function<BigDecimal, BigDecimal> fromBase;
    private final BigDecimal multiplier;
    private final String[] identifiers;

    Length(BigDecimal multiplier, String... identifiers) {
        this.toBase = val -> val.multiply(multiplier);
        this.fromBase = val -> val.divide(multiplier, 4, RoundingMode.HALF_UP);
        this.multiplier = multiplier;
        this.identifiers = identifiers;
    }

    @Override
    public String[] identifiers() {
        return identifiers;
    }

    @Override
    public BigDecimal convert(Convert convert, BigDecimal value, Unit to) {
        if (to instanceof Length) {
            return ((Length) to).fromBase.apply(toBase.apply(value));
        } else {
            throw new IllegalArgumentException("to-Unit is not of equal type!");
        }
    }
}
