package net.sentientturtle.discordbot.botmodules.simple;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.sentientturtle.discordbot.components.core.Core;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.interaction.SelectionMenuManager;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.Command;
import net.sentientturtle.discordbot.components.module.command.CommandCall;
import net.sentientturtle.discordbot.components.permission.BotPermission;
import net.sentientturtle.util.TimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Module that provides commands to run votes, as well as exposing an API for other modules to run votes.
 */
public class Voting extends BotModule {
    private static final Logger logger = LoggerFactory.getLogger(Voting.class);
    private static final long MAXIMUM_VOTE_DURATION_HOURS = 24;
    private static final AtomicLong voteCount = new AtomicLong(0);
    private static final ConcurrentHashMap<Long, Vote> runningVotes = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    private static class Vote {
        private final int[] results;
        private final Set<Long> votedUsers;

        public final String name;
        public final List<String> options;
        public final Consumer<String> onComplete;
        public final long channelID;
        public final long messageID;

        public Vote(String name, List<String> options, Consumer<String> onComplete, long channelID, long messageID) {
            this.name = name;
            this.results = new int[options.size()];
            this.options = Collections.unmodifiableList(options);
            this.onComplete = onComplete;
            this.channelID = channelID;
            this.messageID = messageID;
            votedUsers = Collections.synchronizedSet(new HashSet<>());
        }
    }

    public static void runVote(String voteTitle, MessageChannel channel, List<String> options, Consumer<String> onComplete, long voteDuration, TimeUnit durationUnit, BotPermission votePermission) throws IllegalArgumentException {
        if (options.size() < 1) throw new IllegalArgumentException("Too few options!");
        if (options.size() > 25) throw new IllegalArgumentException("Too many options!");
        long voteID = voteCount.getAndIncrement();

        channel.sendMessage(
                new MessageBuilder()
                        .append(voteTitle, MessageBuilder.Formatting.BOLD).append('\n')
                        .append("Vote lasts ").append(TimeFormat.formatWDHMS(voteDuration, durationUnit)).append(".\n")
                        .append("Use selection box to vote, you may select multiple options.")
                        .setActionRows(ActionRow.of(SelectionMenuManager.newSelect(
                                "Select options to vote",
                                selectionMenuEvent -> {
                                    var vote = runningVotes.get(voteID);
                                    if (vote != null) {
                                        if (vote.votedUsers.add(selectionMenuEvent.getUser().getIdLong())) {
                                            selectionMenuEvent.deferReply(true).setContent("Vote accepted!").queue();
                                            for (String selectedValue : selectionMenuEvent.getValues()) {
                                                vote.results[Integer.parseInt(selectedValue)]++;
                                            }
                                        } else {
                                            selectionMenuEvent.deferReply(true).setContent("You have already voted!").queue();
                                        }
                                    } else {
                                        selectionMenuEvent.deferReply(true).setContent("Vote has already ended!").queue();
                                    }
                                },
                                () -> runningVotes.containsKey(voteID),
                                votePermission,
                                options.toArray(String[]::new)
                        )))
                        .build()
        ).queue(message -> runningVotes.put(voteID, new Vote(voteTitle, options, onComplete, channel.getIdLong(), message.getIdLong())));

        Scheduling.schedule(
                () -> {
                    var vote = runningVotes.remove(voteID);
                    if (vote != null) {

                        var maxVotes = Arrays.stream(vote.results).max().orElse(0);
                        List<Integer> winners = new ArrayList<>(vote.results.length);
                        for (int i = 0; i < vote.results.length; i++) {
                            if (vote.results[i] == maxVotes) {
                                winners.add(i);
                            }
                        }
                        var winningOption = vote.options.get(winners.get(random.nextInt(winners.size())));

                        // noinspection ConstantConditions       #getJDA May not NPE; This is only ever called after JDA has initialised
                        var textChannel = Core.getJDA().getTextChannelById(vote.channelID);
                        if (textChannel != null) {
                            textChannel.retrieveMessageById(vote.messageID)
                                    .complete()
                                    .editMessage(
                                            new MessageBuilder()
                                                    .append(vote.name, MessageBuilder.Formatting.BOLD).append('\n')
                                                    .append("Vote has ended.\n")
                                                    .append("Winner: ").append(winningOption, MessageBuilder.Formatting.BLOCK)
                                                    .build()
                                    )
                                    .queue();
                        }

                        vote.onComplete.accept(winningOption);
                    } else {
                        logger.warn("Vote #" + voteID + " was deleted before finishing!");
                    }
                },
                voteDuration,
                durationUnit
        );
    }

    @Command(description = "Run a vote")
    public void runvote(
            CommandCall commandCall,
            @Command.Parameter(name = "duration", description = "Duration of the vote") String durationString,
            @Command.Parameter(name = "options", description = "Options to choose between, separated by commas.") String optionString,
            @Command.Parameter(name = "role", description = "Only users from this role may vote", optional = true) Role permissionRole
    ) {
        try {
            long duration = Long.parseLong(durationString, 0, durationString.length() - 1, 10);
            TimeUnit durationUnit = switch (durationString.substring(durationString.length() - 1)) {
                case "s", "S" -> TimeUnit.SECONDS;
                case "m", "M" -> TimeUnit.MINUTES;
                case "h", "H" -> TimeUnit.HOURS;
                default -> throw new NumberFormatException("Invalid timeunit");  // Rather nasty control-flow exception, but we must catch anyway. (see Long#parseLong above)
            };

            if (durationUnit.toHours(duration) < MAXIMUM_VOTE_DURATION_HOURS) {
                List<String> options = Arrays.stream(optionString.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList());
                if (options.size() >= 2) {
                    for (int i = 0; i < options.size(); i++) {
                        if (options.get(i).length() > 25) {
                            commandCall.error("⚠ Option #" + i + " too long! (Length must be <= 25)");
                            return;
                        }
                    }
                    commandCall.reply("Holding vote...", true);
                    var member = commandCall.getMember();

                    BotPermission votePermission;
                    if (permissionRole != null) {
                        votePermission = BotPermission.ROLE(permissionRole);
                    } else {
                        votePermission = BotPermission.EVERYONE();
                    }
                    runVote("User vote by " + member.getEffectiveName(), commandCall.getChannel(), options, s -> {}, duration, durationUnit, votePermission);
                } else {
                    commandCall.error("⚠ You must specify at least two options");
                }
            } else {
                commandCall.error("⚠ vote duration too long!");
            }
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            commandCall.error("⚠ Invalid duration");
        } catch (IllegalArgumentException ignored) {
            commandCall.error("⚠ Too many options");
        }
    }
}
