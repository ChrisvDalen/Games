package com.moneyfirst.towerperil.rescue;

import com.moneyfirst.towerperil.battle.UnitType;

/** A rescuable character. Once it slides off the grid's exit it becomes a battle {@code Unit}. */
public final class Character extends GridEntity {
    private final UnitType type;

    public Character(String id, UnitType type, int col, int row) {
        super(id, col, row);
        this.type = type;
    }

    public UnitType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Character{" + getId() + " " + type + "}";
    }
}
