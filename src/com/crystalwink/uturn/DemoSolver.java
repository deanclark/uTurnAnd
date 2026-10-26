package com.crystalwink.uturn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Works out how demo mode solves a board: corner tiles go to corners, edge tiles to edges and inner tiles inside, each
 * rotated so that outside edges face the border. The plan lists the corners first, then the edges, then the inner
 * tiles, so the outer border can be verified before any inner tile is moved.
 *
 * Tiles are int[4] edge colours (top, right, bottom, left); a clockwise turn shifts them one place to the right.
 */
public final class DemoSolver {
    public enum Kind { CORNER, EDGE, INNER }

    public static final class Placement {
        public final int row;
        public final int column;
        public final int tile;
        public final int[] colours;   // edge colours the tile must end up showing
        public final Kind kind;

        Placement(int row, int column, int tile, int[] colours, Kind kind) {
            this.row = row;
            this.column = column;
            this.tile = tile;
            this.colours = colours;
            this.kind = kind;
        }
    }

    private static final int MAX_NODES = 5_000_000;

    private final int[][] tiles;
    private final int rows;
    private final int columns;
    private final int outside;
    private final int[][] placedColours;
    private final int[] placedTile;
    private final boolean[] used;
    private final List<int[]> order = new ArrayList<>();   // {row, column} in search order
    private int nodes;
    private int[] preferredTile;   // per cell: the tile currently in it, or null when not preferring

    private DemoSolver(int[][] tiles, int rows, int columns, int outside) {
        this.tiles = tiles;
        this.rows = rows;
        this.columns = columns;
        this.outside = outside;
        this.placedColours = new int[rows * columns][];
        this.placedTile = new int[rows * columns];
        this.used = new boolean[tiles.length];
    }

    /** @return the plan, or null when the tiles cannot form a board. */
    public static List<Placement> solve(int[][] tiles, int rows, int columns, int outside) {
        return solve(tiles, rows, columns, outside, null);
    }

    /**
     * @param cellOfTile where each tile currently is (row * columns + column), or null. When given, the search tries
     *                   to keep each tile in the cell it is in and the way it is turned, so a partly solved board is
     *                   completed rather than re-solved from scratch.
     * @return the plan, or null when the tiles cannot form a board
     */
    public static List<Placement> solve(int[][] tiles, int rows, int columns, int outside, int[] cellOfTile) {
        if (rows < 2 || columns < 2 || tiles.length != rows * columns) {
            return null;
        }
        DemoSolver solver = new DemoSolver(tiles, rows, columns, outside);
        if (cellOfTile != null) {
            solver.preferredTile = new int[tiles.length];
            Arrays.fill(solver.preferredTile, -1);
            for (int tile = 0; tile < cellOfTile.length; tile++) {
                solver.preferredTile[cellOfTile[tile]] = tile;
            }
        }
        solver.buildOrder();
        if (!solver.search(0)) {
            return null;
        }

        List<Placement> plan = new ArrayList<>();
        for (int[] cell : solver.order) {
            int index = cell[0] * columns + cell[1];
            plan.add(new Placement(cell[0], cell[1], solver.placedTile[index], solver.placedColours[index],
                    kindOf(cell[0], cell[1], rows, columns)));
        }
        plan.sort(Comparator.comparingInt(placement -> placement.kind.ordinal()));   // stable: keeps ring order
        return plan;
    }

    public static Kind kindOf(int row, int column, int rows, int columns) {
        boolean verticalEdge = row == 0 || row == rows - 1;
        boolean horizontalEdge = column == 0 || column == columns - 1;
        if (verticalEdge && horizontalEdge) {
            return Kind.CORNER;
        }
        return verticalEdge || horizontalEdge ? Kind.EDGE : Kind.INNER;
    }

    /** Border cells first, walking round the perimeter, then the inner cells row by row. */
    private void buildOrder() {
        for (int column = 0; column < columns; column++) {
            order.add(new int[] {0, column});
        }
        for (int row = 1; row < rows; row++) {
            order.add(new int[] {row, columns - 1});
        }
        for (int column = columns - 2; column >= 0; column--) {
            order.add(new int[] {rows - 1, column});
        }
        for (int row = rows - 2; row >= 1; row--) {
            order.add(new int[] {row, 0});
        }
        for (int row = 1; row < rows - 1; row++) {
            for (int column = 1; column < columns - 1; column++) {
                order.add(new int[] {row, column});
            }
        }
    }

    private boolean search(int position) {
        if (position == order.size()) {
            return true;
        }
        if (++nodes > MAX_NODES) {
            return false;
        }

        int row = order.get(position)[0];
        int column = order.get(position)[1];
        int cell = row * columns + column;
        int preferred = preferredTile == null ? -1 : preferredTile[cell];
        for (int attempt = 0; attempt < tiles.length + 1; attempt++) {
            // the tile already in this cell is tried first, then the rest in order
            int tile = attempt == 0 ? preferred : attempt - 1;
            if (tile < 0 || used[tile] || (attempt > 0 && tile == preferred)
                    || (attempt > 0 && hasUnusedTwin(tile))) {
                continue;   // a look-alike earlier in the list would lead to exactly the same search
            }
            for (int turn = 0; turn < 4; turn++) {
                int[] candidate = rotate(tiles[tile], turn);
                if (fits(row, column, candidate)) {
                    used[tile] = true;
                    placedTile[cell] = tile;
                    placedColours[cell] = candidate;
                    if (search(position + 1)) {
                        return true;
                    }
                    placedColours[cell] = null;
                    used[tile] = false;
                }
            }
        }
        return false;
    }

    /** True when an earlier unused tile is the same as this one (possibly turned), so trying both is wasted effort. */
    private boolean hasUnusedTwin(int tile) {
        for (int other = 0; other < tile; other++) {
            if (!used[other] && turnsToReach(tiles[other], tiles[tile]) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** Outside edges exactly on the border sides, and every already placed neighbour matches. */
    private boolean fits(int row, int column, int[] c) {
        if ((c[0] == outside) != (row == 0)
                || (c[1] == outside) != (column == columns - 1)
                || (c[2] == outside) != (row == rows - 1)
                || (c[3] == outside) != (column == 0)) {
            return false;
        }
        int[] above = row > 0 ? placedColours[(row - 1) * columns + column] : null;
        int[] below = row < rows - 1 ? placedColours[(row + 1) * columns + column] : null;
        int[] left = column > 0 ? placedColours[row * columns + column - 1] : null;
        int[] right = column < columns - 1 ? placedColours[row * columns + column + 1] : null;
        return (above == null || above[2] == c[0])
                && (right == null || right[3] == c[1])
                && (below == null || below[0] == c[2])
                && (left == null || left[1] == c[3]);
    }

    /** Colours after the given number of clockwise quarter turns. */
    public static int[] rotate(int[] colours, int clockwiseTurns) {
        int[] result = new int[4];
        for (int side = 0; side < 4; side++) {
            result[(side + clockwiseTurns) % 4] = colours[side];
        }
        return result;
    }

    /** Number of clockwise turns that turn {@code from} into {@code to}, or -1 when it cannot be done. */
    public static int turnsToReach(int[] from, int[] to) {
        for (int turns = 0; turns < 4; turns++) {
            if (Arrays.equals(rotate(from, turns), to)) {
                return turns;
            }
        }
        return -1;
    }
}
