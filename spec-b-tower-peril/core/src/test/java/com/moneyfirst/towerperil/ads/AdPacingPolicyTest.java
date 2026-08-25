package com.moneyfirst.towerperil.ads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdPacingPolicyTest {

    /** Records every call made against it - lets tests assert on exact call sequences/counts. */
    private static final class RecordingAdsService implements AdsService {
        int loadInterstitialCalls = 0;
        int showInterstitialCalls = 0;
        int loadRewardedCalls = 0;
        int showRewardedCalls = 0;

        @Override
        public void loadInterstitial() {
            loadInterstitialCalls++;
        }

        @Override
        public void showInterstitialIfLoaded(Runnable onClosed) {
            showInterstitialCalls++;
            onClosed.run();
        }

        @Override
        public void loadRewarded() {
            loadRewardedCalls++;
        }

        @Override
        public void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable) {
            showRewardedCalls++;
            onReward.run();
        }

        @Override
        public void showBanner() {
        }

        @Override
        public void hideBanner() {
        }
    }

    @Test
    void interstitialFiresAfterEverySinglePinPullLevelCompletion() {
        RecordingAdsService ads = new RecordingAdsService();
        AdPacingPolicy policy = new AdPacingPolicy(ads);

        for (int level = 1; level <= 10; level++) {
            policy.onPinPullLevelComplete();
            // Unlike Spec A's every-3rd-level cadence, Tower Peril fires on EVERY level.
            assertEquals(level, ads.showInterstitialCalls,
                    "Interstitial must have fired exactly " + level + " times after " + level + " levels");
            assertEquals(level, policy.getTotalInterstitialsShown());
        }
    }

    @Test
    void interstitialCadenceIsNotEveryThirdLevel() {
        RecordingAdsService ads = new RecordingAdsService();
        AdPacingPolicy policy = new AdPacingPolicy(ads);

        policy.onPinPullLevelComplete();
        policy.onPinPullLevelComplete();
        // A Spec-A-style every-3rd cadence would show zero interstitials after only two levels.
        assertEquals(2, ads.showInterstitialCalls, "Tower Peril's pacing must not skip levels 1 and 2");
    }

    @Test
    void everyLevelCompletionAlsoLoadsAFreshInterstitial() {
        RecordingAdsService ads = new RecordingAdsService();
        AdPacingPolicy policy = new AdPacingPolicy(ads);

        policy.onPinPullLevelComplete();
        policy.onPinPullLevelComplete();
        policy.onPinPullLevelComplete();

        assertEquals(3, ads.loadInterstitialCalls);
    }

    @Test
    void doubleLootOfferUsesRewardedVideoAndInvokesCallback() {
        RecordingAdsService ads = new RecordingAdsService();
        AdPacingPolicy policy = new AdPacingPolicy(ads);
        boolean[] doubled = {false};

        policy.offerDoubleLootAfterBattle(() -> doubled[0] = true, () -> {
        });

        assertTrue(doubled[0]);
        assertEquals(1, ads.loadRewardedCalls);
        assertEquals(1, ads.showRewardedCalls);
        assertEquals(1, policy.getTotalDoubleLootOffers());
    }

    @Test
    void reviveOfferUsesRewardedVideoAndInvokesCallback() {
        RecordingAdsService ads = new RecordingAdsService();
        AdPacingPolicy policy = new AdPacingPolicy(ads);
        boolean[] revived = {false};

        policy.offerRevive(() -> revived[0] = true, () -> {
        });

        assertTrue(revived[0]);
        assertEquals(1, policy.getTotalReviveOffers());
    }
}
