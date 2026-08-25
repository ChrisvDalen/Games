package com.moneyfirst.towerperil.battle;

/** An enemy wave the roster fights in the auto-battle phase. */
public final class EnemyWave {
    private final String id;
    private final double power;
    private final UnitType dominantType;
    private final int baseLootGems;

    public EnemyWave(String id, double power, UnitType dominantType, int baseLootGems) {
        this.id = id;
        this.power = power;
        this.dominantType = dominantType;
        this.baseLootGems = baseLootGems;
    }

    public String getId() {
        return id;
    }

    public double getPower() {
        return power;
    }

    /** The type the wave is mostly made of - used for the roster's matchup multiplier. */
    public UnitType getDominantType() {
        return dominantType;
    }

    public int getBaseLootGems() {
        return baseLootGems;
    }
}
