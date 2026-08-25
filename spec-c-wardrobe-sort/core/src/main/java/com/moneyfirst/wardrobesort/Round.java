package com.moneyfirst.wardrobesort;

import java.util.List;
import java.util.Objects;

/**
 * Everything needed to play one round: the target outfit, the shuffled tray
 * of garments to drag from (guaranteed to contain every garment the target
 * needs, plus decoys), and the starting countdown duration.
 */
public final class Round {

    private final int roundNumber;
    private final long seed;
    private final OutfitTarget target;
    private final List<Garment> tray;
    private final float timerSeconds;

    public Round(int roundNumber, long seed, OutfitTarget target, List<Garment> tray, float timerSeconds) {
        this.roundNumber = roundNumber;
        this.seed = seed;
        this.target = Objects.requireNonNull(target, "target");
        this.tray = List.copyOf(tray);
        this.timerSeconds = timerSeconds;
    }

    public int roundNumber() {
        return roundNumber;
    }

    public long seed() {
        return seed;
    }

    public OutfitTarget target() {
        return target;
    }

    public List<Garment> tray() {
        return tray;
    }

    public float timerSeconds() {
        return timerSeconds;
    }
}
