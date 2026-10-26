package com.crystalwink.uturn;

public final class BoardRules {
    /** Palette index that marks a tile edge lying on the outside of the board. */
    public static final int OUTSIDE_EDGE = 0;

    private static final int TOP = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int LEFT = 3;

    private BoardRules() {
    }

    public static int[][] createSolvedBoard(int rows, int columns, int outsideEdge, int colorCount) {
        if (rows <= 0 || columns <= 0 || colorCount <= 0) {
            throw new IllegalArgumentException("Board dimensions and color count must be positive");
        }

        int[][] cells = new int[rows * columns][4];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int[] segments = cells[row * columns + column];
                segments[TOP] = row == 0 ? outsideEdge : horizontalEdgeColor(row - 1, column, columns, colorCount);
                segments[RIGHT] = column == columns - 1
                        ? outsideEdge
                        : verticalEdgeColor(row, column, rows, colorCount);
                segments[BOTTOM] = row == rows - 1
                        ? outsideEdge
                        : horizontalEdgeColor(row, column, columns, colorCount);
                segments[LEFT] = column == 0
                        ? outsideEdge
                        : verticalEdgeColor(row, column - 1, rows, colorCount);
            }
        }
        return cells;
    }

    /**
     * A solved board whose interior edges get random colours instead of the repeating pattern of
     * {@link #createSolvedBoard}.
     *
     * @param avoidDuplicates when true the board is regenerated (up to a few hundred times) to avoid two tiles being
     *                        the same tile turned; if that is impossible for the board size and colour count the
     *                        board with the fewest duplicates is returned
     */
    public static int[][] createRandomSolvedBoard(int rows, int columns, int outsideEdge, int colorCount, long seed,
                                                  boolean avoidDuplicates) {
        if (rows <= 0 || columns <= 0 || colorCount <= 0) {
            throw new IllegalArgumentException("Board dimensions and color count must be positive");
        }
        int[][] best = null;
        int bestDuplicates = Integer.MAX_VALUE;
        int attempts = avoidDuplicates ? 500 : 1;
        for (int attempt = 0; attempt < attempts; attempt++) {
            int[][] candidate = randomBoard(rows, columns, outsideEdge, colorCount, new java.util.Random(seed + attempt));
            int duplicates = avoidDuplicates ? countDuplicateTiles(candidate) : 0;
            if (duplicates < bestDuplicates) {
                best = candidate;
                bestDuplicates = duplicates;
            }
            if (bestDuplicates == 0) {
                break;
            }
        }
        return best;
    }

    private static int[][] randomBoard(int rows, int columns, int outsideEdge, int colorCount, java.util.Random random) {
        int[][] horizontal = new int[Math.max(rows - 1, 0)][columns];   // edge below cell [row][column]
        int[][] vertical = new int[rows][Math.max(columns - 1, 0)];     // edge right of cell [row][column]
        for (int[] line : horizontal) {
            for (int index = 0; index < line.length; index++) {
                line[index] = 1 + random.nextInt(colorCount);
            }
        }
        for (int[] line : vertical) {
            for (int index = 0; index < line.length; index++) {
                line[index] = 1 + random.nextInt(colorCount);
            }
        }

        int[][] cells = new int[rows * columns][4];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int[] segments = cells[row * columns + column];
                segments[TOP] = row == 0 ? outsideEdge : horizontal[row - 1][column];
                segments[RIGHT] = column == columns - 1 ? outsideEdge : vertical[row][column];
                segments[BOTTOM] = row == rows - 1 ? outsideEdge : horizontal[row][column];
                segments[LEFT] = column == 0 ? outsideEdge : vertical[row][column - 1];
            }
        }
        return cells;
    }

    /** Number of tiles that are the same as an earlier tile, allowing for the earlier tile being turned. */
    public static int countDuplicateTiles(int[][] cells) {
        int duplicates = 0;
        for (int tile = 1; tile < cells.length; tile++) {
            for (int earlier = 0; earlier < tile; earlier++) {
                if (DemoSolver.turnsToReach(cells[earlier], cells[tile]) >= 0) {
                    duplicates++;
                    break;
                }
            }
        }
        return duplicates;
    }

    private static int horizontalEdgeColor(int row, int column, int columns, int colorCount) {
        return 1 + (row * columns + column) % colorCount;
    }

    private static int verticalEdgeColor(int row, int column, int rows, int colorCount) {
        return 1 + (row + column * rows) % colorCount;
    }

    public static boolean isSolved(int[][] cells, int rows, int columns, int outsideEdge) {
        if (cells == null || rows <= 0 || columns <= 0 || cells.length != rows * columns) {
            return false;
        }

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int cellIndex = row * columns + column;
                int[] segments = cells[cellIndex];
                if (segments == null || segments.length < 4) {
                    return false;
                }

                if ((segments[TOP] == outsideEdge) != (row == 0)
                        || (segments[RIGHT] == outsideEdge) != (column == columns - 1)
                        || (segments[BOTTOM] == outsideEdge) != (row == rows - 1)
                        || (segments[LEFT] == outsideEdge) != (column == 0)) {
                    return false;
                }

                if (column + 1 < columns && (cells[cellIndex + 1] == null || segments[RIGHT] != cells[cellIndex + 1][LEFT])) {
                    return false;
                }
                if (row + 1 < rows && (cells[cellIndex + columns] == null || segments[BOTTOM] != cells[cellIndex + columns][TOP])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * True when every border cell has its outside edges on the border (and nowhere else) and neighbouring border
     * cells match. Inner cells are not looked at, so this can be checked before any inner tile is placed.
     * A cell that is null counts as not yet placed and makes the border invalid.
     */
    public static boolean isBorderValid(int[][] cells, int rows, int columns, int outsideEdge) {
        if (cells == null || rows <= 0 || columns <= 0 || cells.length != rows * columns) {
            return false;
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (row != 0 && row != rows - 1 && column != 0 && column != columns - 1) {
                    continue;   // inner cell
                }
                int cellIndex = row * columns + column;
                int[] segments = cells[cellIndex];
                if (segments == null || segments.length < 4
                        || (segments[TOP] == outsideEdge) != (row == 0)
                        || (segments[RIGHT] == outsideEdge) != (column == columns - 1)
                        || (segments[BOTTOM] == outsideEdge) != (row == rows - 1)
                        || (segments[LEFT] == outsideEdge) != (column == 0)) {
                    return false;
                }
                boolean rightIsBorder = column + 1 < columns && (row == 0 || row == rows - 1);
                if (rightIsBorder && (cells[cellIndex + 1] == null || segments[RIGHT] != cells[cellIndex + 1][LEFT])) {
                    return false;
                }
                boolean belowIsBorder = row + 1 < rows && (column == 0 || column == columns - 1);
                if (belowIsBorder && (cells[cellIndex + columns] == null || segments[BOTTOM] != cells[cellIndex + columns][TOP])) {
                    return false;
                }
            }
        }
        return true;
    }
}
