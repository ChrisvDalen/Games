package com.moneyfirst.towerperil.battle;

/** Unit rarity tiers. Ordinal order doubles as merge/upgrade order. */
public enum Rarity {
    COMMON(1.0),
    RARE(2.2),
    EPIC(5.0),
    LEGENDARY(12.0);

    /** Base power multiplier applied to a unit's flat base power at this rarity. */
    public final double powerMultiplier;

    Rarity(double powerMultiplier) {
        this.powerMultiplier = powerMultiplier;
    }

    private static final Rarity[] VALUES = values();

    public boolean hasNext() {
        return ordinal() < VALUES.length - 1;
    }

    public Rarity next() {
        if (!hasNext()) {
            throw new IllegalStateException(this + " has no next rarity to merge into");
        }
        return VALUES[ordinal() + 1];
    }
}
