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

                if (column + 1 < columns && segments[RIGHT] != cells[cellIndex + 1][LEFT]) {
                    return false;
                }
                if (row + 1 < rows && segments[BOTTOM] != cells[cellIndex + columns][TOP]) {
                    return false;
                }
            }
        }

        return true;
    }
}