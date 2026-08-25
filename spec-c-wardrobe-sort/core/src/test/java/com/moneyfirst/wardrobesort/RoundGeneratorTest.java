package com.moneyfirst.wardrobesort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundGeneratorTest {

    private final RoundGenerator generator = RoundGenerator.withBaseCatalog();

    @ParameterizedTest
    @ValueSource(longs = {
        1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
        11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L
    })
    void trayAlwaysContainsEveryGarmentTheTargetNeeds(long seed) {
        for (int round = 1; round <= 15; round++) {
            Round r = generator.generateRound(round, seed);
            Set<String> trayIds = new HashSet<>();
            for (Garment g : r.tray()) {
                trayIds.add(g.id());
            }
            for (GarmentSlot slot : GarmentSlot.values()) {
                Garment required = r.target().required(slot);
                assertTrue(trayIds.contains(required.id()),
                    "seed=" + seed + " round=" + round + ": tray missing required garment " + required.id()
                        + " for slot " + slot);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 3L, 4L, 5L})
    void trayHasNoDuplicateGarments(long seed) {
        for (int round = 1; round <= 10; round++) {
            Round r = generator.generateRound(round, seed);
            Set<String> seen = new HashSet<>();
            for (Garment g : r.tray()) {
                assertTrue(seen.add(g.id()), "duplicate garment " + g.id() + " in tray for seed=" + seed + " round=" + round);
            }
        }
    }

    @Test
    void sameSeedAndRoundNumberProduceIdenticalRounds() {
        Round a = generator.generateRound(7, 42L);
        Round b = generator.generateRound(7, 42L);

        for (GarmentSlot slot : GarmentSlot.values()) {
            assertEquals(a.target().required(slot).id(), b.target().required(slot).id());
        }
        List<String> trayA = a.tray().stream().map(Garment::id).toList();
        List<String> trayB = b.tray().stream().map(Garment::id).toList();
        assertEquals(trayA, trayB, "tray order must be deterministic for the same (seed, roundNumber)");
        assertEquals(a.timerSeconds(), b.timerSeconds());
    }

    @Test
    void differentRoundNumbersTypicallyProduceDifferentRounds() {
        Round r1 = generator.generateRound(1, 99L);
        Round r2 = generator.generateRound(2, 99L);
        // Not a strict guarantee for every possible slot, but the full 5-slot target
        // vector plus tray order matching by coincidence across two different round
        // numbers is astronomically unlikely - a real determinism/entropy smoke test.
        boolean identical = true;
        for (GarmentSlot slot : GarmentSlot.values()) {
            if (!r1.target().required(slot).id().equals(r2.target().required(slot).id())) {
                identical = false;
                break;
            }
        }
        assertFalse(identical, "round 1 and round 2 target outfits should not be identical for the same seed");
    }

    @Test
    void decoyCountTrendsUpAndIsFlooredAndCeilinged() {
        assertEquals(RoundGenerator.MIN_DECOYS, RoundGenerator.computeDecoyCount(1));
        int previous = RoundGenerator.computeDecoyCount(1);
        for (int round = 2; round <= 100; round++) {
            int decoys = RoundGenerator.computeDecoyCount(round);
            assertTrue(decoys >= previous, "decoy count must never decrease as rounds progress (round " + round + ")");
            assertTrue(decoys >= RoundGenerator.MIN_DECOYS, "decoy count must never drop below the floor");
            assertTrue(decoys <= RoundGenerator.MAX_DECOYS, "decoy count must never exceed the ceiling");
            previous = decoys;
        }
        assertEquals(RoundGenerator.MAX_DECOYS, RoundGenerator.computeDecoyCount(1000),
            "decoy count must eventually saturate at the ceiling for very high round numbers");
    }

    @Test
    void timerTrendsDownAndIsFlooredAtASaneMinimum() {
        assertEquals(RoundGenerator.START_TIMER_SECONDS, RoundGenerator.computeTimerSeconds(1));
        float previous = RoundGenerator.computeTimerSeconds(1);
        for (int round = 2; round <= 100; round++) {
            float t = RoundGenerator.computeTimerSeconds(round);
            assertTrue(t <= previous, "timer must never increase as rounds progress (round " + round + ")");
            assertTrue(t >= RoundGenerator.MIN_TIMER_SECONDS, "timer must never drop below the floor");
            previous = t;
        }
        assertEquals(RoundGenerator.MIN_TIMER_SECONDS, RoundGenerator.computeTimerSeconds(1000),
            "timer must eventually floor out for very high round numbers");
    }

    @Test
    void actualTrayDecoyCountNeverExceedsAvailablePoolAndNeverGoesNegative() {
        for (int round = 1; round <= 200; round += 7) {
            Round r = generator.generateRound(round, 12345L);
            int decoysInTray = r.tray().size() - GarmentSlot.values().length;
            assertTrue(decoysInTray >= 0, "round " + round + " has fewer tray items than required slots");
            assertTrue(decoysInTray <= RoundGenerator.MAX_DECOYS, "round " + round + " has more decoys than the ceiling allows");
        }
    }

    @Test
    void generateRoundRejectsNonPositiveRoundNumbers() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> generator.generateRound(0, 1L));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> generator.generateRound(-1, 1L));
    }
}
