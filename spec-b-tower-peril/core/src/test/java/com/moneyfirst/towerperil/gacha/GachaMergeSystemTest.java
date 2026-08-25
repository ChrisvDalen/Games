package com.moneyfirst.towerperil.gacha;

import com.moneyfirst.towerperil.battle.Rarity;
import com.moneyfirst.towerperil.battle.Unit;
import com.moneyfirst.towerperil.battle.UnitType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GachaMergeSystemTest {

    @Test
    void rollDistributionMatchesPublishedWeightsWithinStatisticalTolerance() {
        Random rng = new Random(1234L);
        GachaMergeSystem system = new GachaMergeSystem(rng);
        int sampleSize = 200_000;
        int[] counts = new int[Rarity.values().length];

        for (int i = 0; i < sampleSize; i++) {
            counts[system.roll().getRarity().ordinal()]++;
        }

        // 60/25/12/3 expected; allow +/-1.5 percentage points absolute for this sample size
        // (binomial std-dev for the smallest bucket, 3%, at n=200,000 is ~0.038pp, so 1.5pp is a very safe bound).
        assertWithinTolerance(counts[Rarity.COMMON.ordinal()], sampleSize, 0.60, 0.015);
        assertWithinTolerance(counts[Rarity.RARE.ordinal()], sampleSize, 0.25, 0.015);
        assertWithinTolerance(counts[Rarity.EPIC.ordinal()], sampleSize, 0.12, 0.015);
        assertWithinTolerance(counts[Rarity.LEGENDARY.ordinal()], sampleSize, 0.03, 0.015);
    }

    private static void assertWithinTolerance(int count, int sampleSize, double expectedFraction, double toleranceFraction) {
        double observed = (double) count / sampleSize;
        assertTrue(Math.abs(observed - expectedFraction) <= toleranceFraction,
                "Expected ~" + expectedFraction + " but observed " + observed);
    }

    @Test
    void rollWeightsSumToExactlyOneHundred() {
        int sum = 0;
        for (Rarity rarity : Rarity.values()) {
            sum += GachaMergeSystem.weightFor(rarity);
        }
        assertEquals(100, sum);
    }

    @Test
    void threeIdenticalUnitsMergeIntoOneOfTheNextRarity() {
        GachaMergeSystem system = new GachaMergeSystem(new Random(1L));
        List<Unit> roster = new ArrayList<>();
        roster.add(new Unit("a", UnitType.WARRIOR, Rarity.COMMON));
        roster.add(new Unit("b", UnitType.WARRIOR, Rarity.COMMON));
        roster.add(new Unit("c", UnitType.WARRIOR, Rarity.COMMON));

        List<Unit> merged = system.mergeAll(roster);

        assertEquals(1, merged.size());
        assertEquals(Rarity.RARE, merged.get(0).getRarity());
        assertEquals(UnitType.WARRIOR, merged.get(0).getType());
    }

    @Test
    void mergingCascadesThroughMultipleTiers() {
        GachaMergeSystem system = new GachaMergeSystem(new Random(2L));
        List<Unit> roster = new ArrayList<>();
        // 9 identical commons -> 3 rares -> 1 epic
        for (int i = 0; i < 9; i++) {
            roster.add(new Unit("u" + i, UnitType.MAGE, Rarity.COMMON));
        }

        List<Unit> merged = system.mergeAll(roster);

        assertEquals(1, merged.size());
        assertEquals(Rarity.EPIC, merged.get(0).getRarity());
    }

    @Test
    void unitsOfDifferentTypeOrRarityNeverMergeTogether() {
        GachaMergeSystem system = new GachaMergeSystem(new Random(3L));
        List<Unit> roster = new ArrayList<>();
        roster.add(new Unit("a", UnitType.WARRIOR, Rarity.COMMON));
        roster.add(new Unit("b", UnitType.ARCHER, Rarity.COMMON));
        roster.add(new Unit("c", UnitType.WARRIOR, Rarity.RARE));

        List<Unit> merged = system.mergeAll(roster);

        assertEquals(3, merged.size(), "No group reaches 3 identical type+rarity units, so nothing merges");
    }

    @Test
    void legendaryUnitsDoNotMergeFurtherButExcessCopiesRemainInRoster() {
        GachaMergeSystem system = new GachaMergeSystem(new Random(4L));
        List<Unit> roster = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            roster.add(new Unit("l" + i, UnitType.TANK, Rarity.LEGENDARY));
        }

        List<Unit> merged = system.mergeAll(roster);

        assertEquals(3, merged.size());
        for (Unit unit : merged) {
            assertEquals(Rarity.LEGENDARY, unit.getRarity());
        }
    }

    @Test
    void mergeAllDoesNotMutateTheInputList() {
        GachaMergeSystem system = new GachaMergeSystem(new Random(5L));
        List<Unit> roster = new ArrayList<>();
        roster.add(new Unit("a", UnitType.WARRIOR, Rarity.COMMON));
        roster.add(new Unit("b", UnitType.WARRIOR, Rarity.COMMON));
        roster.add(new Unit("c", UnitType.WARRIOR, Rarity.COMMON));
        int originalSize = roster.size();

        system.mergeAll(roster);

        assertEquals(originalSize, roster.size());
    }
}
