package com.moneyfirst.towerperil.battle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoBattleResolverTest {

    @Test
    void sameRosterWaveAndSeedAlwaysProduceTheSameOutcome() {
        List<Unit> roster = Arrays.asList(
                new Unit("a", UnitType.WARRIOR, Rarity.RARE),
                new Unit("b", UnitType.MAGE, Rarity.COMMON));
        EnemyWave wave = new EnemyWave("wave-1", 20.0, UnitType.TANK, 30);

        BattleOutcome first = AutoBattleResolver.resolve(roster, wave, 999L);
        BattleOutcome second = AutoBattleResolver.resolve(roster, wave, 999L);

        assertEquals(first.isWin(), second.isWin());
        assertEquals(first.getRosterEffectivePower(), second.getRosterEffectivePower());
        assertEquals(first.getWaveEffectivePower(), second.getWaveEffectivePower());
        assertEquals(first.getLootGems(), second.getLootGems());
    }

    @Test
    void differentSeedsCanChangeTheVarianceButStayWithinBounds() {
        List<Unit> roster = Arrays.asList(new Unit("a", UnitType.WARRIOR, Rarity.EPIC));
        EnemyWave wave = new EnemyWave("wave-2", 40.0, UnitType.ARCHER, 25);

        for (long seed = 0; seed < 50; seed++) {
            BattleOutcome outcome = AutoBattleResolver.resolve(roster, wave, seed);
            assertTrue(outcome.getRosterEffectivePower() > 0);
            assertTrue(outcome.getWaveEffectivePower() > 0);
            assertTrue(outcome.getLootGems() >= 0);
        }
    }

    @Test
    void anOverwhelminglyStrongerRosterAlwaysWinsDespiteVariance() {
        List<Unit> roster = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            roster.add(new Unit("legend" + i, UnitType.WARRIOR, Rarity.LEGENDARY));
        }
        EnemyWave weakWave = new EnemyWave("weak-wave", 5.0, UnitType.TANK, 10);

        for (long seed = 0; seed < 30; seed++) {
            BattleOutcome outcome = AutoBattleResolver.resolve(roster, weakWave, seed);
            assertTrue(outcome.isWin(), "Legendary-stacked roster should crush a trivial wave (seed " + seed + ")");
        }
    }

    @Test
    void anOverwhelminglyWeakerRosterAlwaysLosesDespiteVariance() {
        List<Unit> roster = Arrays.asList(new Unit("weak", UnitType.TANK, Rarity.COMMON));
        EnemyWave crushingWave = new EnemyWave("crushing-wave", 500.0, UnitType.WARRIOR, 100);

        for (long seed = 0; seed < 30; seed++) {
            BattleOutcome outcome = AutoBattleResolver.resolve(roster, crushingWave, seed);
            assertTrue(!outcome.isWin(), "A single common should never beat a crushing wave (seed " + seed + ")");
            assertTrue(outcome.getLootGems() < crushingWave.getBaseLootGems(),
                    "Consolation loot on a loss must be less than the full win payout");
        }
    }

    @Test
    void winningLootEqualsWaveBaseLoot() {
        List<Unit> roster = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            roster.add(new Unit("u" + i, UnitType.WARRIOR, Rarity.LEGENDARY));
        }
        EnemyWave wave = new EnemyWave("wave-3", 10.0, UnitType.MAGE, 42);

        BattleOutcome outcome = AutoBattleResolver.resolve(roster, wave, 7L);

        assertTrue(outcome.isWin());
        assertEquals(42, outcome.getLootGems());
    }
}
