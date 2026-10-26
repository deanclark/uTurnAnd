package com.crystalwink.uturn;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BoardRulesTest {
    @Test
    public void createsSolvedBoardsWithMatchingSharedEdges() {
        int[][] cells = BoardRules.createSolvedBoard(3, 4, 0, 5);

        assertTrue(BoardRules.isSolved(cells, 3, 4, 0));
    }

    @Test
    public void acceptsAllFourGlobalBoardOrientations() {
        int rows = 3;
        int columns = 3;
        int[][] cells = BoardRules.createSolvedBoard(rows, columns, 0, 5);

        for (int orientation = 0; orientation < 4; orientation++) {
            assertTrue(BoardRules.isSolved(cells, rows, columns, 0));
            int[][] rotatedCells = new int[rows * columns][4];
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    int source = row * columns + column;
                    int rotatedRow = column;
                    int rotatedColumn = rows - 1 - row;
                    int destination = rotatedRow * rows + rotatedColumn;
                    rotatedCells[destination][0] = cells[source][3];
                    rotatedCells[destination][1] = cells[source][0];
                    rotatedCells[destination][2] = cells[source][1];
                    rotatedCells[destination][3] = cells[source][2];
                }
            }
            cells = rotatedCells;
        }
    }

    @Test
    public void rejectsMismatchedNeighborEdges() {
        int[][] cells = BoardRules.createSolvedBoard(2, 2, 0, 3);
        cells[0][1] = 4;

        assertFalse(BoardRules.isSolved(cells, 2, 2, 0));
    }

    @Test
    public void rejectsOutsideMarkerOnInteriorEdge() {
        int[][] cells = BoardRules.createSolvedBoard(2, 2, 0, 3);
        cells[0][1] = 0;

        assertFalse(BoardRules.isSolved(cells, 2, 2, 0));
    }
}