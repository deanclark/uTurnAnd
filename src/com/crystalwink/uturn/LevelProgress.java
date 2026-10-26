package com.crystalwink.uturn;

import java.util.Arrays;

/**
 * Best star rating (0 = not completed, 1..3) for every level the player has reached, and how stars are earned.
 * Stored as a comma separated list, index 0 being level 1.
 */
public final class LevelProgress {
    public static final int MAX_STARS = 3;

    private static final int MAX_LEVELS = 10000;   // sanity bound for decoding

    private int[] best = new int[0];

    /** 3 stars for a clean solve, 2 for one or two hints, 1 for more hints or for watching the demo. */
    public static int starsFor(int hintsUsed, boolean demoUsed) {
        if (demoUsed || hintsUsed >= 3) {
            return 1;
        }
        return hintsUsed == 0 ? MAX_STARS : 2;
    }

    /** Record a finished level; only improvements are kept. @return true when the stored rating improved */
    public boolean record(int level, int stars) {
        if (level < 1 || level > MAX_LEVELS) {
            return false;
        }
        int clamped = Math.max(0, Math.min(MAX_STARS, stars));
        if (level > best.length) {
            best = Arrays.copyOf(best, level);
        }
        if (clamped > best[level - 1]) {
            best[level - 1] = clamped;
            return true;
        }
        return false;
    }

    public int stars(int level) {
        return level >= 1 && level <= best.length ? best[level - 1] : 0;
    }

    public int totalStars() {
        int total = 0;
        for (int stars : best) {
            total += stars;
        }
        return total;
    }

    /** Highest level that may be played: the one after the last completed, or the level in progress if further on. */
    public int highestUnlocked(int currentLevel) {
        int completed = 0;
        for (int level = best.length; level >= 1; level--) {
            if (best[level - 1] > 0) {
                completed = level;
                break;
            }
        }
        return Math.max(Math.max(1, currentLevel), completed + 1);
    }

    public String encode() {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < best.length; index++) {
            if (index > 0) {
                text.append(',');
            }
            text.append(best[index]);
        }
        return text.toString();
    }

    /** Tolerant of null or damaged text: unreadable entries count as 0. */
    public static LevelProgress decode(String text) {
        LevelProgress progress = new LevelProgress();
        if (text == null || text.isEmpty()) {
            return progress;
        }
        String[] parts = text.split(",", -1);
        int length = Math.min(parts.length, MAX_LEVELS);
        progress.best = new int[length];
        for (int index = 0; index < length; index++) {
            try {
                progress.best[index] = Math.max(0, Math.min(MAX_STARS, Integer.parseInt(parts[index].trim())));
            } catch (NumberFormatException e) {
                progress.best[index] = 0;
            }
        }
        return progress;
    }
}
