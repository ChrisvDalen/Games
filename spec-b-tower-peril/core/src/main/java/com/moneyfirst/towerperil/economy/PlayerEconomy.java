package com.moneyfirst.towerperil.economy;

import com.moneyfirst.towerperil.battle.Unit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The player's persistent wallet + roster. Every mutation is guarded so gem
 * balances can never go negative, regardless of what calls into it (gacha
 * rolls, IAP grants, battle loot, season-pass claims).
 */
public final class PlayerEconomy {
    private long gems;
    private final List<Unit> roster = new ArrayList<>();
    private SeasonPassState seasonPassState = SeasonPassState.inactive();

    public long getGems() {
        return gems;
    }

    public void addGems(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add a negative gem amount: " + amount);
        }
        gems += amount;
    }

    /** @throws IllegalStateException if the spend would drive the balance negative. */
    public void spendGems(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot spend a negative gem amount: " + amount);
        }
        if (amount > gems) {
            throw new IllegalStateException("Insufficient gems: have " + gems + ", need " + amount);
        }
        gems -= amount;
    }

    public boolean canAfford(long amount) {
        return amount >= 0 && amount <= gems;
    }

    public List<Unit> getRoster() {
        return Collections.unmodifiableList(roster);
    }

    public void addUnit(Unit unit) {
        roster.add(unit);
    }

    public void addUnits(Collection<Unit> units) {
        roster.addAll(units);
    }

    /** Replaces the whole roster - used after a merge pass produces a new unit list. */
    public void setRoster(List<Unit> newRoster) {
        roster.clear();
        roster.addAll(newRoster);
    }

    public SeasonPassState getSeasonPassState() {
        return seasonPassState;
    }

    public void setSeasonPassState(SeasonPassState seasonPassState) {
        this.seasonPassState = seasonPassState;
    }
}
