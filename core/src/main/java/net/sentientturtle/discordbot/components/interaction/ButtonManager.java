package net.sentientturtle.discordbot.components.interaction;

import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.permission.BotPermission;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Manager for Discord Buttons<br>
 * Convenience class to automatically bind button events to callbacks<br>
 *  * Also eventhandler for button events
 */
public class ButtonManager implements StaticLoaded {
    private static final Logger logger = LoggerFactory.getLogger(ButtonManager.class);
    private static final AtomicLong buttonID = new AtomicLong(System.currentTimeMillis());     // Initialise using calendar time; This provides a best-effort attempt to ensure buttonIDs are not reused even if the program is restarted.

    private static final ConcurrentHashMap<String, ClickHandler> clickHandlers = new ConcurrentHashMap<>();   // This could be optimized by using array-backed storage as buttonIDs are sequential.

    private record ClickHandler(Consumer<ButtonClickEvent> eventHandler, BooleanSupplier isStale, BotPermission clickPermission) {}

    static {
        // TODO: Replace this hack with a better way of avoiding memory leaks (Endless accumulation of handlers)
        Scheduling.scheduleAtFixedRate(() -> clickHandlers.values().removeIf(clickHandler -> clickHandler.isStale.getAsBoolean()), 10, 10, TimeUnit.MINUTES);
    }

    public static void handleEvent(ButtonClickEvent buttonClickEvent) {
        var button = buttonClickEvent.getButton();
        if (button != null) {
            var id = button.getId();
            if (id != null) {
                ClickHandler onClick = clickHandlers.get(id);
                if (
                        onClick != null &&
                        buttonClickEvent.getMember() != null &&
                        onClick.clickPermission.memberHasPermission(buttonClickEvent.getMember())
                ) {
                    try {
                        onClick.eventHandler.accept(buttonClickEvent);
                    } catch (Throwable t) {
                        logger.warn("Exception in buttonClick handler: " + onClick.eventHandler, t);
                    }
                }
            }
        }
        if (!buttonClickEvent.isAcknowledged()) {
            buttonClickEvent.deferEdit().queue();
        }
    }

    private static Button newButton(@NotNull ButtonStyle style, @Nullable String label, @Nullable Emoji emoji, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        var id = "Button-" + buttonID.getAndIncrement();
        var button = Button.of(style, id, label, emoji);
        clickHandlers.put(id, new ClickHandler(onClick, staleCheck, clickPermission));
        return button;
    }

    public static Button newLink(@NotNull String label, @NotNull String url) {
        return Button.of(ButtonStyle.LINK, url, label, null);
    }

    public static Button newLink(@NotNull Emoji emoji, @NotNull String url) {
        return Button.of(ButtonStyle.LINK, url, null, emoji);
    }

    public static Button newLink(@NotNull String label, @NotNull Emoji emoji, @NotNull String url) {
        return Button.of(ButtonStyle.LINK, url, label, emoji);
    }

    public static Button newPrimary(@NotNull String label, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.PRIMARY, label, null, onClick, staleCheck, clickPermission);
    }

    public static Button newPrimary(@NotNull Emoji emoji, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.PRIMARY, null, emoji, onClick, staleCheck, clickPermission);
    }

    public static Button newPrimary(@NotNull String label, @NotNull Emoji emoji, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.PRIMARY, label, emoji, onClick, staleCheck, clickPermission);
    }

    public static Button newSecondary(@NotNull String label, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.SECONDARY, label, null, onClick, staleCheck, clickPermission);
    }

    public static Button newSecondary(@NotNull Emoji emoji, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.SECONDARY, null, emoji, onClick, staleCheck, clickPermission);
    }

    public static Button newSecondary(@NotNull String label, @NotNull Emoji emoji, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.SECONDARY, label, emoji, onClick, staleCheck, clickPermission);
    }

    public static Button newSuccess(@NotNull String label, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.SUCCESS, label, null, onClick, staleCheck, clickPermission);
    }

    public static Button newSuccess(@NotNull Emoji emoji, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.SUCCESS, null, emoji, onClick, staleCheck, clickPermission);
    }

    public static Button newSuccess(@NotNull String label, @NotNull Emoji emoji, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.SUCCESS, label, emoji, onClick, staleCheck, clickPermission);
    }

    public static Button newDanger(@NotNull String label, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.DANGER, label, null, onClick, staleCheck, clickPermission);
    }

    public static Button newDanger(@NotNull Emoji emoji, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.DANGER, null, emoji, onClick, staleCheck, clickPermission);
    }

    public static Button newDanger(@NotNull String label, @NotNull Emoji emoji, @NotNull Consumer<ButtonClickEvent> onClick, @NotNull BooleanSupplier staleCheck, @NotNull BotPermission clickPermission) {
        return newButton(ButtonStyle.DANGER, label, emoji, onClick, staleCheck, clickPermission);
    }
}
