package com.crystalwink.uturn;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo / redo stacks of the player's moves. A move is either swapping two tiles or turning one tile.
 * The history only stays correct when moves are undone and redone in strict order, so it must be cleared whenever
 * tiles are moved some other way (new level, demo mode).
 */
final class MoveHistory {
    enum Type { SWAP, ROTATE }

    static final class Move {
        final Type type;
        final int tile;
        final int otherTile;   // swap only
        final int turns;       // rotate only: clockwise quarter turns, negative = anticlockwise

        private Move(Type type, int tile, int otherTile, int turns) {
            this.type = type;
            this.tile = tile;
            this.otherTile = otherTile;
            this.turns = turns;
        }

        static Move swap(int tile, int otherTile) {
            return new Move(Type.SWAP, tile, otherTile, 0);
        }

        static Move rotate(int tile, int turns) {
            return new Move(Type.ROTATE, tile, -1, turns);
        }

        /** The move that cancels this one. */
        Move inverse() {
            return type == Type.SWAP ? this : rotate(tile, -turns);
        }
    }

    private final Deque<Move> undoStack = new ArrayDeque<>();
    private final Deque<Move> redoStack = new ArrayDeque<>();

    /** Record a move the player has just made; anything that could be redone is forgotten. */
    void record(Move move) {
        undoStack.push(move);
        redoStack.clear();
    }

    boolean canUndo() {
        return !undoStack.isEmpty();
    }

    boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** @return the move to apply (already inverted) to undo the last move, or null when there is nothing to undo */
    Move undo() {
        if (undoStack.isEmpty()) {
            return null;
        }
        Move move = undoStack.pop();
        redoStack.push(move);
        return move.inverse();
    }

    /** @return the move to apply again, or null when there is nothing to redo */
    Move redo() {
        if (redoStack.isEmpty()) {
            return null;
        }
        Move move = redoStack.pop();
        undoStack.push(move);
        return move;
    }

    void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    int size() {
        return undoStack.size();
    }
}
