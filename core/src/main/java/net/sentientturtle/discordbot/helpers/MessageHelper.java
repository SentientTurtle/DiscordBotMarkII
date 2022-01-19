package net.sentientturtle.discordbot.helpers;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.sentientturtle.discordbot.components.interaction.ButtonManager;
import net.sentientturtle.discordbot.components.permission.BotPermission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Class containing helper functions for constructing Discord messages
 */
public class MessageHelper {
    private static final long LINES_PER_PAGE = 10;

    /**
     * Creates an automatic pagination message using Discord buttons
     * @param title Title of the paginated content
     * @param content Lines of content to display
     * @param page Initial page to display
     * @return Message containing the pagination
     */
    public static Message paginate(@NotNull String title, @NotNull Collection<String> content, @Range(from = 1, to = Integer.MAX_VALUE) long page) {
        final String pageText = content.stream().skip((page - 1) * LINES_PER_PAGE)
                .limit(LINES_PER_PAGE)
                .collect(Collectors.joining("\n"));

        boolean isLastPage = (content.size() - (page * LINES_PER_PAGE)) < 0;

        AtomicBoolean isStale = new AtomicBoolean(false);

        var builder = new MessageBuilder()
                .append(title)
                .appendCodeBlock(pageText, "")
                .append("Page ").append(String.valueOf(page));

        if (page > 1) {
            if (!isLastPage) {
                builder.setActionRows(ActionRow.of(
                        ButtonManager.newPrimary("Previous", buttonClickEvent -> {
                            buttonClickEvent.editMessage(paginate(title, content, page - 1)).queue();
                            isStale.set(true);
                        }, isStale::get, BotPermission.EVERYONE()),
                        ButtonManager.newPrimary("Next", buttonClickEvent -> {
                            buttonClickEvent.editMessage(paginate(title, content, page + 1)).queue();
                            isStale.set(true);
                        }, isStale::get, BotPermission.EVERYONE())
                ));
            } else {
                builder.setActionRows(ActionRow.of(
                        ButtonManager.newPrimary("Previous", buttonClickEvent -> {
                            buttonClickEvent.editMessage(paginate(title, content, page - 1)).queue();
                            isStale.set(true);
                        }, isStale::get, BotPermission.EVERYONE())
                ));
            }
        } else if (!isLastPage) {
            builder.setActionRows(ActionRow.of(
                    ButtonManager.newPrimary("Next", buttonClickEvent -> {
                        buttonClickEvent.editMessage(paginate(title, content, page + 1)).queue();
                        isStale.set(true);
                    }, isStale::get, BotPermission.EVERYONE())
            ));
        }

        if (isLastPage) builder.append(" (last page)");

        return builder.build();
    }
}
