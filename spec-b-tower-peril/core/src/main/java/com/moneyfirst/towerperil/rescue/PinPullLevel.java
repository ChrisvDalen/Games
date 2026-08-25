package com.moneyfirst.towerperil.rescue;

/** A generated, guaranteed-solvable pin-pull room plus the seed that produced it. */
public final class PinPullLevel {
    private final Grid grid;
    private final long seed;
    private final int totalCharacters;

    public PinPullLevel(Grid grid, long seed) {
        this.grid = grid;
        this.seed = seed;
        this.totalCharacters = grid.getCharacters().size();
    }

    public Grid getGrid() {
        return grid;
    }

    public long getSeed() {
        return seed;
    }

    /** Number of characters present when the level was generated (before any pulls). */
    public int getTotalCharacters() {
        return totalCharacters;
    }
}
