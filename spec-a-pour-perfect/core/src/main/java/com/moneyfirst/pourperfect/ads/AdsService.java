package com.moneyfirst.pourperfect.ads;

/**
 * Platform-agnostic ad surface, injected into {@code PourPerfectGame} at construction time so
 * {@code core} never depends on a platform ad SDK directly. Shape matches
 * {@code docs/MONEY_FIRST_ARCHITECTURE.md}'s cross-spec {@code AdsService} contract exactly.
 *
 * <ul>
 *   <li>Android: {@code com.google.android.gms.ads.*} (Google Mobile Ads SDK).</li>
 *   <li>iOS: a thin Objective-C shim ({@code ios/native/AdsBridge.h/.m}) around
 *       {@code GADInterstitialAd}/{@code GADRewardedAd}/{@code GADBannerView}, called from Java
 *       via RoboVM native-method binding.</li>
 * </ul>
 */
public interface AdsService {
    void loadInterstitial();

    /**
     * Shows a preloaded interstitial if one is ready. {@code onClosed} is always invoked exactly
     * once - either after the shown ad is dismissed, or immediately if no ad was loaded - so
     * callers can unconditionally chain follow-up logic (e.g. a screen transition) off it without
     * needing a separate "was it actually shown" query.
     */
    void showInterstitialIfLoaded(Runnable onClosed);

    void loadRewarded();

    void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable);

    void showBanner();

    void hideBanner();
}
