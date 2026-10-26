package com.crystalwink.uturn;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class LevelAndStateTest {

    @Test
    public void firstSixLevelsKeepTheirOriginalSizes() {
        int[][] expected = {{3, 3, 5}, {3, 4, 5}, {4, 4, 4}, {4, 5, 4}, {5, 5, 4}, {5, 6, 4}};
        for (int level = 1; level <= expected.length; level++) {
            LevelRules rules = LevelRules.forLevel(level);
            assertEquals(expected[level - 1][0], rules.columns);
            assertEquals(expected[level - 1][1], rules.rows);
            assertEquals(expected[level - 1][2], rules.colours);
        }
    }

    @Test
    public void levelsAreUnlimitedAndNeverExceedTheTileArray() {
        int previousTiles = 0;
        for (int level = 1; level <= 500; level++) {
            LevelRules rules = LevelRules.forLevel(level);
            int tiles = rules.columns * rules.rows;
            assertTrue(tiles <= LevelRules.MAX_TILES);
            assertTrue(tiles >= previousTiles);
            assertTrue(rules.colours >= 3 && rules.colours <= 5);
            previousTiles = tiles;
        }
        assertEquals(LevelRules.MAX_TILES, LevelRules.forLevel(500).columns * LevelRules.forLevel(500).rows);
    }

    @Test
    public void demoSolverSolvesEveryLevelUpToFifty() {
        for (int level = 1; level <= 50; level++) {
            LevelRules rules = LevelRules.forLevel(level);
            int[][] tiles = BoardRules.createSolvedBoard(rules.rows, rules.columns, BoardRules.OUTSIDE_EDGE, rules.colours);
            for (int tile = 0; tile < tiles.length; tile++) {
                tiles[tile] = DemoSolver.rotate(tiles[tile], (tile * 7 + level) % 4);
            }
            List<DemoSolver.Placement> plan = DemoSolver.solve(tiles, rules.rows, rules.columns, BoardRules.OUTSIDE_EDGE);
            assertNotNull("level " + level, plan);
        }
    }

    @Test
    public void gameStateSurvivesAnEncodeDecodeRoundTrip() {
        int[] cells = {2, 0, 1, 3};
        int[][] colours = {{0, 1, 2, 0}, {3, 4, 0, 0}, {1, 1, 1, 1}, {5, 4, 3, 2}};

        GameState state = GameState.decode(GameState.encode(7, 2, 2, cells, colours));

        assertNotNull(state);
        assertEquals(7, state.level);
        assertEquals(2, state.columns);
        assertEquals(2, state.rows);
        assertArrayEquals(cells, state.cellOfTile);
        for (int tile = 0; tile < colours.length; tile++) {
            assertArrayEquals(colours[tile], state.colours[tile]);
        }
    }

    @Test
    public void damagedOrInconsistentStateIsRejected() {
        assertNull(GameState.decode(null));
        assertNull(GameState.decode(""));
        assertNull(GameState.decode("v2;1;2;2;0,1,2,3|1,1,2,3|2,1,2,3|3,1,2,3"));
        assertNull(GameState.decode("v1;1;2;2;0,1,2,3|1,1,2,3|2,1,2,3"));          // a tile missing
        assertNull(GameState.decode("v1;1;2;2;0,1,2,3|0,1,2,3|2,1,2,3|3,1,2,3"));  // two tiles in one cell
        assertNull(GameState.decode("v1;1;2;2;0,1,2,3|1,1,2,3|2,1,2,3|9,1,2,3"));  // cell off the board
        assertNull(GameState.decode("v1;1;2;2;0,1,2,3|1,1,2,x|2,1,2,3|3,1,2,3"));
    }
}
