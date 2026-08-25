package com.moneyfirst.pourperfect.ads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdPacingPolicyTest {

    @Test
    void firstAndSecondSolveDoNotTriggerAnInterstitial() {
        AdPacingPolicy policy = new AdPacingPolicy();

        assertFalse(policy.onLevelSolved(), "level 1 solved should not show an interstitial");
        assertFalse(policy.onLevelSolved(), "level 2 solved should not show an interstitial");
    }

    @Test
    void thirdSolveTriggersInterstitialAndResetsCounter() {
        AdPacingPolicy policy = new AdPacingPolicy();
        policy.onLevelSolved();
        policy.onLevelSolved();

        boolean thirdSolve = policy.onLevelSolved();

        assertTrue(thirdSolve, "the 3rd solved level should trigger an interstitial");
        assertEquals(0, policy.getSolvesSinceLastInterstitial(), "counter must reset after firing");
    }

    @Test
    void cycleRepeatsExactlyEveryThreeSolves() {
        AdPacingPolicy policy = new AdPacingPolicy();
        boolean[] fired = new boolean[9];
        for (int i = 0; i < 9; i++) {
            fired[i] = policy.onLevelSolved();
        }

        // Solves are 1-indexed here: fired[2] is the 3rd solve, fired[5] the 6th, fired[8] the 9th.
        for (int i = 0; i < 9; i++) {
            boolean expectedFire = (i + 1) % 3 == 0;
            assertEquals(expectedFire, fired[i], "solve #" + (i + 1) + " fire mismatch");
        }
    }

    @Test
    void interstitialNeverFiresMoreOftenThanEveryThirdSolve() {
        AdPacingPolicy policy = new AdPacingPolicy();
        int fireCount = 0;
        for (int i = 0; i < 30; i++) {
            if (policy.onLevelSolved()) {
                fireCount++;
            }
        }
        assertEquals(10, fireCount, "30 solves at a 1-in-3 cadence must fire exactly 10 times");
    }

    @Test
    void customCadenceIsHonored() {
        AdPacingPolicy policy = new AdPacingPolicy(5);

        for (int i = 0; i < 4; i++) {
            assertFalse(policy.onLevelSolved());
        }
        assertTrue(policy.onLevelSolved());
    }

    @Test
    void constructorRejectsNonPositiveCadence() {
        assertThrows(IllegalArgumentException.class, () -> new AdPacingPolicy(0));
        assertThrows(IllegalArgumentException.class, () -> new AdPacingPolicy(-1));
    }

    @Test
    void rewardedOfferIsSuggestedOnlyWhenPlayerHasNoLegalMove() {
        AdPacingPolicy policy = new AdPacingPolicy();

        assertFalse(policy.shouldOfferRewardedForStuckPlayer(true));
        assertTrue(policy.shouldOfferRewardedForStuckPlayer(false));
    }
}
