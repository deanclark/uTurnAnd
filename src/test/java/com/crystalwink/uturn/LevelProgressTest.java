package com.crystalwink.uturn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LevelProgressTest {
    @Test
    public void starsDependOnHintsAndDemo() {
        assertEquals(3, LevelProgress.starsFor(0, false));
        assertEquals(2, LevelProgress.starsFor(1, false));
        assertEquals(2, LevelProgress.starsFor(2, false));
        assertEquals(1, LevelProgress.starsFor(3, false));
        assertEquals(1, LevelProgress.starsFor(0, true));
    }

    @Test
    public void onlyImprovementsAreKept() {
        LevelProgress progress = new LevelProgress();
        assertTrue(progress.record(3, 2));
        assertFalse(progress.record(3, 1));
        assertTrue(progress.record(3, 3));
        assertEquals(3, progress.stars(3));
        assertEquals(0, progress.stars(1));
        assertEquals(0, progress.stars(99));
        assertEquals(3, progress.totalStars());
    }

    @Test
    public void unlockingFollowsTheLastCompletedLevelOrTheCurrentOne() {
        LevelProgress progress = new LevelProgress();
        assertEquals(1, progress.highestUnlocked(1));
        progress.record(2, 1);
        assertEquals(3, progress.highestUnlocked(1));
        assertEquals(7, progress.highestUnlocked(7));   // saved before stars existed
    }

    @Test
    public void encodeDecodeRoundTripsAndToleratesDamage() {
        LevelProgress progress = new LevelProgress();
        progress.record(1, 3);
        progress.record(3, 2);
        LevelProgress copy = LevelProgress.decode(progress.encode());
        assertEquals("3,0,2", copy.encode());

        LevelProgress damaged = LevelProgress.decode("3,x,9,-4,,2");
        assertEquals(3, damaged.stars(1));
        assertEquals(0, damaged.stars(2));
        assertEquals(3, damaged.stars(3));
        assertEquals(0, damaged.stars(4));
        assertEquals(2, damaged.stars(6));
        assertEquals(0, LevelProgress.decode(null).totalStars());
    }
}
