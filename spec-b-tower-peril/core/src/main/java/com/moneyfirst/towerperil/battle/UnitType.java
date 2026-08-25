package com.moneyfirst.towerperil.battle;

/**
 * The four rescued-unit archetypes. Each beats the next in the cycle and
 * loses to the previous one, giving the auto-battler a simple, deterministic
 * rock-paper-scissors-style matchup table.
 */
public enum UnitType {
    WARRIOR,
    ARCHER,
    MAGE,
    TANK;

    private static final UnitType[] VALUES = values();

    /** WARRIOR beats ARCHER beats MAGE beats TANK beats WARRIOR. */
    public UnitType getBeats() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    /** Multiplier applied when this type's units fight the given defender type. */
    public double matchupMultiplier(UnitType defender) {
        if (this == defender) {
            return 1.0;
        }
        if (getBeats() == defender) {
            return 1.25;
        }
        if (defender.getBeats() == this) {
            return 0.8;
        }
        return 1.0;
    }
}
