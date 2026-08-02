package com.keplersharvest.player;

/**
 * The player's stamina.
 *
 * <p>Maximum energy is a base value plus whatever repaired modules contribute, so the meter grows
 * as the colony comes back online.
 */
public final class EnergyMeter {

    private final int baseMax;
    private int bonusMax;
    private int current;

    public EnergyMeter(int baseMax) {
        if (baseMax < 1) {
            throw new IllegalArgumentException("Base max energy must be positive");
        }
        this.baseMax = baseMax;
        this.current = baseMax;
    }

    public int current() {
        return current;
    }

    public int max() {
        return baseMax + bonusMax;
    }

    public float fraction() {
        return (float) current / max();
    }

    public void setBonusMax(int bonusMax) {
        this.bonusMax = Math.max(0, bonusMax);
        current = Math.min(current, max());
    }

    /** True when there is not enough energy left for {@code cost}. */
    public boolean tooTiredFor(int cost) {
        return current < cost;
    }

    /**
     * Spends energy if it is available.
     *
     * @return false when the player is too tired, leaving the meter untouched
     */
    public boolean spend(int cost) {
        if (cost <= 0) {
            return true;
        }
        if (tooTiredFor(cost)) {
            return false;
        }
        current -= cost;
        return true;
    }

    /** Drains energy without refusing; used by hazards, which can push the player to collapse. */
    public void drain(int amount) {
        current = Math.max(0, current - amount);
    }

    public void restore(int amount) {
        current = Math.clamp((long) current + amount, 0, max());
    }

    public void fill() {
        current = max();
    }

    public boolean collapsed() {
        return current <= 0;
    }

    /** Restores an exact value from a save. */
    public void set(int value) {
        current = Math.clamp(value, 0, max());
    }
}
