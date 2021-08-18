package net.sentientturtle.discordbot.botmodules.convert.units;

import net.sentientturtle.discordbot.botmodules.convert.Convert;
import net.sentientturtle.discordbot.botmodules.convert.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Area implements Unit {
    square_yottametres(new BigDecimal("1.00E+24").multiply(new BigDecimal("1.00E+24")), "Ym2", "Ym²", "square yottametre", "square yottameter", "square yottametres", "square yottameters"),
    square_zettametres(new BigDecimal("1.00E+21").multiply(new BigDecimal("1.00E+21")), "Zm2", "Zm²", "square zettametre", "square zettameter", "square zettametres", "square zettameters"),
    square_exametres(new BigDecimal("1.00E+18").multiply(new BigDecimal("1.00E+18")), "Em2", "Em²", "square exametre", "square exameter", "square exametres", "square exameters"),
    square_petametres(new BigDecimal("1.00E+15").multiply(new BigDecimal("1.00E+15")), "Pm2", "Pm²", "square petametre", "square petameter", "square petametres", "square petameters"),
    square_terametres(new BigDecimal("1.00E+12").multiply(new BigDecimal("1.00E+12")), "Tm2", "Tm²", "square terametre", "square terameter", "square terametres", "square terameters"),
    square_gigametres(new BigDecimal("1.00E+09").multiply(new BigDecimal("1.00E+09")), "Gm2", "Gm²", "square gigametre", "square gigameter", "square gigametres", "square gigameters"),
    square_megametres(new BigDecimal("1.00E+06").multiply(new BigDecimal("1.00E+06")), "Mm2", "Mm²", "square megametre", "square megameter", "square megametres", "square megameters"),
    square_kilometres(new BigDecimal("1.00E+03").multiply(new BigDecimal("1.00E+03")), "km2", "km²", "square kilometre", "square kilometer", "square kilometres", "square kilometers"),
    square_hectometres(new BigDecimal("1.00E+02").multiply(new BigDecimal("1.00E+02")), "hm2", "hm²", "square hectometre", "square hectometer", "square hectometres", "square hectometers"),
    square_decametres(new BigDecimal("1.00E+01").multiply(new BigDecimal("1.00E+01")), "dam2", "dam²", "square decametre", "square decameter", "square decametres", "square decameters"),
    square_metres(new BigDecimal("1.00E+00").multiply(new BigDecimal("1.00E+00")), "m2", "m²", "square metre", "square meter", "square metres", "square meters"),
    square_decimetres(new BigDecimal("1.00E-01").multiply(new BigDecimal("1.00E-01")), "dm2", "dm²", "square decimetre", "square decimeter", "square decimetres", "square decimeters"),
    square_centimetres(new BigDecimal("1.00E-02").multiply(new BigDecimal("1.00E-02")), "cm2", "cm²", "square centimetre", "square centimeter", "square centimetres", "square centimeters"),
    square_millimetres(new BigDecimal("1.00E-03").multiply(new BigDecimal("1.00E-03")), "mm2", "mm²", "square millimetre", "square millimeter", "square millimetres", "square millimeters"),
    square_micrometres(new BigDecimal("1.00E-06").multiply(new BigDecimal("1.00E-06")), "μm2", "μm²", "square micrometre", "square micrometer", "square micrometres", "square micrometers"),
    square_nanometres(new BigDecimal("1.00E-09").multiply(new BigDecimal("1.00E-09")), "nm2", "nm²", "square nanometre", "square nanometer", "square nanometres", "square nanometers"),
    square_picometres(new BigDecimal("1.00E-12").multiply(new BigDecimal("1.00E-12")), "pm2", "pm²", "square picometre", "square picometer", "square picometres", "square picometers"),
    square_femtometres(new BigDecimal("1.00E-15").multiply(new BigDecimal("1.00E-15")), "fm2", "fm²", "square femtometre", "square femtometer", "square femtometres", "square femtometers"),
    square_attometres(new BigDecimal("1.00E-18").multiply(new BigDecimal("1.00E-18")), "am2", "am²", "square attometre", "square attometer", "square attometres", "square attometers"),
    square_zeptometres(new BigDecimal("1.00E-21").multiply(new BigDecimal("1.00E-21")), "zm2", "zm²", "square zeptometre", "square zeptometer", "square zeptometres", "square zeptometers"),
    square_yoctometres(new BigDecimal("1.00E-24").multiply(new BigDecimal("1.00E-24")), "ym2", "ym²", "square yoctometre", "square yoctometer", "square yoctometres", "square yoctometers"),

    // US-Customary
    square_points(new BigDecimal("127.0").divide(new BigDecimal("360.0"), RoundingMode.HALF_UP).multiply(new BigDecimal("127.0").divide(new BigDecimal("360.0"), RoundingMode.HALF_UP)).multiply(square_millimetres.multiplier), "sq p", "p2", "p²", "square point", "square points"),
    square_picas(new BigDecimal("127.0").divide(new BigDecimal("30.0"), RoundingMode.HALF_UP).multiply(new BigDecimal("127.0").divide(new BigDecimal("30.0"), RoundingMode.HALF_UP)).multiply(square_millimetres.multiplier), "sq pc", "pc2", "pc²", "square pica", "square picas"),
    square_inches(new BigDecimal("25.4").multiply(new BigDecimal("25.4")).multiply(square_millimetres.multiplier), "sq in", "square inch", "square inches"),
    square_feet(new BigDecimal("0.3048").multiply(new BigDecimal("0.3048")).multiply(square_metres.multiplier), "sq ft", "square foot", "square feet"),
    square_yards(new BigDecimal("0.9144").multiply(new BigDecimal("0.9144")).multiply(square_metres.multiplier), "sq yd", "square yard", "square yards"),
    square_miles(new BigDecimal("1.609344").multiply(new BigDecimal("1.609344")).multiply(square_kilometres.multiplier), "sq mi", "square mile", "square miles"),

    // Nautical
    square_nautical_miles(new BigDecimal("1852.0").multiply(new BigDecimal("1852.0")).multiply(square_metres.multiplier), "sq NM", "sq nmi", "square nautical mile", "square nautical miles");

    private final Function<BigDecimal, BigDecimal> toBase;
    private final Function<BigDecimal, BigDecimal> fromBase;
    private final BigDecimal multiplier;
    private final String[] identifiers;

    Area(BigDecimal multiplier, String... identifiers) {
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
        if (to instanceof Area) {
            return ((Area) to).fromBase.apply(toBase.apply(value));
        } else {
            throw new IllegalArgumentException("to-Unit is not of equal type!");
        }
    }
}
