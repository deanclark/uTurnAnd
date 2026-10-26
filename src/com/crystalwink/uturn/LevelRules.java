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

    /**
     * Interior colours for levels 1..9, easiest first: with many colours most edges are unique so each match is nearly
     * forced; as the board grows the colour count falls so look-alike edges (and difficulty) increase.
     */
    private static final int[] COLOURS = {7, 7, 6, 6, 5, 5, 4, 4, 4};
    private static final int FOUR_COLOUR_LEVELS = 14;   // levels 10..14 stay at 4, 15 and up use 3

    /** Highest colour count used, i.e. the number of interior colours the palette must provide. */
    public static final int MAX_COLOURS = 7;

    /**
     * How the solved board's interior edges are coloured. false = the original repeating pattern (many duplicate
     * tiles on big boards); true = random colours from a seed, see BoardRules.createRandomSolvedBoard.
     */
    public static final boolean RANDOM_EDGES = false;
    public static final boolean AVOID_DUPLICATE_TILES = true;

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
        int colours = safeLevel <= COLOURS.length ? COLOURS[safeLevel - 1] : safeLevel <= FOUR_COLOUR_LEVELS ? 4 : 3;
        return new LevelRules(safeLevel, size[0], size[1], colours);
    }
}
