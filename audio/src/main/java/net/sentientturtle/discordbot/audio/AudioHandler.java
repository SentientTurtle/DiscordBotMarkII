package net.sentientturtle.discordbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.*;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Activity;
import net.sentientturtle.discordbot.components.presence.PresenceImportance;
import net.sentientturtle.discordbot.components.presence.PresenceManager;
import net.sentientturtle.discordbot.components.presence.PresenceProvider;
import net.sentientturtle.util.TimeFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Bridge object to handle interaction between JDA and LavaPlayer, also provides audio playback status as presence
 */
public class AudioHandler implements AudioSendHandler, AudioEventListener, PresenceProvider {
    private final Logger logger = LoggerFactory.getLogger(Audio.class);
    public final AudioPlayer player;
    private ArrayDeque<AudioTrack> queue;
    private AudioFrame frame;
    private volatile boolean isPlaying;    // TODO: Check if this really needs to be volatile
    private boolean isRepeating;

    public AudioHandler(@NotNull AudioPlayer player) {
        this.player = player;
        this.queue = new ArrayDeque<>();
        this.frame = null;
        this.isPlaying = false;
        this.isRepeating = false;
        player.addListener(this);
        PresenceManager.registerProvider(this, PresenceImportance.FOREGROUND);
    }

    public void destroy() {
        this.player.destroy();
        PresenceManager.removeProvider(this);
    }

    public void skip() {
        if (isPlaying) {
            player.startTrack(queue.pollFirst(), false);
        } else {
            queue.pollFirst();
        }
    }

    public void shuffle() {
        var list = Arrays.asList(queue.toArray(AudioTrack[]::new));
        Collections.shuffle(list);
        queue = new ArrayDeque<>(list);
    }

    public boolean isRepeating() {
        return this.isRepeating;
    }

    public boolean setRepeat(boolean repeat) {
        this.isRepeating = repeat;
        return this.isRepeating;
    }

    // Has a race condition
    public boolean toggleRepeat() {
        this.isRepeating = !this.isRepeating;
        return this.isRepeating;
    }

    public void queueTrack(@NotNull AudioTrack track) {
        queue.addLast(track);
        if (!player.isPaused() && !isPlaying) {
            player.startTrack(queue.pollFirst(), false);
        }
    }

    public void queuePlaylist(AudioPlaylist playlist) {
        queue.addAll(playlist.getTracks());
        if (!player.isPaused() && !isPlaying) {
            player.startTrack(queue.pollFirst(), false);
        }
    }

    @Override
    public boolean canProvide() {
        return (frame = player.provide()) != null;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(frame.getData());
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    @Override
    public void onEvent(AudioEvent event) {
        if (event instanceof PlayerPauseEvent) {
            this.isPlaying = false;
        } else if (event instanceof PlayerResumeEvent) {
            this.isPlaying = true;
        } else if (event instanceof TrackEndEvent) {
            this.isPlaying = false;
            if (isRepeating) queue.add(((TrackEndEvent) event).track.makeClone());
            if (((TrackEndEvent) event).endReason.mayStartNext) player.startTrack(queue.pollFirst(), false);
        } else if (event instanceof TrackExceptionEvent) {
            this.isPlaying = false;
            logger.debug("Error playing audio track ", ((TrackExceptionEvent) event).exception);
        } else if (event instanceof TrackStartEvent) {
            this.isPlaying = true;
        } else if (event instanceof TrackStuckEvent) {
            this.isPlaying = false;
        } else {
            throw new RuntimeException("Unreachable code!");
        }
    }

    @Override
    public Activity getActivity() {
        if (this.isPlaying) {
            String icon;
            if (this.player.isPaused()) {
                icon = "⏸";
            } else if (this.isRepeating){
                icon = "🔁";
            } else {
                icon = "▶";
            }
            var track = player.getPlayingTrack();
            if (track != null) {
                if (track.getInfo().isStream) {
                    return Activity.listening(
                            String.format(
                                    "%s %s %s",
                                    icon,
                                    track.getInfo().title,
                                    TimeFormat.formatHMS(track.getPosition(), TimeUnit.MILLISECONDS)
                            )
                    );
                } else {
                    return Activity.listening(
                            String.format(
                                    "%s %s %s/%s",
                                    icon,
                                    track.getInfo().title,
                                    TimeFormat.formatHMS(track.getPosition(), TimeUnit.MILLISECONDS),
                                    TimeFormat.formatHMS(track.getDuration(), TimeUnit.MILLISECONDS)
                            )
                    );
                }
            } else {
                logger.warn("Player status de-synchronized!");
                return null;
            }
        } else {
            return null;
        }
    }
}
