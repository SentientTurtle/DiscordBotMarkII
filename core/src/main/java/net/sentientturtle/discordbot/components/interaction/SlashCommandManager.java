package net.sentientturtle.discordbot.components.interaction;

import net.dv8tion.jda.api.entities.AbstractChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.sentientturtle.discordbot.components.core.Core;
import net.sentientturtle.discordbot.components.module.ModuleManager;
import net.sentientturtle.discordbot.components.module.command.CommandCall;
import net.sentientturtle.discordbot.components.module.command.UnifiedCommand;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Eventhandler for slashcommand events
 */
public class SlashCommandManager implements StaticLoaded {
    private static final Logger logger = LoggerFactory.getLogger(SlashCommandManager.class);

    public static void handleEvent(SlashCommandEvent slashCommandEvent) {
        var guild = slashCommandEvent.getGuild();
        if (guild != null) {
            assert slashCommandEvent.getMember() != null;
            if (guild.getIdLong() == Core.targetGuildId()) {
                String commandPath;
                if (slashCommandEvent.getSubcommandGroup() != null) {
                    commandPath = slashCommandEvent.getSubcommandName() + '/' + slashCommandEvent.getSubcommandGroup() + '/' + slashCommandEvent.getSubcommandName();
                } else if (slashCommandEvent.getSubcommandName() != null) {
                    commandPath = slashCommandEvent.getName() + '/' + slashCommandEvent.getSubcommandName();
                } else {
                    commandPath = slashCommandEvent.getName();
                }

                var command = ModuleManager.getCommandMap().get(commandPath);
                if (command != null) {
                    if (command.commandUserPermission().memberHasPermission(slashCommandEvent.getMember())) {
                        var call = new CommandCall() {
                            private final SlashCommandEvent event = slashCommandEvent;

                            @Override
                            public UnifiedCommand getCommand() {
                                return command;
                            }

                            @Override
                            public Object[] getParameters() {
                                int i = 0;
                                Object[] parameters = new Object[command.requiredParameters().length + command.optionalParameters().length];
                                for (UnifiedCommand.Parameter parameter : command.requiredParameters()) {
                                    OptionMapping option = slashCommandEvent.getOption(parameter.name());
                                    if (option != null) {
                                        Object value = getOptionValue(option);
                                        parameters[i] = value;
                                        i++;
                                    } else {
                                        throw new IllegalStateException("Discord has not sent required option!");
                                    }
                                }
                                for (UnifiedCommand.Parameter parameter : command.optionalParameters()) {
                                    OptionMapping option = slashCommandEvent.getOption(parameter.name());
                                    if (option != null) {
                                        Object value = getOptionValue(option);
                                        parameters[i] = value;
                                        i++;
                                    } else {
                                        parameters[i] = null;
                                        i++;
                                    }
                                }
                                return parameters;
                            }

                            private Object getOptionValue(OptionMapping option) {
                                return switch (option.getType()) {
                                    case STRING -> option.getAsString();
                                    case INTEGER -> option.getAsLong();
                                    case BOOLEAN -> option.getAsBoolean();
                                    case USER -> option.getAsUser();
                                    case CHANNEL -> {
                                        AbstractChannel channel = option.getAsMessageChannel();
                                        yield channel == null ? option.getAsGuildChannel() : channel;
                                    }
                                    case ROLE -> option.getAsRole();
                                    case MENTIONABLE -> option.getAsMentionable();
                                    default -> throw new IllegalStateException("Discord sent invalid option value type!");
                                };
                            }

                            public SlashCommandEvent getEvent() {
                                return event;
                            }
                        };
                        try {
                            command.command().call(call);
                            if (!slashCommandEvent.getInteraction().isAcknowledged()) {
                                slashCommandEvent.reply("Command complete.").setEphemeral(true).queue();
                            }
                        } catch (Throwable t) {
                            logger.debug("Error during command call", t);
                            if (!slashCommandEvent.getInteraction().isAcknowledged()) {
                                call.reply("Error during command: " + t, true);
                            }
                        }
                    } else {
                        slashCommandEvent.reply("You do not have permission for this command").setEphemeral(true).queue();
                    }
                } else {
                    logger.trace("Could not find command for event: " + slashCommandEvent);
                }
            } else {
                logger.info("Received event from non-target guild!");
            }
        } else {
            slashCommandEvent.reply("Commands may not be issued through private messages, please issue commands in the Guild!").queue();
        }
    }
}
