package com.moneyfirst.towerperil.ads;

/**
 * Platform-agnostic ads surface, injected into {@code TowerPerilGame} at
 * launch. Shape matches the shared money-first architecture contract
 * exactly (see docs/MONEY_FIRST_ARCHITECTURE.md) so every spec's core
 * depends only on this interface, never on a platform ad SDK.
 *
 * <ul>
 *   <li>Android implementation: {@code com.google.android.gms.ads.*} (Google
 *   Mobile Ads SDK for Android).</li>
 *   <li>iOS implementation: a thin Objective-C shim ({@code ios/native/AdsBridge.h/.m})
 *   around {@code GADInterstitialAd}/{@code GADRewardedAd}/{@code GADBannerView}
 *   from the Google-Mobile-Ads-SDK CocoaPod, called from Java via RoboVM
 *   native-method binding.</li>
 * </ul>
 */
public interface AdsService {
    void loadInterstitial();

    void showInterstitialIfLoaded(Runnable onClosed);

    void loadRewarded();

    void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable);

    void showBanner();

    void hideBanner();
}
