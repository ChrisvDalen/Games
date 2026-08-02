package com.keplersharvest.inventory;

/** What the equipped tool does when the player presses the tool key. */
public enum ToolKind {
    /** Turns wild soil into tilled soil. */
    TILLER,
    /** Waters a planted tile. */
    IRRIGATOR,
    /** Breaks mineral nodes and salvage piles. */
    CUTTER,
    /** Reveals crew logs and reads module diagnostics. */
    SCANNER
}
