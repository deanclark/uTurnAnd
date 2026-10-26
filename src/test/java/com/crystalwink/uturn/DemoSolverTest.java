package com.crystalwink.uturn;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class DemoSolverTest {
    private static final int OUTSIDE = BoardRules.OUTSIDE_EDGE;

    /** The tiles of a solved board, each turned randomly and shuffled, as the game scrambles them. */
    private static int[][] scrambled(int rows, int columns, int colours, long seed) {
        int[][] solved = BoardRules.createSolvedBoard(rows, columns, OUTSIDE, colours);
        List<int[]> tiles = new ArrayList<>();
        Random random = new Random(seed);
        for (int[] tile : solved) {
            tiles.add(DemoSolver.rotate(tile, random.nextInt(4)));
        }
        Collections.shuffle(tiles, random);
        return tiles.toArray(new int[0][]);
    }

    @Test
    public void solvesEveryLevelSizeToAValidBoard() {
        int[][] sizes = {{3, 3, 5}, {4, 3, 5}, {4, 4, 4}, {5, 4, 4}, {5, 5, 4}, {6, 5, 4}};
        for (int[] size : sizes) {
            for (long seed = 1; seed <= 5; seed++) {
                int[][] tiles = scrambled(size[0], size[1], size[2], seed);
                List<DemoSolver.Placement> plan = DemoSolver.solve(tiles, size[0], size[1], OUTSIDE);
                assertNotNull(size[0] + "x" + size[1], plan);

                int[][] board = new int[tiles.length][];
                for (DemoSolver.Placement placement : plan) {
                    assertTrue(DemoSolver.turnsToReach(tiles[placement.tile], placement.colours) >= 0);
                    board[placement.row * size[1] + placement.column] = placement.colours;
                }
                assertTrue(BoardRules.isSolved(board, size[0], size[1], OUTSIDE));
            }
        }
    }

    @Test
    public void placesCornersThenEdgesThenInnerTiles() {
        List<DemoSolver.Placement> plan = DemoSolver.solve(scrambled(4, 5, 4, 7), 4, 5, OUTSIDE);

        assertNotNull(plan);
        int lastKind = 0;
        int corners = 0;
        int edges = 0;
        for (DemoSolver.Placement placement : plan) {
            assertTrue(placement.kind.ordinal() >= lastKind);
            lastKind = placement.kind.ordinal();
            corners += placement.kind == DemoSolver.Kind.CORNER ? 1 : 0;
            edges += placement.kind == DemoSolver.Kind.EDGE ? 1 : 0;
        }
        assertEquals(4, corners);
        assertEquals(2 * (4 - 2) + 2 * (5 - 2), edges);
    }

    @Test
    public void cornerAndEdgeTilesOnlyGoToCornersAndEdgesWithOutsideEdgesAligned() {
        int rows = 5;
        int columns = 4;
        int[][] tiles = scrambled(rows, columns, 4, 3);
        for (DemoSolver.Placement placement : DemoSolver.solve(tiles, rows, columns, OUTSIDE)) {
            int outsideEdges = 0;
            for (int colour : tiles[placement.tile]) {
                outsideEdges += colour == OUTSIDE ? 1 : 0;
            }
            int expected = placement.kind == DemoSolver.Kind.CORNER ? 2
                    : placement.kind == DemoSolver.Kind.EDGE ? 1 : 0;
            assertEquals(expected, outsideEdges);
            assertEquals(placement.row == 0, placement.colours[0] == OUTSIDE);
            assertEquals(placement.row == rows - 1, placement.colours[2] == OUTSIDE);
            assertEquals(placement.column == 0, placement.colours[3] == OUTSIDE);
            assertEquals(placement.column == columns - 1, placement.colours[1] == OUTSIDE);
        }
    }

    @Test
    public void borderIsValidOnceCornersAndEdgesArePlacedBeforeAnyInnerTile() {
        int rows = 4;
        int columns = 4;
        int[][] tiles = scrambled(rows, columns, 4, 11);
        int[][] board = new int[rows * columns][];
        boolean reachedInner = false;
        for (DemoSolver.Placement placement : DemoSolver.solve(tiles, rows, columns, OUTSIDE)) {
            if (placement.kind == DemoSolver.Kind.INNER) {
                reachedInner = true;
                assertFalse(BoardRules.isSolved(board, rows, columns, OUTSIDE));
                assertTrue(BoardRules.isBorderValid(board, rows, columns, OUTSIDE));
                break;
            }
            board[placement.row * columns + placement.column] = placement.colours;
        }
        assertTrue(reachedInner);
    }

    @Test
    public void borderIsInvalidWhenAnEdgeTileIsMissingOrMismatched() {
        int[][] board = BoardRules.createSolvedBoard(3, 3, OUTSIDE, 5);
        assertTrue(BoardRules.isBorderValid(board, 3, 3, OUTSIDE));

        board[1][1] = board[1][1] == 4 ? 3 : 4;   // top edge tile no longer matches its neighbour
        assertFalse(BoardRules.isBorderValid(board, 3, 3, OUTSIDE));

        board = BoardRules.createSolvedBoard(3, 3, OUTSIDE, 5);
        board[3] = null;
        assertFalse(BoardRules.isBorderValid(board, 3, 3, OUTSIDE));
    }

    @Test
    public void rotatesClockwiseLikeTheGame() {
        assertArrayEquals(new int[] {4, 1, 2, 3}, DemoSolver.rotate(new int[] {1, 2, 3, 4}, 1));
    }
}
