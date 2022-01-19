package net.sentientturtle.discordbot.botmodules.convert;

import net.sentientturtle.discordbot.botmodules.convert.units.*;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.core.Shutdown;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.Command;
import net.sentientturtle.discordbot.components.module.command.CommandCall;
import net.sentientturtle.util.NotYetImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Module providing Unit conversion
 */
public class Convert extends BotModule {
    private final Logger logger;
    private final HashMap<String, Unit> unitMap = new HashMap<>();
    private static CurrencyValues currencyValues = new CurrencyValues();

    public Convert() {
        logger = LoggerFactory.getLogger(Convert.class);

        Unit[][] units = {Currency.values(), Area.values(), Energy.values(), Information.values(), Length.values(), Mass.values(), Temperature.values(), Volume.values()};
        Arrays.stream(units)
                .flatMap(Arrays::stream)
                .forEach(unit -> {
                    for (String identifier : unit.identifiers()) {
                        if (unitMap.putIfAbsent(identifier, unit) != null) {
                            logger.error("Duplicate unit identifier: [" + identifier + "] for units [" + unit + "] and [" + unitMap.get(identifier) + "]");
                        }
                    }
                });

        ScheduledFuture<?> updateCurrencyFuture = Scheduling.scheduleAtFixedRate(this::updateCurrencyValues, 0, 24, TimeUnit.HOURS);
        Shutdown.registerHook(() -> updateCurrencyFuture.cancel(true));
        HealthCheck.addInstance(this, () -> {
            if (!updateCurrencyFuture.isDone()) {
                return HealthStatus.RUNNING;
            } else if (updateCurrencyFuture.isCancelled()) {
                return HealthStatus.SHUTTING_DOWN;
            } else {
                return HealthStatus.ERROR_NONCRITICAL;
            }
        }, new Supplier<>() {
            Throwable error = null; // Save any exception raised, as they're only raised once.

            @Override
            public Optional<String> get() {
                if (error != null) {
                    return Optional.ofNullable(error.getMessage());
                } else {
                    if (updateCurrencyFuture.isDone()) {
                        try {
                            updateCurrencyFuture.get(0, TimeUnit.NANOSECONDS);
                        } catch (InterruptedException | TimeoutException e) {
                            error = e;
                            return Optional.ofNullable(e.getMessage());
                        } catch (ExecutionException e) {
                            if (e.getCause() != null) {
                                error = e.getCause();
                                return Optional.ofNullable(e.getCause().getMessage());
                            } else {
                                return Optional.empty();
                            }
                        }
                        return Optional.empty();
                    } else {
                        return Optional.of("Currency exchange rates updated on: " + currencyValues.date);
                    }
                }
            }
        });
        logger.info("Started currency conversion daemon!");
    }

    @Command(description = "Converts between units and currencies")
    public void convert(
            CommandCall commandCall,
            @Command.Parameter(name = "value", description = "Value to convert") String valueString,
            @Command.Parameter(name = "from_unit", description = "Value unit") String fromUnitString,
            @Command.Parameter(name = "to_unit", description = "Target unit") String toUnitString
    ) {
        BigDecimal value;
        try {
            value = new BigDecimal(valueString);
        } catch (NumberFormatException e) {
            commandCall.error("Value is not numerical!");
            return;
        }
        Unit fromUnit = unitMap.get(fromUnitString);
        Unit toUnit = unitMap.get(toUnitString);
        if (fromUnit == null) {
            commandCall.error("⚠ Unknown unit: " + fromUnitString);
        } else if (toUnit == null) {
            commandCall.error("⚠ Unknown unit: " + toUnitString);
        } else if (fromUnit.getClass() != toUnit.getClass()) {
            commandCall.error("⚠ Cannot convert measure " + fromUnit.getClass().getSimpleName() + " to measure " + toUnit.getClass().getSimpleName());
        } else {
            try {
                BigDecimal newValue = fromUnit.convert(value, toUnit);
                value = value.round(new MathContext(5));    // We round for display to deal with floating point errors and conversions that give very large amounts of decimals
                newValue = newValue.round(new MathContext(5));
                String message = value + " "
                                 + fromUnit.name().replace('_', ' ')
                                 + " ≈ "  // Display an approximately-equals-sign, as we rounded the values
                                 + newValue + " "
                                 + toUnit.name().replace('_', ' ');
                if (toUnit instanceof Currency) {
                    message += "\n(Exchange rates: " + currencyValues.date + ")";
                }
                commandCall.reply(message);
            } catch (ArithmeticException ignored) {
                if (toUnit instanceof Currency) {
                    commandCall.error("⚠ Exchange rates are currently unavailable!");
                }
            } catch (Exception e) {
                logger.error("Unit conversion failed!", e);
                commandCall.error("⚠ Unit conversion failed!");
            }
        }
    }

    public static CurrencyValues getCurrencyValues() {
        return currencyValues;
    }

    /**
     * Currently unimplemented after deprecation of the external API used.
     */
    private void updateCurrencyValues() {
        throw new NotYetImplementedException("API no longer available");   // TODO: Reimplement currency conversion rate lookup
    }
}
