package com.moneyfirst.pourperfect.game;

/** Outcome of a {@link GameSession#pour(int, int)} attempt. */
public enum PourResult {
    /** The pour was legal and applied; the level may or may not now be won. */
    POURED,
    /** The requested pour was not legal (see {@code Tube.canPourInto}). Nothing changed. */
    ILLEGAL_MOVE,
    /** The level is already won; no further pours are accepted. */
    ALREADY_WON
}
