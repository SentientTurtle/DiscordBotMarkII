package net.sentientturtle.discordbot.botmodules.convert;

import java.math.BigDecimal;

/**
 * Interface for units of measurement
 */
public interface Unit {
    /**
     * @return Full name of this unit, e.g. 'metre'
     */
    String name();

    /**
     * @return Array of identifiers and alternative names for this unit, e.g. 'meter', 'm', 'meters', 'metres'
     */
    String[] identifiers();

    /**
     * @param value Amount of this unit to convert
     * @param toUnit Unit to convert the specified value to
     * @return Equivalent amount of {@code toUnit} for the specified value
     */
    BigDecimal convert(BigDecimal value, Unit toUnit);
}
