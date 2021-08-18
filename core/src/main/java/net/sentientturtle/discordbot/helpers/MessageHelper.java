package net.sentientturtle.discordbot.helpers;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageHelper {
    private static final long LINES_PER_PAGE = 10; // TODO: Move to settings

    public static Message paginate(@NotNull String title, @NotNull Stream<String> content, @Range(from = 1, to = Integer.MAX_VALUE) int page) {
        final var count = new int[]{0};
        final String pageText = content.skip((page - 1) * LINES_PER_PAGE)
                                        .limit(LINES_PER_PAGE)
                                        .peek(s -> count[0]++)
                                        .collect(Collectors.joining("\n"));

        var builder = new MessageBuilder()
                              .append(title)
                              .appendCodeBlock(pageText, "")
                              .append("Page ").append(String.valueOf(page));

        if (count[0] < LINES_PER_PAGE) builder.append(" (last page)");

        return builder.build();
    }
}
