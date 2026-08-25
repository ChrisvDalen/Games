package com.moneyfirst.towerperil.battle;

/** Result of resolving one auto-battle. */
public final class BattleOutcome {
    private final boolean win;
    private final double rosterEffectivePower;
    private final double waveEffectivePower;
    private final int lootGems;

    public BattleOutcome(boolean win, double rosterEffectivePower, double waveEffectivePower, int lootGems) {
        this.win = win;
        this.rosterEffectivePower = rosterEffectivePower;
        this.waveEffectivePower = waveEffectivePower;
        this.lootGems = lootGems;
    }

    public boolean isWin() {
        return win;
    }

    public double getRosterEffectivePower() {
        return rosterEffectivePower;
    }

    public double getWaveEffectivePower() {
        return waveEffectivePower;
    }

    public int getLootGems() {
        return lootGems;
    }

    @Override
    public String toString() {
        return "BattleOutcome{" + (win ? "WIN" : "LOSS") + " roster=" + rosterEffectivePower
                + " wave=" + waveEffectivePower + " loot=" + lootGems + "}";
    }
}
