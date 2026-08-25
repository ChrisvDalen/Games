package com.moneyfirst.pourperfect.ads;

/**
 * Pure ad-frequency logic, deliberately isolated from any {@link AdsService} call so it's cheap
 * to unit-test exhaustively. Encodes the two placement rules from
 * {@code docs/MONEY_FIRST_ARCHITECTURE.md} for Pour Perfect:
 * <ul>
 *   <li>an interstitial after every 3rd <em>solved</em> level, never more often;</li>
 *   <li>a rewarded-video offer (extra tube / hint / undo-all) surfaced when it's actually useful
 *       to the player - i.e. when they're stuck with no legal move.</li>
 * </ul>
 */
public final class AdPacingPolicy {

    private final int interstitialEveryNSolves;
    private int solvesSinceLastInterstitial;

    public AdPacingPolicy() {
        this(3);
    }

    public AdPacingPolicy(int interstitialEveryNSolves) {
        if (interstitialEveryNSolves < 1) {
            throw new IllegalArgumentException("interstitialEveryNSolves must be >= 1");
        }
        this.interstitialEveryNSolves = interstitialEveryNSolves;
    }

    /**
     * Call exactly once per level solved. Returns whether an interstitial should be shown now;
     * when it returns {@code true} the internal counter resets, so the next interstitial is again
     * exactly {@code interstitialEveryNSolves} solves away.
     */
    public boolean onLevelSolved() {
        solvesSinceLastInterstitial++;
        if (solvesSinceLastInterstitial >= interstitialEveryNSolves) {
            solvesSinceLastInterstitial = 0;
            return true;
        }
        return false;
    }

    public int getSolvesSinceLastInterstitial() {
        return solvesSinceLastInterstitial;
    }

    /**
     * Whether to proactively offer the player a rewarded-video boost (extra tube / hint /
     * undo-all) right now. Pure function of whether they currently have a legal move: offering it
     * only when they're stuck keeps the prompt meaningful instead of a constant nag.
     */
    public boolean shouldOfferRewardedForStuckPlayer(boolean hasLegalMoveAvailable) {
        return !hasLegalMoveAvailable;
    }
}
