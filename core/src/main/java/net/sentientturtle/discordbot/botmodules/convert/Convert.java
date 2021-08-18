package net.sentientturtle.discordbot.botmodules.convert;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.sentientturtle.discordbot.botmodules.convert.units.*;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.core.Shutdown;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand;
import net.sentientturtle.util.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.dv8tion.jda.api.Permission.MESSAGE_WRITE;

/**
 * Module providing Unit & Currency conversion
 */
public class Convert extends BotModule {
    private final Logger logger;
    private final Pattern prefixConversionPattern;
    private final Pattern postfixConversionPattern;
    private final HashMap<String, Unit> unitMap = new HashMap<>();
    private transient CurrencyValues currencyValues = new CurrencyValues();

    public Convert() {
        logger = LoggerFactory.getLogger(Convert.class);
        prefixConversionPattern = Pattern.compile("([^0-9 ]+)(-?\\d+\\.?\\d*) to (\\D+\\d?)");
        postfixConversionPattern = Pattern.compile("(-?\\d+\\.?\\d*) ?([a-zA-Z]+\\d?) to ([a-zA-Z]+\\d?)");

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

        ScheduledFuture<?> scheduledFuture = Scheduling.scheduleAtFixedRate(this::updateCurrencyValues, 0, 24, TimeUnit.HOURS);
        Shutdown.registerHook(() -> scheduledFuture.cancel(true));
        HealthCheck.addInstance(this, () -> {
            if (!scheduledFuture.isDone()) {
                return HealthStatus.RUNNING;
            } else if (scheduledFuture.isCancelled()) {
                return HealthStatus.SHUTTING_DOWN;
            } else {
                return HealthStatus.ERROR_CRITICAL;
            }
        }, new Supplier<>() {
            Throwable error = null; // Save any exception raised, as they're only raised once.
            @Override
            public Optional<String> get() {
                if (error != null) {
                    return Optional.ofNullable(error.getMessage());
                } else {
                    if (scheduledFuture.isDone()) {
                        try {
                            scheduledFuture.get(0, TimeUnit.NANOSECONDS);
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

    @ATextCommand(syntax = "conv <value> to <unit>", helpText = "Converts values to a specified unit", discordPermissions = {MESSAGE_WRITE})
    public void conv(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        Matcher prefixMatcher = prefixConversionPattern.matcher(args);
        Matcher postfixMatcher = postfixConversionPattern.matcher(args);

        String valueString;
        String fromUnitString;
        String toUnitString;
        if (postfixMatcher.matches()) {
            valueString = postfixMatcher.group(1);
            fromUnitString = postfixMatcher.group(2);
            toUnitString = postfixMatcher.group(3);
        } else if (prefixMatcher.matches()) {
            fromUnitString = prefixMatcher.group(1);
            valueString = prefixMatcher.group(2);
            toUnitString = prefixMatcher.group(3);
        } else {
            event.getChannel().sendMessage("⚠ Invalid command format!").queue();
            return;
        }
        BigDecimal value = new BigDecimal(valueString);
        Unit fromUnit = unitMap.get(fromUnitString);
        Unit toUnit = unitMap.get(toUnitString);
        if (fromUnit == null) {
            event.getChannel().sendMessage("⚠ Unknown unit: " + fromUnitString).queue();
        } else if (toUnit == null) {
            event.getChannel().sendMessage("⚠ Unknown unit: " + toUnitString).queue();
        } else if (fromUnit.getClass() != toUnit.getClass()) {
            event.getChannel().sendMessage("⚠ Cannot convert measure " + fromUnit.getClass().getSimpleName() + " to measure " + toUnit.getClass().getSimpleName()).queue();
        } else {
            try {
                BigDecimal newValue = fromUnit.convert(this, value, toUnit);
                value = value.round(new MathContext(5));
                newValue = newValue.round(new MathContext(5));
                String message = value + " "
                                 + fromUnit.name().replace('_', ' ')
                                 + " is ~"
                                 + newValue + " "
                                 + toUnit.name().replace('_', ' ');
                if (toUnit instanceof Currency) {
                    message += "\n(Exchange rates: " + currencyValues.date + ")";
                }
                event.getChannel().sendMessage(message).queue();
            } catch (ArithmeticException ignored) {
                if (toUnit instanceof Currency) {
                    event.getChannel().sendMessage("⚠ Exchange rates are currently unavailable!").queue();
                }
            } catch (Exception e) {
                logger.error("Unit conversion failed!", e);
            }
        }
    }

    public CurrencyValues getCurrencyValues() {
        return currencyValues;
    }

    private void updateCurrencyValues() {
        throw new NotImplementedException("API no longer available");   // TODO: Reimplement currency conversion rate lookup
    }
}
