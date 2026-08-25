package com.moneyfirst.towerperil.rescue;

/**
 * A spike/enemy obstacle. Hazards never leave the grid through an exit - if
 * one is nearer the exit than a character sharing its line when a pin is
 * pulled, it blocks (catches) that character and every character behind it.
 */
public final class Hazard extends GridEntity {
    public Hazard(String id, int col, int row) {
        super(id, col, row);
    }

    @Override
    public String toString() {
        return "Hazard{" + getId() + "}";
    }
}
