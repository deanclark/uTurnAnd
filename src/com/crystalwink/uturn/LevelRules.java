package com.crystalwink.uturn;

/**
 * Board size and colour count for each level. Levels are unlimited: the board grows to {@link #MAX_COLUMNS} by
 * {@link #MAX_ROWS}, after which levels get harder by using fewer interior colours (more look-alike edges).
 */
public final class LevelRules {
    public static final int MAX_COLUMNS = 6;
    public static final int MAX_ROWS = 8;
    public static final int MAX_TILES = MAX_COLUMNS * MAX_ROWS;

    /** columns x rows for the levels that grow the board; later levels keep the last size. */
    private static final int[][] SIZES = {
        {3, 3}, {3, 4}, {4, 4}, {4, 5}, {5, 5}, {5, 6}, {5, 7}, {6, 7}, {6, 8}
    };

    private static final int FIVE_COLOUR_LEVELS = 2;
    private static final int FOUR_COLOUR_LEVELS = 14;

    public final int level;
    public final int columns;
    public final int rows;
    public final int colours;   // interior colours, not counting the outside edge

    private LevelRules(int level, int columns, int rows, int colours) {
        this.level = level;
        this.columns = columns;
        this.rows = rows;
        this.colours = colours;
    }

    public static LevelRules forLevel(int level) {
        int safeLevel = Math.max(1, level);
        int[] size = SIZES[Math.min(safeLevel, SIZES.length) - 1];
        int colours = safeLevel <= FIVE_COLOUR_LEVELS ? 5 : safeLevel <= FOUR_COLOUR_LEVELS ? 4 : 3;
        return new LevelRules(safeLevel, size[0], size[1], colours);
    }
}
