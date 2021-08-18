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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.AudioManager;
import net.sentientturtle.discordbot.botmodules.simple.Voting;
import net.sentientturtle.discordbot.components.core.ExclusiveFeatures;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.core.Shutdown;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.annotation.ATextCommand;
import net.sentientturtle.discordbot.helpers.ErrorHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.*;

/**
 * Module for audio playback
 */
public class Audio extends BotModule implements EventListener {
    private final Logger logger = LoggerFactory.getLogger(Audio.class);
    private final AudioPlayerManager manager;
    private AudioHandler sendHandler = null;

    public Audio() {
        ExclusiveFeatures.lockOrThrow(this.getClass(), ExclusiveFeatures.VOICE);    // TODO: Handle presence via settings

        this.manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new YoutubeAudioSourceManager(true));
        manager.registerSourceManager(new HttpAudioSourceManager() {
            @Override
            public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
                // HttpAudioSourceManager is a bit overeager and will throw exceptions on non-audio http links instead of returning null
                // and conflict with other source managers (e.g. on youtube video pages)
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

        Shutdown.registerHook(manager::shutdown);
    }

    private AudioSendHandler prepareSendHandler() {
        if (this.sendHandler == null) {
            this.sendHandler = new AudioHandler(manager.createPlayer());
        }
        return this.sendHandler;
    }

    private boolean joinVoiceChannel(GuildMessageReceivedEvent event) {
        final AudioManager audioManager = event.getGuild().getAudioManager();

        if (audioManager.getSendingHandler() == null || audioManager.getSendingHandler() != this.sendHandler) {
            audioManager.setSendingHandler(prepareSendHandler());
        }

        if (!audioManager.isConnected()) {
            Member member = event.getMember();
            if (member == null) return false;
            GuildVoiceState voiceState = member.getVoiceState();
            if (voiceState == null) return false;
            VoiceChannel voiceChannel = voiceState.getChannel();
            if (voiceChannel == null) return false;
            audioManager.openAudioConnection(voiceChannel);
        }
        return true;
    }

    @ATextCommand(helpText = "Summons this bot to the user's current voice channel", discordPermissions = {MESSAGE_WRITE, VOICE_CONNECT, VOICE_SPEAK})
    public void summon(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        var caller = event.getMember();
        if (caller != null) {
            final VoiceChannel voiceChannel = Objects.requireNonNull(caller.getVoiceState()).getChannel();
            if (voiceChannel != null) {
                final AudioManager audioManager = event.getGuild().getAudioManager();

                // Reset sendHandler if needed regardless of current connection status
                if (audioManager.getSendingHandler() == null || audioManager.getSendingHandler() != this.sendHandler) {
                    audioManager.setSendingHandler(prepareSendHandler());
                }

                if (voiceChannel.equals(audioManager.getConnectedChannel())) {
                    ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "I'm already in your voice channel");
                } else {
                    audioManager.openAudioConnection(voiceChannel);
                }
            } else {
                ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "You are not in a voice channel");
            }
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "You are not in a voice channel");
        }
    }

    @ATextCommand(helpText = "Dismisses this bot from voice channels", discordPermissions = {VOICE_CONNECT, VOICE_SPEAK})
    public void dismiss(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        final AudioManager audioManager = event.getGuild().getAudioManager();
        audioManager.closeAudioConnection();
        audioManager.setSendingHandler(null);
        if (this.sendHandler != null) {
            this.sendHandler.destroy();
            this.sendHandler = null;
        }
    }

    @ATextCommand(syntax = "play <track>", helpText = "Plays the specified song over voice", discordPermissions = {MESSAGE_WRITE, VOICE_CONNECT, VOICE_SPEAK})
    public void play(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        if (args.length() > 0) {
            if (joinVoiceChannel(event) && this.sendHandler != null) {
                event.getChannel()
                        .sendMessage("🔄 Loading track(s)...")
                        .queue(message -> {
                            this.manager.loadItem(args, new AudioLoadResultHandler() {
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
                ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Bot is not in a voice channel");
            }
        } else if (this.sendHandler != null && this.sendHandler.player.isPaused()) {
            this.sendHandler.player.setPaused(false);
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "You must specify a track to play");
        }
    }

    @ATextCommand(syntax = "play vote <track 1, track 2, ...>", helpText = "Hold a vote for which track to play", discordPermissions = {MESSAGE_WRITE, VOICE_CONNECT, VOICE_SPEAK})
    public void play_vote(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        List<String> options = Arrays.stream(args.split(","))
                                       .map(String::trim)
                                       .filter(s -> !s.isBlank())
                                       .collect(Collectors.toList());

        if (options.size() >= 2) {
            if (joinVoiceChannel(event) && this.sendHandler != null) {
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
                                event.getChannel(),
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
                                TimeUnit.MINUTES
                        );
                    } else if (tracks.size() == 1) {
                        var track = tracks.get(0);
                        if (track instanceof AudioTrack) {
                            event.getChannel().sendMessage("Only one track loaded successfully, now playing: " + ((AudioTrack) track).getInfo().title).queue();
                            sendHandler.queueTrack((AudioTrack) track);
                        } else if (track instanceof AudioPlaylist) {
                            event.getChannel().sendMessage("Only one item successfully, now playing: " + ((AudioPlaylist) track).getName()).queue();
                            sendHandler.queuePlaylist((AudioPlaylist) track);
                        }
                    } else {
                        event.getChannel().sendMessage("No audio tracks loaded successfully, vote skipped.").queue();
                    }
                });
            } else {
                ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Bot is not in a voice channel");
            }
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "You must specify at least two options");
        }
    }

    @ATextCommand(helpText = "pauses audio playback", discordPermissions = {MESSAGE_WRITE})
    public void pause(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        if (this.sendHandler != null) {
            this.sendHandler.player.setPaused(true);
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Bot is not currently playing anything");
        }
    }

    @ATextCommand(helpText = "skips the currently playing song", discordPermissions = {MESSAGE_WRITE})
    public void skip(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        if (this.sendHandler != null) {
            this.sendHandler.skip();
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Bot is not currently playing anything");
        }
    }

    @ATextCommand(helpText = "shuffles the current playlist", discordPermissions = {MESSAGE_WRITE})
    public void shuffle(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        if (this.sendHandler != null) {
            this.sendHandler.shuffle();
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Bot is not currently playing anything");
        }
    }

    @ATextCommand(syntax = "repeat [on|off|toggle]", helpText = "Turns on, off, or toggles repeat mode", discordPermissions = {MESSAGE_WRITE})
    public void repeat(String args, GuildMessageReceivedEvent event, EnumSet<Permission> selfPermissions) {
        if (this.sendHandler != null) {
            Boolean isRepeating = switch (args.toLowerCase()) {
                case "on" -> this.sendHandler.setRepeat(true);
                case "off" -> this.sendHandler.setRepeat(false);
                case "toggle" -> this.sendHandler.toggleRepeat();
                default -> null;
            };
            if (isRepeating == null) {
                ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Invalid command syntax");
            } else if (isRepeating) {
                event.getChannel().sendMessage("Repeat now **ON**").queue();
            } else {
                event.getChannel().sendMessage("Repeat now **OFF**").queue();
            }
        } else {
            ErrorHelper.reportFromGuildEvent(event, selfPermissions, "⚠", "Bot is not currently playing anything");
        }
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        // TODO: Auto-leave voice channel if last (other) user leaves
    }
}
