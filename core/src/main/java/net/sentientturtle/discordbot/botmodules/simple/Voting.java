package net.sentientturtle.discordbot.botmodules.simple;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.sentientturtle.discordbot.components.core.Core;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand;
import net.sentientturtle.discordbot.helpers.ErrorHelper;
import net.sentientturtle.util.TimeFormat;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.MESSAGE_WRITE;

/**
 * Module that provides commands to run votes, as well as exposing an API for other modules to run votes.
 */
public class Voting extends BotModule {
    private static final String[] reactionEmoji = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟"};
    private static final long MAXIMUM_VOTE_DURATION = 24;
    private static final Random random = new Random();

    public static void runVote(String voteTitle, TextChannel channel, List<String> options, Consumer<String> onComplete, long voteDuration, TimeUnit durationUnit) {
        if (options.size() > reactionEmoji.length) throw new IllegalArgumentException("Too many options!");

        StringBuilder optionList = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            optionList.append(reactionEmoji[i]).append(" ").append(options.get(i));
            if (i < options.size() - 1) optionList.append("\n");
        };

        var embedBuilder = new EmbedBuilder()
                                   .setTitle(voteTitle)
                                   .setDescription("Vote lasts " + TimeFormat.formatWDHMS(voteDuration, durationUnit) + " from start")
                                   .addField("Use reactions to vote", optionList.toString(), false);

        var content = new EmbedBuilder(embedBuilder).build();    // Make a clone, we'll be using the same embed later
        long channelID = channel.getIdLong();
        channel.sendMessageEmbeds(content)
                .queue(message -> {
                    long messageID = message.getIdLong();
                    for (int i = 0; i < options.size(); i++) {
                        message.addReaction(reactionEmoji[i]).queue();
                    }

                    Scheduling.schedule(
                            () -> {
                                //noinspection ConstantConditions       #getJDA May not NPE; This is only ever called after JDA has initialised
                                Message updatedMessage = Core.getJDA().getTextChannelById(channelID)
                                                                 .retrieveMessageById(messageID)
                                                                 .complete();

                                int maxCount = -1;
                                int[] counts = new int[options.size()];

                                for (int i = 0; i < options.size(); i++) {
                                    String optionEmoji = reactionEmoji[i];
                                    int count = updatedMessage.getReactions().stream()
                                                        .filter(r -> r.getReactionEmote().getName().equals(optionEmoji))
                                                        .findFirst()
                                                        .map(MessageReaction::getCount)
                                                        .orElse(-1);
                                    counts[i] = count;
                                    if (count > maxCount) {
                                        maxCount = count;
                                    }
                                }

                                ArrayList<String> winningOptions = new ArrayList<>(options.size());
                                for (int i = 0; i < options.size(); i++) {
                                    if (counts[i] == maxCount) {
                                        winningOptions.add(options.get(i));
                                    }
                                }

                                String winner = winningOptions.get(random.nextInt(winningOptions.size()));

                                MessageEmbed embedUpdate = embedBuilder.setDescription(null).addField("Vote ended", "Winner: " + winner, false).build();
                                message.editMessageEmbeds(embedUpdate).queue();
                                onComplete.accept(winner);
                            },
                            voteDuration,
                            durationUnit
                    );
                });
    }

    @ATextCommand(syntax = "vote <duration> <option1, option2, ...>", helpText = "Stops all docker configurations", discordPermissions = {MESSAGE_WRITE})
    public void vote(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        final String[] split = args.split(" ", 2);
        if (split.length > 1) {
            String durationString = split[0];
            String optionString = split[1];

            try {
                long duration = Long.parseLong(durationString, 0, durationString.length() - 1, 10);
                TimeUnit durationUnit = switch (durationString.substring(durationString.length() - 1)) {
                    case "s", "S" -> TimeUnit.SECONDS;
                    case "m", "M" -> TimeUnit.MINUTES;
                    case "h", "H" -> TimeUnit.HOURS;
                    default -> throw new NumberFormatException("Invalid timeunit");  // Rather nasty control-flow exception, but we must catch anyway. (see Long#parseLong above)
                };

                if (durationUnit.toHours(duration) < MAXIMUM_VOTE_DURATION) {
                    List<String> options = Arrays.stream(optionString.split(","))
                                                   .map(String::trim)
                                                   .filter(s -> !s.isBlank())
                                                   .collect(Collectors.toList());
                    if (options.size() >= 2) {
                        runVote("User vote", event.getChannel(), options, s -> {}, duration, durationUnit);
                    } else {
                        ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "You must specify at least two options");
                    }
                } else {
                    ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "vote duration too long!");
                }
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Invalid duration");
            } catch (IllegalArgumentException ignored) {
                ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Too many options");
            }
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "You must specify a duration and at least two options");
        }
    }
}
