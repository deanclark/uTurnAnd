package com.crystalwink.uturn;

import java.util.Arrays;
import java.util.List;

/**
 * Works out the next useful move for a stuck player. The solution is searched with the player's existing placements
 * preferred, so a correct tile is never suggested to be moved away; the hint is the first tile (corners, then edges,
 * then inner tiles) that is not yet right.
 */
public final class HintFinder {
    public static final class Hint {
        public final int fromCell;   // cell the tile to move is in now
        public final int toCell;     // cell it should go to (same as fromCell when it only needs turning)
        public final int turns;      // clockwise quarter turns still needed once it is there (0..3)

        Hint(int fromCell, int toCell, int turns) {
            this.fromCell = fromCell;
            this.toCell = toCell;
            this.turns = turns;
        }

        public boolean rotateOnly() {
            return fromCell == toCell;
        }
    }

    private HintFinder() {
    }

    /**
     * @param colours    current edge colours of every tile, indexed by tile
     * @param cellOfTile the cell (row * columns + column) every tile is in
     * @return the next hint, or null when the board is solved or cannot be solved
     */
    public static Hint find(int[][] colours, int[] cellOfTile, int rows, int columns, int outsideEdge) {
        List<DemoSolver.Placement> plan = DemoSolver.solve(colours, rows, columns, outsideEdge, cellOfTile);
        if (plan == null) {
            return null;
        }

        int[] tileAtCell = new int[colours.length];
        for (int tile = 0; tile < cellOfTile.length; tile++) {
            tileAtCell[cellOfTile[tile]] = tile;
        }
        boolean[] cellCorrect = new boolean[colours.length];
        for (DemoSolver.Placement placement : plan) {
            int cell = placement.row * columns + placement.column;
            cellCorrect[cell] = Arrays.equals(colours[tileAtCell[cell]], placement.colours);
        }

        for (DemoSolver.Placement placement : plan) {
            int cell = placement.row * columns + placement.column;
            if (cellCorrect[cell]) {
                continue;
            }
            int tile = chooseTile(placement, colours, cellOfTile, cellCorrect);
            return new Hint(cellOfTile[tile], cell, DemoSolver.turnsToReach(colours[tile], placement.colours));
        }
        return null;
    }

    /** The planned tile, or a look-alike of it that is not already sitting in a correct cell. */
    private static int chooseTile(DemoSolver.Placement placement, int[][] colours, int[] cellOfTile, boolean[] cellCorrect) {
        if (!cellCorrect[cellOfTile[placement.tile]]) {
            return placement.tile;
        }
        for (int tile = 0; tile < colours.length; tile++) {
            if (!cellCorrect[cellOfTile[tile]] && DemoSolver.turnsToReach(colours[tile], placement.colours) >= 0) {
                return tile;
            }
        }
        return placement.tile;
    }
}
