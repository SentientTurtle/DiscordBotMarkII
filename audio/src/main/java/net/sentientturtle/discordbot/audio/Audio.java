package net.sentientturtle.discordbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.AudioManager;
import net.sentientturtle.discordbot.botmodules.simple.Voting;
import net.sentientturtle.discordbot.components.core.Core;
import net.sentientturtle.discordbot.components.core.FeatureLock;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.core.Shutdown;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.healthcheck.HealthStatus;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.Command;
import net.sentientturtle.discordbot.components.module.command.CommandCall;
import net.sentientturtle.discordbot.components.permission.BotPermission;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Module for audio playback
 */
public class Audio extends BotModule implements EventListener {
    private final Logger logger = LoggerFactory.getLogger(Audio.class);
    private final AudioPlayerManager manager;
    private AudioHandler sendHandler = null;

    public Audio() {
        FeatureLock.lockOrThrow(this.getClass(), FeatureLock.VOICE);

        this.manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new YoutubeAudioSourceManager(true));
        manager.registerSourceManager(new HttpAudioSourceManager() {
            @Override
            public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
                // HttpAudioSourceManager is a bit overeager and will throw exceptions on non-audio http links instead of returning null (null signals the sourcemanager is not suitable for this source)
                // This causes conflict with other source managers (e.g. on youtube video pages) as the player-manager will stop trying to load if an exception is thrown
                try {
                    return super.loadItem(manager, reference);
                } catch (FriendlyException ignored) {
                    return null;
                }
            }
        });
        manager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        manager.registerSourceManager(new BandcampAudioSourceManager());
        manager.registerSourceManager(new VimeoAudioSourceManager());

        HealthCheck.addInstance(this, () -> {
            if (this.sendHandler == null) {
                return HealthStatus.PAUSED;
            } else {
                return HealthStatus.RUNNING;
            }
        }, () -> {
            if (this.sendHandler != null) {
                return Optional.of(this.sendHandler.queueLength() + " tracks queued.");
            } else {
                return Optional.empty();
            }
        });

        Shutdown.registerHook(manager::shutdown);
    }

    private AudioSendHandler prepareSendHandler() {
        if (this.sendHandler == null) {
            this.sendHandler = new AudioHandler(manager.createPlayer());
        }
        return this.sendHandler;
    }

    private boolean joinVoiceChannel(CommandCall commandCall) {
        final AudioManager audioManager = commandCall.getGuild().getAudioManager();

        if (audioManager.getSendingHandler() == null || audioManager.getSendingHandler() != this.sendHandler) {
            audioManager.setSendingHandler(prepareSendHandler());
        }

        if (!audioManager.isConnected()) {
            GuildVoiceState voiceState = commandCall.getMember().getVoiceState();
            if (voiceState == null) return false;
            VoiceChannel voiceChannel = voiceState.getChannel();
            if (voiceChannel == null) return false;
            audioManager.openAudioConnection(voiceChannel);
        }
        return true;
    }

    @Command(commandName = "audio", subcommandName = "summon", description = "Summons the bot to your voice channel")
    public void summon(CommandCall commandCall) {
        var caller = commandCall.getMember();
        final VoiceChannel voiceChannel = Objects.requireNonNull(caller.getVoiceState()).getChannel();
        if (voiceChannel != null) {
            final AudioManager audioManager = commandCall.getGuild().getAudioManager();

            // Reset sendHandler if needed regardless of current connection status
            if (audioManager.getSendingHandler() == null || audioManager.getSendingHandler() != this.sendHandler) {
                audioManager.setSendingHandler(prepareSendHandler());
            }

            if (voiceChannel.equals(audioManager.getConnectedChannel())) {
                commandCall.error("⚠ I'm already in your voice channel");
            } else {
                audioManager.openAudioConnection(voiceChannel);
            }
        } else {
            commandCall.error("⚠ You are not in a voice channel");
        }
    }

    @Command(commandName = "audio", subcommandName = "dismiss", description = "Dismisses the bot from voice channels")
    public void dismiss(CommandCall commandCall) {
        final AudioManager audioManager = commandCall.getGuild().getAudioManager();
        audioManager.closeAudioConnection();
        audioManager.setSendingHandler(null);
        if (this.sendHandler != null) {
            this.sendHandler.destroy();
            this.sendHandler = null;
        }
    }

    @Command(commandName = "audio", subcommandName = "play", description = "plays an audio track")
    public void play(
            CommandCall commandCall,
            @Command.Parameter(name = "track", description = "Track or playlist to play", optional = true) String tracks
    ) {
        if (tracks != null && tracks.length() > 0) {
            if (joinVoiceChannel(commandCall) && this.sendHandler != null) {
                commandCall.getChannel()
                        .sendMessage("🔄 Loading track(s)...")
                        .queue(message -> {
                            this.manager.loadItem(tracks, new AudioLoadResultHandler() {
                                @Override
                                public void trackLoaded(AudioTrack track) {
                                    sendHandler.queueTrack(track);
                                    message.editMessage("☑ Track loaded: " + track.getInfo().title).queue();
                                }

                                @Override
                                public void playlistLoaded(AudioPlaylist playlist) {
                                    sendHandler.queuePlaylist(playlist);
                                    message.editMessage("☑ " + playlist.getTracks().size() + " tracks loaded").queue();
                                }

                                @Override
                                public void noMatches() {
                                    message.editMessage("ℹ Could not find source matching query").queue();
                                }

                                @Override
                                public void loadFailed(FriendlyException exception) {
                                    message.editMessage("⚠ Error loading track").queue();
                                    logger.debug("Error loading audio track", exception);
                                }
                            });
                        });
            } else {
                commandCall.error("⚠ Bot cannot join voice channel");
            }
        } else if (this.sendHandler != null && this.sendHandler.player.isPaused()) {
            this.sendHandler.player.setPaused(false);
        } else {
            commandCall.error("⚠ You must specify a track to play if the queue is empty");
        }
    }

    @Command(commandName = "audio", subcommandName = "vote", description = "Starts a vote for which audio track to play")
    public void play_vote(
            CommandCall commandCall,
            @Command.Parameter(name = "tracks", description = "Tracks to vote between, separated by comma") String trackString
    ) {
        List<String> options = Arrays.stream(trackString.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        if (options.size() >= 2 && options.size() <= 25) {
            if (joinVoiceChannel(commandCall) && this.sendHandler != null) {
                commandCall.reply("Holding vote...", true);
                List<AudioItem> tracks = new ArrayList<>();
                var futures = options.stream().map(option -> this.manager.loadItem(option, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        tracks.add(track);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        tracks.add(playlist);
                    }

                    @Override
                    public void noMatches() {
                        // ignored
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        // ignored
                    }
                })).toArray(Future<?>[]::new);

                Scheduling.submit(() -> {
                    for (int i = 0; i < 240; i++) { // 500ms wait per cycle, 2 minute total
                        if (Arrays.stream(futures).allMatch(Future::isDone)) break;
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }

                    if (tracks.size() > 2) {
                        var trackMap = tracks.stream().collect(Collectors.toMap(audioItem -> {
                            if (audioItem instanceof AudioTrack) {
                                return ((AudioTrack) audioItem).getInfo().title;
                            } else if (audioItem instanceof AudioPlaylist) {
                                return ((AudioPlaylist) audioItem).getName();
                            } else {
                                throw new RuntimeException("Unreachable");
                            }
                        }, Function.identity()));
                        Voting.runVote(
                                "Vote on the next audio to play...",
                                commandCall.getChannel(),
                                new ArrayList<>(trackMap.keySet()),
                                winner -> {
                                    var audioItem = trackMap.get(winner);
                                    if (audioItem instanceof AudioTrack) {
                                        sendHandler.queueTrack((AudioTrack) audioItem);
                                    } else if (audioItem instanceof AudioPlaylist) {
                                        sendHandler.queuePlaylist((AudioPlaylist) audioItem);
                                    } else {
                                        throw new RuntimeException("Unreachable");
                                    }
                                },
                                1,
                                TimeUnit.MINUTES,
                                BotPermission.EVERYONE()    // TODO: Make configurable
                        );
                    } else if (tracks.size() == 1) {
                        var track = tracks.get(0);
                        if (track instanceof AudioTrack) {
                            commandCall.getChannel().sendMessage("Only one track loaded successfully, now playing: " + ((AudioTrack) track).getInfo().title).queue();
                            sendHandler.queueTrack((AudioTrack) track);
                        } else if (track instanceof AudioPlaylist) {
                            commandCall.getChannel().sendMessage("Only one item loaded successfully, now playing: " + ((AudioPlaylist) track).getName()).queue();
                            sendHandler.queuePlaylist((AudioPlaylist) track);
                        }
                    } else {
                        commandCall.getChannel().sendMessage("No audio tracks loaded successfully, vote skipped.").queue();
                    }
                });
            } else {
                commandCall.error("⚠ Bot cannot join voice channel");
            }
        } else {
            commandCall.error("⚠ You must specify at least 2 and at most 25 options");
        }
    }

    @Command(commandName = "audio", subcommandName = "pause", description = "Pauses audio playback")
    public void pause(CommandCall commandCall) {
        if (this.sendHandler != null) {
            this.sendHandler.player.setPaused(true);
        } else {
            commandCall.error("⚠ Bot is not currently playing anything");
        }
    }

    @Command(commandName = "audio", subcommandName = "skip", description = "Skips the currently playing track")
    public void skip(CommandCall commandCall) {
        if (this.sendHandler != null) {
            this.sendHandler.skip();
        } else {
            commandCall.error("⚠ Bot is not currently playing anything");
        }
    }

    @Command(commandName = "audio", subcommandName = "shuffle", description = "Shuffles the current playlist once; New tracks will be added to the end of the playlist")
    public void shuffle(CommandCall commandCall) {
        if (this.sendHandler != null) {
            this.sendHandler.shuffle();
        } else {
            commandCall.error("⚠ Bot is not currently playing anything");
        }
    }

    @Command(commandName = "audio", subcommandName = "repeat", description = "Changes repeat mode")
    public void repeat(CommandCall commandCall, @Command.Parameter(name = "mode", description = "Repeat mode", optional = true) @Command.Choices({"on", "off"}) String mode) {
        if (this.sendHandler != null) {
            Boolean isRepeating = switch (mode) {
                case "on" -> this.sendHandler.setRepeat(true);
                case "off" -> this.sendHandler.setRepeat(false);
                default -> this.sendHandler.toggleRepeat();
            };
            if (isRepeating) {
                commandCall.reply("Repeat now **ON**");
            } else {
                commandCall.reply("Repeat now **OFF**");
            }
        } else {
            commandCall.error("⚠ Bot is not currently playing anything");
        }
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (this.sendHandler != null && event instanceof GuildVoiceLeaveEvent guildVoiceLeaveEvent) {
            var members = guildVoiceLeaveEvent.getOldValue().getMembers();
            assert Core.getJDA() != null;   // We wouldn't be receiving events if Core's JDA is unset
            if (members.size() == 1 && members.get(0).getUser() == Core.getJDA().getSelfUser()) {   // If bot is the only user in the voice channel
                final AudioManager audioManager = guildVoiceLeaveEvent.getGuild().getAudioManager();
                audioManager.closeAudioConnection();
                audioManager.setSendingHandler(null);
                if (this.sendHandler != null) {
                    this.sendHandler.destroy();
                    this.sendHandler = null;
                }
            }
        }
    }
}
