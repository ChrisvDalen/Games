package com.moneyfirst.wardrobesort;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdPacingPolicyTest {

    @Test
    void interstitialFiresAfterEveryRound() {
        AdPacingPolicy policy = new AdPacingPolicy();

        for (int round = 1; round <= 25; round++) {
            assertTrue(policy.onRoundCompleted(round), "round " + round + " should trigger an interstitial - Wardrobe Sort shows one after EACH round");
            assertEquals(round, policy.lastInterstitialRound());
        }
    }

    @Test
    void repeatedCallForTheSameRoundDoesNotFireTwice() {
        AdPacingPolicy policy = new AdPacingPolicy();

        assertTrue(policy.onRoundCompleted(1));
        assertFalse(policy.onRoundCompleted(1), "calling onRoundCompleted twice for the same round must not double-fire the interstitial");
        assertEquals(1, policy.lastInterstitialRound());
    }

    @Test
    void goingBackwardsInRoundNumberDoesNotFireAgain() {
        AdPacingPolicy policy = new AdPacingPolicy();
        policy.onRoundCompleted(5);

        assertFalse(policy.onRoundCompleted(3), "an out-of-order/earlier round number must not re-trigger the interstitial");
    }

    @Test
    void rewardedVideoIsOfferableForBothExtraTimeAndOutfitHint() {
        AdPacingPolicy policy = new AdPacingPolicy();

        assertTrue(policy.canRequestRewarded(RewardedPurpose.EXTRA_TIME));
        assertTrue(policy.canRequestRewarded(RewardedPurpose.OUTFIT_HINT));
    }

    @Test
    void bannerOnlyOnHubScreen() {
        AdPacingPolicy policy = new AdPacingPolicy();

        assertTrue(policy.shouldShowBanner(true));
        assertFalse(policy.shouldShowBanner(false));
    }

    @Test
    void rejectsRoundNumbersBelowOne() {
        AdPacingPolicy policy = new AdPacingPolicy();
        assertThrows(IllegalArgumentException.class, () -> policy.onRoundCompleted(0));
    }
}
