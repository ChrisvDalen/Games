package com.moneyfirst.towerperil.ads;

/**
 * Tower Peril's ad placement/frequency rules, kept in {@code core} (not
 * left to platform code) so pacing is unit-testable:
 *
 * <ul>
 *   <li>Interstitial after <b>every single</b> completed pin-pull level -
 *   deliberately more aggressive than Spec A's every-3rd-level cadence,
 *   matching this spec's ads/IAP-hybrid monetization play.</li>
 *   <li>Rewarded video offered for "double loot" after an auto-battle.</li>
 *   <li>Rewarded video offered as a "revive" when a character is lost to a
 *   hazard, or when a battle is lost.</li>
 * </ul>
 */
public final class AdPacingPolicy {
    private final AdsService adsService;
    private int totalInterstitialsShown;
    private int totalReviveOffers;
    private int totalDoubleLootOffers;

    public AdPacingPolicy(AdsService adsService) {
        this.adsService = adsService;
    }

    /** Call once per completed pin-pull level, win or lose. Always shows an interstitial. */
    public void onPinPullLevelComplete() {
        adsService.loadInterstitial();
        adsService.showInterstitialIfLoaded(() -> {
        });
        totalInterstitialsShown++;
    }

    public int getTotalInterstitialsShown() {
        return totalInterstitialsShown;
    }

    /** Offers a rewarded-video "double loot" after an auto-battle resolves. */
    public void offerDoubleLootAfterBattle(Runnable onDoubled, Runnable onDeclinedOrUnavailable) {
        adsService.loadRewarded();
        adsService.showRewardedIfLoaded(onDoubled, onDeclinedOrUnavailable);
        totalDoubleLootOffers++;
    }

    /** Offers a rewarded-video revive when a character is lost, or a battle is lost. */
    public void offerRevive(Runnable onRevived, Runnable onDeclinedOrUnavailable) {
        adsService.loadRewarded();
        adsService.showRewardedIfLoaded(onRevived, onDeclinedOrUnavailable);
        totalReviveOffers++;
    }

    public int getTotalDoubleLootOffers() {
        return totalDoubleLootOffers;
    }

    public int getTotalReviveOffers() {
        return totalReviveOffers;
    }
}
