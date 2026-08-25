package com.moneyfirst.towerperil.battle;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** A rescued or gacha-rolled unit that can fight in the auto-battle phase. */
public final class Unit {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final double BASE_POWER = 10.0;

    private final String id;
    private final UnitType type;
    private final Rarity rarity;

    public Unit(String id, UnitType type, Rarity rarity) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.rarity = Objects.requireNonNull(rarity, "rarity");
    }

    /** Convenience factory that mints a unique id - handy for gacha rolls and tests. */
    public static Unit rolled(UnitType type, Rarity rarity) {
        return new Unit("unit-" + SEQUENCE.incrementAndGet(), type, rarity);
    }

    public String getId() {
        return id;
    }

    public UnitType getType() {
        return type;
    }

    public Rarity getRarity() {
        return rarity;
    }

    /** Flat combat power, before any type-matchup multiplier is applied. */
    public double getPower() {
        return BASE_POWER * rarity.powerMultiplier;
    }

    /** True if this unit is the same type+rarity as another - the merge-3 eligibility test. */
    public boolean isMergeableWith(Unit other) {
        return other != null && type == other.type && rarity == other.rarity;
    }

    @Override
    public String toString() {
        return "Unit{" + id + " " + rarity + " " + type + " pow=" + getPower() + "}";
    }
}
