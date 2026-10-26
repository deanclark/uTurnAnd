package com.crystalwink.uturn;

import java.util.Random;

/**
 * Generates the background music in code, so there are no audio files and no third-party licences. Each mood is a short
 * loop of eight bars built from a chord progression, a pad, an arpeggio, a bass line and light percussion.
 *
 * <ul>
 * <li>SOOTHING: slow, soft pentatonic notes over long pads, no drums</li>
 * <li>FOCUS: steady medium pulse, gentle plucks and a quiet bass, light hats</li>
 * <li>INTENSE: fast minor-key driving bass, fast arpeggio, kick and hats</li>
 * </ul>
 *
 * Every note ends inside its own bar and the loop starts and ends silent, so it repeats without a click.
 */
public final class MusicSynth {
    public enum Mood { SOOTHING, FOCUS, INTENSE }

    public static final int SAMPLE_RATE = 22050;
    private static final int BARS = 8;
    private static final int BEATS_PER_BAR = 4;

    private MusicSynth() {
    }

    /** Mood for a stored setting: 1 = soothing, 2 = focus, 3 = intense, anything else = no music (null). */
    public static Mood fromSetting(int setting) {
        return setting >= 1 && setting <= Mood.values().length ? Mood.values()[setting - 1] : null;
    }

    /** Mono 16-bit samples of one loop. */
    public static short[] render(Mood mood) {
        double bpm;
        int[][] chords;   // per bar: root MIDI note followed by the chord intervals
        switch (mood) {
        case SOOTHING:
            bpm = 58;
            chords = new int[][] {
                {48, 0, 4, 7, 11}, {48, 0, 4, 7, 11}, {45, 0, 3, 7, 10}, {45, 0, 3, 7, 10},
                {41, 0, 4, 7, 11}, {41, 0, 4, 7, 11}, {43, 0, 4, 7, 9}, {43, 0, 4, 7, 9}};
            break;
        case FOCUS:
            bpm = 84;
            chords = new int[][] {
                {45, 0, 3, 7, 10}, {41, 0, 4, 7, 11}, {48, 0, 4, 7, 11}, {43, 0, 4, 7, 9},
                {45, 0, 3, 7, 10}, {41, 0, 4, 7, 11}, {48, 0, 4, 7, 11}, {43, 0, 4, 7, 9}};
            break;
        default:
            bpm = 132;
            chords = new int[][] {
                {45, 0, 3, 7, 12}, {41, 0, 4, 7, 12}, {48, 0, 4, 7, 12}, {40, 0, 4, 7, 10},
                {45, 0, 3, 7, 12}, {41, 0, 4, 7, 12}, {43, 0, 4, 7, 12}, {40, 0, 4, 7, 10}};
            break;
        }

        double beatSeconds = 60.0 / bpm;
        double barSeconds = beatSeconds * BEATS_PER_BAR;
        int total = (int) Math.round(barSeconds * BARS * SAMPLE_RATE);
        float[] mix = new float[total];
        Random noise = new Random(7);

        for (int bar = 0; bar < BARS; bar++) {
            int[] chord = chords[bar];
            double barStart = bar * barSeconds;

            // pad: the chord held for the whole bar, fading in and out inside the bar
            double padLevel = mood == MusicSynth.Mood.INTENSE ? 0.10 : mood == MusicSynth.Mood.FOCUS ? 0.12 : 0.18;
            for (int tone = 1; tone < chord.length; tone++) {
                addTone(mix, barStart, barSeconds, midiToHz(chord[0] + 12 + chord[tone]), padLevel / 3,
                        Wave.SINE, Envelope.PAD);
            }

            // bass
            if (mood == MusicSynth.Mood.FOCUS) {
                for (int beat = 0; beat < BEATS_PER_BAR; beat += 2) {
                    addTone(mix, barStart + beat * beatSeconds, beatSeconds * 1.8, midiToHz(chord[0] - 12), 0.16,
                            Wave.TRIANGLE, Envelope.PLUCK);
                }
            } else if (mood == MusicSynth.Mood.INTENSE) {
                for (int step = 0; step < BEATS_PER_BAR * 2; step++) {
                    int note = chord[0] - 12 + (step % 4 == 3 ? 12 : 0);
                    addTone(mix, barStart + step * beatSeconds / 2, beatSeconds / 2 * 0.9, midiToHz(note), 0.20,
                            Wave.SAW, Envelope.PLUCK);
                }
            }

            // arpeggio / melody
            int notesPerBeat = mood == MusicSynth.Mood.INTENSE ? 4 : mood == MusicSynth.Mood.FOCUS ? 2 : 1;
            int[] pattern = mood == MusicSynth.Mood.SOOTHING ? new int[] {1, 3, 2, 4} : new int[] {1, 2, 3, 4, 3, 2};
            int steps = BEATS_PER_BAR * notesPerBeat;
            double stepSeconds = beatSeconds / notesPerBeat;
            for (int step = 0; step < steps; step++) {
                int interval = chord[pattern[step % pattern.length]];
                int octave = mood == MusicSynth.Mood.SOOTHING ? 24 : 24 + (step % 7 == 6 ? 12 : 0);
                double level = mood == MusicSynth.Mood.INTENSE ? 0.11 : mood == MusicSynth.Mood.FOCUS ? 0.13 : 0.15;
                addTone(mix, barStart + step * stepSeconds, stepSeconds * (mood == MusicSynth.Mood.SOOTHING ? 3.2 : 1.6),
                        midiToHz(chord[0] + octave + interval), level,
                        mood == MusicSynth.Mood.INTENSE ? Wave.TRIANGLE : Wave.SINE, Envelope.PLUCK_CAPPED,
                        barStart + barSeconds);
            }

            // percussion
            if (mood != MusicSynth.Mood.SOOTHING) {
                for (int beat = 0; beat < BEATS_PER_BAR; beat++) {
                    if (mood == MusicSynth.Mood.INTENSE) {
                        addKick(mix, barStart + beat * beatSeconds, 0.34);
                    }
                    int hats = mood == MusicSynth.Mood.INTENSE ? 2 : 1;
                    for (int hat = 0; hat < hats; hat++) {
                        addHat(mix, barStart + (beat + (hats == 1 ? 0.5 : hat * 0.5 + 0.25)) * beatSeconds, 0.05, noise);
                    }
                }
            }
        }

        return toPcm(mix);
    }

