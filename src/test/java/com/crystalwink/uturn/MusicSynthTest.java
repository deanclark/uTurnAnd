package com.crystalwink.uturn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MusicSynthTest {

    private static double rms(short[] pcm) {
        double sum = 0;
        for (short sample : pcm) {
            sum += (double) sample * sample;
        }
        return Math.sqrt(sum / pcm.length) / Short.MAX_VALUE;
    }

    /** Note onsets per second: windows that are clearly louder than the one before. Busier music scores higher. */
    private static double onsetsPerSecond(short[] pcm) {
        int window = 512;
        int onsets = 0;
        double previous = 0;
        for (int start = 0; start + window <= pcm.length; start += window) {
            double energy = 0;
            for (int index = start; index < start + window; index++) {
                energy += (double) pcm[index] * pcm[index];
            }
            energy = Math.sqrt(energy / window) / Short.MAX_VALUE;
            if (energy > 0.02 && energy > previous * 1.4) {
                onsets++;
            }
            previous = energy;
        }
        return onsets / (pcm.length / (double) MusicSynth.SAMPLE_RATE);
    }

    @Test
    public void everyMoodRendersAudibleUnclippedMusicOfSensibleLength() {
        for (MusicSynth.Mood mood : MusicSynth.Mood.values()) {
            short[] pcm = MusicSynth.render(mood);
            double seconds = pcm.length / (double) MusicSynth.SAMPLE_RATE;
            assertTrue(mood + " length " + seconds, seconds >= 12 && seconds <= 40);

            int peak = 0;
            for (short sample : pcm) {
                peak = Math.max(peak, Math.abs(sample));
            }
            assertTrue(mood + " peak " + peak, peak > 20000 && peak < Short.MAX_VALUE);
            assertTrue(mood + " rms", rms(pcm) > 0.03);
        }
    }

    @Test
    public void loopsStartAndEndSilentSoThereIsNoClickAtTheSeam() {
        for (MusicSynth.Mood mood : MusicSynth.Mood.values()) {
            short[] pcm = MusicSynth.render(mood);
            assertEquals(0, pcm[0]);
            assertTrue(Math.abs(pcm[pcm.length - 1]) <= 8);
        }
    }

    @Test
    public void moodsAreAudiblyDifferentAndIntenseIsBusiestSoothingCalmest() {
        double soothing = onsetsPerSecond(MusicSynth.render(MusicSynth.Mood.SOOTHING));
        double focus = onsetsPerSecond(MusicSynth.render(MusicSynth.Mood.FOCUS));
        double intense = onsetsPerSecond(MusicSynth.render(MusicSynth.Mood.INTENSE));

        assertTrue(soothing < focus);
        assertTrue(focus < intense);
    }

    @Test
    public void renderingIsRepeatable() {
        short[] first = MusicSynth.render(MusicSynth.Mood.FOCUS);
        short[] second = MusicSynth.render(MusicSynth.Mood.FOCUS);
        assertEquals(first.length, second.length);
        for (int index = 0; index < first.length; index += 97) {
            assertEquals(first[index], second[index]);
        }
    }

    @Test
    public void settingsMapToMoods() {
        assertNull(MusicSynth.fromSetting(0));
        assertNull(MusicSynth.fromSetting(-1));
        assertNull(MusicSynth.fromSetting(4));
        assertEquals(MusicSynth.Mood.SOOTHING, MusicSynth.fromSetting(1));
        assertEquals(MusicSynth.Mood.FOCUS, MusicSynth.fromSetting(2));
        assertNotNull(MusicSynth.fromSetting(3));
    }
}
