package com.moneyfirst.wardrobesort;

/**
 * Platform-agnostic ads contract, injected into {@link WardrobeSortGame} at
 * launch (Android: {@code com.google.android.gms.ads.*}; iOS: the
 * {@code native/AdsBridge} Objective-C shim around the Google Mobile Ads
 * iOS SDK). Shape matches docs/MONEY_FIRST_ARCHITECTURE.md exactly so all
 * three specs share the same contract.
 */
public interface AdsService {
    void loadInterstitial();

    void showInterstitialIfLoaded(Runnable onClosed);

    void loadRewarded();

    void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable);

    void showBanner();

    void hideBanner();
}
