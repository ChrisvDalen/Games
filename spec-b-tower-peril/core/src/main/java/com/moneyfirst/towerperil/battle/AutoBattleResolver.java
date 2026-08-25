package com.moneyfirst.towerperil.battle;

import java.util.List;
import java.util.Random;

/**
 * Resolves an automated battle: roster total power (with type-matchup
 * multipliers against the wave's dominant type) versus wave power, with a
 * small seeded variance so the same roster/wave/seed always produces the
 * same outcome.
 */
public final class AutoBattleResolver {

    private static final double VARIANCE_SPAN = 0.20; // +/-10%
    private static final double VARIANCE_FLOOR = 0.90;
    private static final double CONSOLATION_LOOT_FRACTION = 0.25;

    private AutoBattleResolver() {
    }

    public static BattleOutcome resolve(List<Unit> roster, EnemyWave wave, long seed) {
        Random rng = new Random(seed);

        double rosterPower = 0.0;
        for (Unit unit : roster) {
            double multiplier = unit.getType().matchupMultiplier(wave.getDominantType());
            rosterPower += unit.getPower() * multiplier;
        }
        double rosterVariance = VARIANCE_FLOOR + rng.nextDouble() * VARIANCE_SPAN;
        double waveVariance = VARIANCE_FLOOR + rng.nextDouble() * VARIANCE_SPAN;

        double rosterEffective = rosterPower * rosterVariance;
        double waveEffective = wave.getPower() * waveVariance;

        boolean win = rosterEffective >= waveEffective;
        int loot = win
                ? wave.getBaseLootGems()
                : (int) Math.round(wave.getBaseLootGems() * CONSOLATION_LOOT_FRACTION);

        return new BattleOutcome(win, rosterEffective, waveEffective, loot);
    }
}
