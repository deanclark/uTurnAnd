package com.crystalwink.uturn;

/**
 * Text form of a game in progress so it can survive the app being minimised or killed: the level, the board size and
 * for every tile which grid cell it is in and the colours of its four edges (top, right, bottom, left).
 */
public final class GameState {
    public final int level;
    public final int columns;
    public final int rows;
    public final int[] cellOfTile;
    public final int[][] colours;

    private GameState(int level, int columns, int rows, int[] cellOfTile, int[][] colours) {
        this.level = level;
        this.columns = columns;
        this.rows = rows;
        this.cellOfTile = cellOfTile;
        this.colours = colours;
    }

    public static String encode(int level, int columns, int rows, int[] cellOfTile, int[][] colours) {
        StringBuilder text = new StringBuilder("v1;").append(level).append(';').append(columns).append(';').append(rows);
        for (int tile = 0; tile < cellOfTile.length; tile++) {
            text.append(tile == 0 ? ';' : '|').append(cellOfTile[tile]);
            for (int colour : colours[tile]) {
                text.append(',').append(colour);
            }
        }
        return text.toString();
    }

    /** @return the state, or null when the text is missing, malformed or inconsistent. */
    public static GameState decode(String text) {
        if (text == null) {
            return null;
        }
        try {
            String[] parts = text.split(";", -1);
            if (parts.length != 5 || !parts[0].equals("v1")) {
                return null;
            }
            int level = Integer.parseInt(parts[1]);
            int columns = Integer.parseInt(parts[2]);
            int rows = Integer.parseInt(parts[3]);
            String[] tiles = parts[4].split("\\|", -1);
            if (level < 1 || columns < 1 || rows < 1 || tiles.length != columns * rows) {
                return null;
            }

            int[] cellOfTile = new int[tiles.length];
            int[][] colours = new int[tiles.length][4];
            boolean[] cellUsed = new boolean[tiles.length];
            for (int tile = 0; tile < tiles.length; tile++) {
                String[] fields = tiles[tile].split(",", -1);
                if (fields.length != 5) {
                    return null;
                }
                int cell = Integer.parseInt(fields[0]);
                if (cell < 0 || cell >= tiles.length || cellUsed[cell]) {
                    return null;
                }
                cellUsed[cell] = true;
                cellOfTile[tile] = cell;
                for (int side = 0; side < 4; side++) {
                    colours[tile][side] = Integer.parseInt(fields[side + 1]);
                    if (colours[tile][side] < 0) {
                        return null;
                    }
                }
            }
            return new GameState(level, columns, rows, cellOfTile, colours);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
