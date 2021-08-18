package net.sentientturtle.discordbot.botmodules.convert;

import java.math.BigDecimal;

public interface Unit {
    String name();
    String[] identifiers();
    BigDecimal convert(Convert convert, BigDecimal value, Unit to) throws ArithmeticException;
}
