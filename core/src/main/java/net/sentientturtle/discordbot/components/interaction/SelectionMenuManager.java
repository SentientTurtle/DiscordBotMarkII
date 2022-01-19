package net.sentientturtle.discordbot.components.interaction;

import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.permission.BotPermission;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Manager for Discord selection menus<br>
 * Convenience class to automatically bind selection events to callbacks<br>
 * Also eventhandler for selection events
 */
public class SelectionMenuManager implements StaticLoaded {
    private static final Logger logger = LoggerFactory.getLogger(SelectionMenuManager.class);
    private static final AtomicLong selectID = new AtomicLong(System.currentTimeMillis());     // Initialise using calendar time; This provides a best-effort attempt to ensure buttonIDs are not reused even if the program is restarted.

    private static final ConcurrentHashMap<String, SelectHandler> selectHandlers = new ConcurrentHashMap<>();
    private record SelectHandler(Consumer<SelectionMenuEvent> eventHandler, BooleanSupplier isStale, BotPermission selectPermission){}

    static {
        // TODO: Replace this hack with a better way of avoiding memory leaks (Endless accumulation of handlers)
        Scheduling.scheduleAtFixedRate(() -> selectHandlers.values().removeIf(selectHandler -> selectHandler.isStale.getAsBoolean()), 10,10, TimeUnit.MINUTES);
    }

    public static void handleEvent(SelectionMenuEvent selectionMenuEvent) {
        var selectionMenu = selectionMenuEvent.getComponent();
        if (selectionMenu != null) {
            var id = selectionMenu.getId();
            if (id != null) {
                SelectionMenuManager.SelectHandler onSelect = selectHandlers.get(id);
                if (
                        onSelect != null &&
                        selectionMenuEvent.getMember() != null &&
                        onSelect.selectPermission.memberHasPermission(selectionMenuEvent.getMember())
                ) {
                    try {
                        onSelect.eventHandler.accept(selectionMenuEvent);
                    } catch (Throwable t) {
                        logger.warn("Exception in selectMenu handler: " + onSelect.eventHandler, t);
                    }
                }
            }
        }
        if (!selectionMenuEvent.isAcknowledged()) {
            selectionMenuEvent.deferEdit().queue();
        }
    }

    public static SelectionMenu newSelect(String placeholder, Consumer<SelectionMenuEvent> eventHandler, BooleanSupplier isStale, BotPermission selectPermission, String... options) {
        return newSelect(placeholder, eventHandler, isStale, 0, Integer.MAX_VALUE, selectPermission, options);
    }

    public static SelectionMenu newSelect(String placeholder, Consumer<SelectionMenuEvent> eventHandler, BooleanSupplier isStale, int minimumSelectedOptions, int maximumSelectedOptions, BotPermission selectPermission, String... options) {
        var id = "Select-" + selectID.getAndIncrement();

        if (options.length < 1) throw new IllegalArgumentException("Too few options!");
        if (options.length > 25) throw new IllegalArgumentException("Too many options!");
        var builder = SelectionMenu.create(id);
        builder.setPlaceholder(placeholder);
        for (int i = 0; i < options.length; i++) {
            if (options[i].length() > 25) throw new IllegalArgumentException("Option #" + i + " too long! (Length must be <= 25)");
            builder.addOption(options[i], String.valueOf(i));
        }
        builder.setRequiredRange(Math.max(1, minimumSelectedOptions), Math.min(options.length, maximumSelectedOptions));
        var selectMenu = builder.build();
        selectHandlers.put(id, new SelectHandler(eventHandler, isStale, selectPermission));
        return selectMenu;
    }
}
