package com.crystalwink.uturn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class HintAndHistoryTest {
    private static final int OUTSIDE = BoardRules.OUTSIDE_EDGE;

    // ---- MoveHistory

    @Test
    public void undoReturnsTheInverseOfTheLastMoveAndRedoReplaysIt() {
        MoveHistory history = new MoveHistory();
        history.record(MoveHistory.Move.swap(1, 2));
        history.record(MoveHistory.Move.rotate(3, 1));

        MoveHistory.Move undone = history.undo();
        assertEquals(MoveHistory.Type.ROTATE, undone.type);
        assertEquals(3, undone.tile);
        assertEquals(-1, undone.turns);

        MoveHistory.Move redone = history.redo();
        assertEquals(1, redone.turns);

        MoveHistory.Move undoSwap = history.undo();
        assertEquals(-1, undoSwap.turns);
        history.undo();
        assertFalse(history.canUndo());
        assertNull(history.undo());
    }

    @Test
    public void aNewMoveForgetsWhatCouldHaveBeenRedone() {
        MoveHistory history = new MoveHistory();
        history.record(MoveHistory.Move.rotate(0, 1));
        history.undo();
        assertTrue(history.canRedo());

        history.record(MoveHistory.Move.swap(0, 1));

        assertFalse(history.canRedo());
        assertNull(history.redo());
        history.clear();
        assertFalse(history.canUndo());
    }

    // ---- HintFinder

    private static final class Board {
        final int rows;
        final int columns;
        final int[][] colours;   // by tile
        final int[] cellOfTile;

        Board(int rows, int columns, int colourCount, long seed, boolean scramble) {
            this.rows = rows;
            this.columns = columns;
            int[][] solved = BoardRules.createRandomSolvedBoard(rows, columns, OUTSIDE, colourCount, seed, true);
            colours = new int[solved.length][];
            cellOfTile = new int[solved.length];
            List<Integer> cells = new ArrayList<>();
            for (int cell = 0; cell < solved.length; cell++) {
                cells.add(cell);
            }
            Random random = new Random(seed);
            if (scramble) {
                Collections.shuffle(cells, random);
            }
            for (int tile = 0; tile < solved.length; tile++) {
                cellOfTile[tile] = cells.get(tile);
                colours[tile] = scramble ? DemoSolver.rotate(solved[tile], random.nextInt(4)) : solved[tile];
            }
        }

        int[][] byCell() {
            int[][] cells = new int[colours.length][];
            for (int tile = 0; tile < colours.length; tile++) {
                cells[cellOfTile[tile]] = colours[tile];
            }
            return cells;
        }

        boolean solved() {
            return BoardRules.isSolved(byCell(), rows, columns, OUTSIDE);
        }

        /** Do what the hint says: move the tile (swap with the occupant) and turn it. */
        void apply(HintFinder.Hint hint) {
            int moving = -1;
            int occupant = -1;
            for (int tile = 0; tile < colours.length; tile++) {
                if (cellOfTile[tile] == hint.fromCell) {
                    moving = tile;
                }
                if (cellOfTile[tile] == hint.toCell) {
                    occupant = tile;
                }
            }
            cellOfTile[moving] = hint.toCell;
            cellOfTile[occupant] = hint.fromCell;
            colours[moving] = DemoSolver.rotate(colours[moving], hint.turns);
        }
    }

    @Test
    public void aSolvedBoardNeedsNoHint() {
        Board board = new Board(4, 4, 6, 3, false);
        assertTrue(board.solved());
        assertNull(HintFinder.find(board.colours, board.cellOfTile, 4, 4, OUTSIDE));
    }

    @Test
    public void aSingleTurnedTileIsHintedAsTurnOnly() {
        Board board = new Board(3, 3, 7, 5, false);
        int tile = 4;
        int[] original = board.colours[tile];
        board.colours[tile] = DemoSolver.rotate(original, 1);   // centre tile turned a quarter clockwise

        HintFinder.Hint hint = HintFinder.find(board.colours, board.cellOfTile, 3, 3, OUTSIDE);

        assertNotNull(hint);
        assertTrue(hint.rotateOnly());
        assertEquals(4, hint.fromCell);
        // the turns in the hint put the tile back (3, or fewer if the tile's pattern repeats when turned)
        assertEquals(java.util.Arrays.toString(original),
                java.util.Arrays.toString(DemoSolver.rotate(board.colours[tile], hint.turns)));
    }

    @Test
    public void aSwappedPairIsHintedWithoutDisturbingTheRestOfTheBoard() {
        Board board = new Board(4, 4, 6, 9, false);
        int cellA = board.cellOfTile[5];
        int cellB = board.cellOfTile[10];
        board.cellOfTile[5] = cellB;
        board.cellOfTile[10] = cellA;

        HintFinder.Hint hint = HintFinder.find(board.colours, board.cellOfTile, 4, 4, OUTSIDE);

        assertNotNull(hint);
        assertTrue(hint.fromCell == cellA || hint.fromCell == cellB);
        assertTrue(hint.toCell == cellA || hint.toCell == cellB);
        board.apply(hint);
        assertTrue(board.solved());
    }

    @Test
    public void followingTheHintsSolvesAnyScrambledBoardWithinAReasonableNumberOfMoves() {
        int[][] sizes = {{3, 3, 7}, {4, 3, 7}, {4, 4, 6}, {5, 4, 6}, {5, 5, 5}, {6, 5, 5}, {7, 5, 4}, {7, 6, 4}, {8, 6, 4}};
        for (int[] size : sizes) {
            for (long seed = 1; seed <= 3; seed++) {
                Board board = new Board(size[0], size[1], size[2], seed, true);
                int moves = 0;
                HintFinder.Hint hint;
                while ((hint = HintFinder.find(board.colours, board.cellOfTile, size[0], size[1], OUTSIDE)) != null) {
                    board.apply(hint);
                    assertTrue(size[0] + "x" + size[1] + " seed " + seed + " not converging", ++moves <= size[0] * size[1] * 2);
                }
                assertTrue(size[0] + "x" + size[1] + " seed " + seed, board.solved());
            }
        }
    }
}
