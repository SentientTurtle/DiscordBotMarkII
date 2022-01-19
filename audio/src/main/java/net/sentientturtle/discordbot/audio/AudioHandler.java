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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridge object to handle interaction between JDA and LavaPlayer, also provides audio playback status as presence
 */
public class AudioHandler implements AudioSendHandler, AudioEventListener, PresenceProvider {
    private final Logger logger = LoggerFactory.getLogger(Audio.class);
    public final AudioPlayer player;
    private ArrayDeque<AudioTrack> queue;
    private AudioFrame frame;   // Current ~20ms of audio
    private final AtomicBoolean isPlaying;
    private final AtomicBoolean isRepeating;

    public AudioHandler(@NotNull AudioPlayer player) {
        this.player = player;
        this.queue = new ArrayDeque<>();
        this.frame = null;
        this.isPlaying = new AtomicBoolean(false);
        this.isRepeating = new AtomicBoolean(false);
        player.addListener(this);
        PresenceManager.registerProvider(this, PresenceImportance.FOREGROUND);
    }

    public void destroy() {
        this.player.destroy();
        PresenceManager.removeProvider(this);
    }

    /**
     * Starts playing the next track in the queue if currently playing audio.<br>
     * If not currently playing audio, simply deletes the first entry in the playback queue
     */
    public void skip() {
        if (isPlaying.get()) {
            player.startTrack(queue.pollFirst(), false);
        } else {
            queue.pollFirst();
        }
    }

    /**
     * Shuffles the playback queue once.<br>
     * Tracks are still played sequentially, and tracks added after shuffling will be queued sequentially at the end of the current playlist.
     */
    public void shuffle() {
        var list = Arrays.asList(queue.toArray(AudioTrack[]::new));
        Collections.shuffle(list);
        queue = new ArrayDeque<>(list);
    }

    public boolean isRepeating() {
        return this.isRepeating.get();
    }

    /**
     * @param repeat True to enable repeating, false to disable.
     * @return The current repeat status
     */
    public boolean setRepeat(boolean repeat) {
        this.isRepeating.set(repeat);
        return this.isRepeating.get();  // In the event of multiple concurrent calls of this method, the last call to return will provide the correct repeat status
    }

    /**
     * Makes a best-effort attempt to toggle repeat status<br>
     * May not toggle if the repeat status is changed concurrently
     * @return Current repeat status
     */
    public boolean toggleRepeat() {
        /*
         * AtomicBoolean does not (yet) support negating the value.
         * The below code approximates negation by:
         * 1. Attempt to set the boolean to false only if it is true.
         * 2. If this succeeds, the value has been negated, and simply return the latest value with #get()
         * 3. If this fails, attempt to set the boolean to true only if it is still false.
         * 4. This succeeds if the value has not been mutated concurrently; Step 1 only fails if the value is already false.
         * 5. This fails if the value has been mutated concurrently and has been changed during this method call. No further attempts are made to mutate the value. The concurrent modification is considered to be the negation.
         * As compromise, this means the value may not respect all calls to mutate it. For the purposes of this method this compromise is acceptable.
         */
        if (!this.isRepeating.compareAndSet(true, false)) {
            this.isRepeating.compareAndSet(false, true);
        }
        return this.isRepeating.get();
    }

    /**
     * Adds the specified track to the end of the queue, and if not currently playing audio or paused, starts playing the first track in the queue.
     * @param track Track to add to the playlist
     */
    public void queueTrack(@NotNull AudioTrack track) {
        queue.addLast(track);
        if (!player.isPaused() && !isPlaying.get()) {
            player.startTrack(queue.pollFirst(), false);
        }
    }

    /**
     * Adds the specified playlist of tracks to the end of the queue, and if not currently playing audio or paused, starts playing the first track in the queue.
     * @param playlist List of tracks to add to the queue
     */
    public void queuePlaylist(AudioPlaylist playlist) {
        queue.addAll(playlist.getTracks());
        if (!player.isPaused() && !isPlaying.get()) {
            player.startTrack(queue.pollFirst(), false);
        }
    }

    public int queueLength() {
        return queue.size();
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
            this.isPlaying.set(false);
        } else if (event instanceof PlayerResumeEvent) {
            this.isPlaying.set(true);
        } else if (event instanceof TrackEndEvent) {
            this.isPlaying.set(false);
            if (isRepeating.get()) queue.add(((TrackEndEvent) event).track.makeClone());
            if (((TrackEndEvent) event).endReason.mayStartNext) player.startTrack(queue.pollFirst(), false);
        } else if (event instanceof TrackExceptionEvent) {
            this.isPlaying.set(false);
            logger.debug("Error playing audio track ", ((TrackExceptionEvent) event).exception);
        } else if (event instanceof TrackStartEvent) {
            this.isPlaying.set(true);
        } else if (event instanceof TrackStuckEvent) {
            this.isPlaying.set(false);
        } else {
            throw new RuntimeException("Unreachable code; Unknown AudioEvent type: " + event.getClass() + "\t" + event);    // At time of writing, all subclasses of AudioEvent are handled. In the event new subclasses are added, an exception is raised.
        }
    }

    @Override
    public Activity getActivity() {
        if (this.isPlaying.get()) {
            String icon;
            if (this.player.isPaused()) {
                icon = "⏸";
            } else if (this.isRepeating.get()){
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
