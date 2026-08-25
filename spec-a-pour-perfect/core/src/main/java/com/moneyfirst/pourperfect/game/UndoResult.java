package com.moneyfirst.pourperfect.game;

/** Outcome of a {@link GameSession#undo()} attempt. */
public enum UndoResult {
    /** The last move was reverted. */
    UNDONE,
    /** There is no move to undo (fresh level, or already back at the start). */
    NOTHING_TO_UNDO,
    /** There is a move to undo, but the player has no undo credits remaining. */
    NO_UNDOS_REMAINING
}
