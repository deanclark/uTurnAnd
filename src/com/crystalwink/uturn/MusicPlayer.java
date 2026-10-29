package com.crystalwink.uturn;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.EnumMap;
import java.util.Map;

/**
 * Plays the generated background music (see {@link MusicSynth}) as a seamless loop.
 * Call {@link #resume()} / {@link #pause()} from the activity lifecycle and {@link #release()} when finished.
 * The music is rendered off the UI thread the first time a mood is chosen and then kept in memory.
 */
final class MusicPlayer {
    private static final String TAG = "uTurn";
    private static final float DUCK_FACTOR = 0.25f;   // share of the volume kept while another app talks over us

    private final AudioManager audioManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<MusicSynth.Mood, short[]> rendered = new EnumMap<>(MusicSynth.Mood.class);
    private final AudioFocusRequest focusRequest;

    private MusicSynth.Mood mood;       // chosen mood, null = music off
    private boolean appVisible;         // false while the app is in the background
    private boolean focusLost;          // another app is playing audio
    private AudioTrack track;
    private MusicSynth.Mood playingMood;
    private boolean released;
    private float volume = 0.6f;        // 0..1, the player's own gain on top of the device media volume
    private boolean ducked;

    MusicPlayer(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(this::onFocusChange)
                .build();
    }

    /** Choose the mood (null = off). Takes effect straight away if the app is visible. */
    void setMood(MusicSynth.Mood newMood) {
        mood = newMood;
        update();
    }

    /** Music gain 0..1, applied at once to the music that is playing. */
    void setVolume(float newVolume) {
        volume = Math.max(0f, Math.min(1f, newVolume));
        if (track != null) {
            track.setVolume(ducked ? volume * DUCK_FACTOR : volume);
        }
    }

    void resume() {
        appVisible = true;
        update();
    }

    void pause() {
        appVisible = false;
        update();
    }

    void release() {
        released = true;
        stopTrack();
        audioManager.abandonAudioFocusRequest(focusRequest);
    }

    private void onFocusChange(int change) {
        mainHandler.post(() -> {
            if (change == AudioManager.AUDIOFOCUS_GAIN) {
                focusLost = false;
                ducked = false;
                if (track != null) {
                    track.setVolume(volume);
                }
            } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                ducked = true;
                if (track != null) {
                    track.setVolume(volume * DUCK_FACTOR);
                }
            } else {
                focusLost = true;
            }
            update();
        });
    }

    /** Make the playing state match the chosen mood and whether the app is visible. */
    private void update() {
        if (released) {
            return;
        }
        boolean shouldPlay = mood != null && appVisible && !focusLost;
        if (!shouldPlay) {
            stopTrack();
            return;
        }
        if (track != null && playingMood == mood) {
            return;   // already playing the right thing
        }
        stopTrack();

        short[] pcm = rendered.get(mood);
        if (pcm != null) {
            startTrack(mood, pcm);
            return;
        }
        final MusicSynth.Mood wanted = mood;
        new Thread(() -> {
            short[] samples = MusicSynth.render(wanted);
            mainHandler.post(() -> {
                rendered.put(wanted, samples);
                update();   // starts playing if this mood is still the one wanted
            });
        }, "music-render").start();
    }

    private void startTrack(MusicSynth.Mood moodToPlay, short[] pcm) {
        if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Log.i(TAG, "music: audio focus refused");
            return;
        }
        try {
            AudioTrack newTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(MusicSynth.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.length * 2)
                    .build();
            newTrack.write(pcm, 0, pcm.length);
            newTrack.setLoopPoints(0, pcm.length, -1);
            newTrack.setVolume(ducked ? volume * DUCK_FACTOR : volume);
            newTrack.play();
            track = newTrack;
            playingMood = moodToPlay;
            Log.i(TAG, "music: playing " + moodToPlay + " state=" + newTrack.getPlayState());
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            Log.w(TAG, "music: could not start", e);
        }
    }

    private void stopTrack() {
        if (track != null) {
            Log.i(TAG, "music: stopped " + playingMood);
            track.stop();
            track.release();
            track = null;
            playingMood = null;
        }
    }
}
