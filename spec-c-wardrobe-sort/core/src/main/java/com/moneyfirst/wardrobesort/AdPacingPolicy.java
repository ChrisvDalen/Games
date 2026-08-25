package com.moneyfirst.wardrobesort;

/**
 * Wardrobe Sort's ad frequency/placement rules, kept in {@code core} (not
 * platform code) so they're unit-testable:
 *
 * <ul>
 *   <li>Interstitial: after EACH round (win or lose), at most once per round.</li>
 *   <li>Rewarded video: player-initiated, for either an extra {@value RoundTimer#REWARDED_EXTENSION_SECONDS}s
 *       of round time or one outfit hint - always offerable, gated only by ad
 *       availability (an {@link AdsService} concern, not a pacing concern).</li>
 *   <li>Banner: shown only on the level/round-select hub screen.</li>
 * </ul>
 */
public final class AdPacingPolicy {

    private int lastInterstitialRound = 0;

    /**
     * Call once when a round finishes (win or timeout). Returns {@code true}
     * exactly the first time it's called for a given {@code roundNumber} -
     * i.e. "yes, show the interstitial now" - and {@code false} on any
     * repeat call for a round already accounted for, so a caller that
     * accidentally invokes this twice for the same round transition can
     * never double up on interstitials.
     */
    public boolean onRoundCompleted(int roundNumber) {
        if (roundNumber < 1) {
            throw new IllegalArgumentException("roundNumber must be >= 1, was " + roundNumber);
        }
        if (roundNumber <= lastInterstitialRound) {
            return false;
        }
        lastInterstitialRound = roundNumber;
        return true;
    }

    /** The highest round number an interstitial has been fired for so far (0 if none yet). */
    public int lastInterstitialRound() {
        return lastInterstitialRound;
    }

    /**
     * Whether the player may currently request a rewarded video for the
     * given purpose. Both purposes are always offerable by pacing policy;
     * the actual gate is ad availability ({@link AdsService#showRewardedIfLoaded})
     * and, for {@link RewardedPurpose#EXTRA_TIME}, {@link RoundTimer#isExtensionAvailable()}.
     */
    public boolean canRequestRewarded(RewardedPurpose purpose) {
        return purpose != null;
    }

    /** True only on the level/round-select hub screen, per the banner placement rule. */
    public boolean shouldShowBanner(boolean onHubScreen) {
        return onHubScreen;
    }
}