    private enum Wave { SINE, TRIANGLE, SAW }

    private enum Envelope { PAD, PLUCK, PLUCK_CAPPED }

    private static double midiToHz(int note) {
        return 440.0 * Math.pow(2.0, (note - 69) / 12.0);
    }

    private static void addTone(float[] mix, double start, double length, double hz, double level, Wave wave, Envelope envelope) {
        addTone(mix, start, length, hz, level, wave, envelope, Double.MAX_VALUE);
    }

    /** @param notAfter the tone is faded out so that it is silent by this time (the end of its bar) */
    private static void addTone(float[] mix, double start, double length, double hz, double level, Wave wave,
                                Envelope envelope, double notAfter) {
        double end = Math.min(start + length, notAfter - 0.005);
        int first = (int) (start * SAMPLE_RATE);
        int last = Math.min(mix.length, (int) (end * SAMPLE_RATE));
        double duration = Math.max(1e-3, end - start);
        for (int index = first; index < last; index++) {
            double time = (index - first) / (double) SAMPLE_RATE;
            double phase = hz * time;
            double sample;
            switch (wave) {
            case TRIANGLE:
                sample = 4 * Math.abs(phase - Math.floor(phase + 0.5)) - 1;
                break;
            case SAW:
                sample = (2 * (phase - Math.floor(phase + 0.5))) * 0.6
                        + 0.4 * Math.sin(2 * Math.PI * phase);
                break;
            default:
                sample = Math.sin(2 * Math.PI * phase);
                break;
            }
            double gain;
            if (envelope == Envelope.PAD) {
                double fade = Math.min(1.0, Math.min(time / (duration * 0.35), (duration - time) / (duration * 0.35)));
                gain = fade * fade * (3 - 2 * fade);   // smooth in and out, silent at both ends of the bar
            } else {
                double attack = Math.min(1.0, time / 0.01);
                double release = Math.min(1.0, (duration - time) / 0.03);
                gain = attack * Math.max(0, release) * Math.exp(-3.2 * time / duration);
            }
            mix[index] += (float) (sample * level * gain);
        }
    }

    private static void addKick(float[] mix, double start, double level) {
        int first = (int) (start * SAMPLE_RATE);
        int count = (int) (0.16 * SAMPLE_RATE);
        for (int offset = 0; offset < count && first + offset < mix.length; offset++) {
            double time = offset / (double) SAMPLE_RATE;
            double hz = 45 + 90 * Math.exp(-time * 28);
            mix[first + offset] += (float) (Math.sin(2 * Math.PI * hz * time) * Math.exp(-time * 22) * level);
        }
    }

    private static void addHat(float[] mix, double start, double level, Random noise) {
        int first = (int) (start * SAMPLE_RATE);
        int count = (int) (0.035 * SAMPLE_RATE);
        float previous = 0;
        for (int offset = 0; offset < count && first + offset < mix.length; offset++) {
            float white = noise.nextFloat() * 2 - 1;
            float highPassed = white - previous;   // crude high-pass for a hissy hat
            previous = white;
            double time = offset / (double) SAMPLE_RATE;
            mix[first + offset] += (float) (highPassed * Math.exp(-time * 120) * level);
        }
    }

    /** Scale to a comfortable peak and fade the first and last few milliseconds so the loop never clicks. */
    private static short[] toPcm(float[] mix) {
        float peak = 1e-6f;
        for (float sample : mix) {
            peak = Math.max(peak, Math.abs(sample));
        }
        float scale = 0.8f * Short.MAX_VALUE / peak;
        int edge = SAMPLE_RATE / 50;   // 20 ms
        short[] pcm = new short[mix.length];
        for (int index = 0; index < mix.length; index++) {
            float fade = Math.min(1f, Math.min(index, mix.length - 1 - index) / (float) edge);
            pcm[index] = (short) Math.round(mix[index] * scale * fade);
        }
        return pcm;
    }
}
