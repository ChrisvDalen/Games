package com.moneyfirst.towerperil.ios;

import com.moneyfirst.towerperil.ads.AdsService;
import org.robovm.rt.bro.annotation.Bridge;

/**
 * Real {@link AdsService} implementation for iOS. Delegates to the small
 * Objective-C shim in {@code native/AdsBridge.h/.m} (built on the
 * Google-Mobile-Ads-SDK CocoaPod's {@code GADInterstitialAd}/{@code GADRewardedAd}/
 * {@code GADBannerView}) via RoboVM's {@code @Bridge} native-method binding -
 * the standard way to call plain C functions that RoboVM has no generated
 * bindings for.
 *
 * <p><b>Known limitation</b> (see README.md "iOS native ads shim"): the
 * native {@code ads_*} functions are fire-and-forget C calls with no
 * callback parameters, so this class cannot yet learn <i>when</i> an
 * interstitial is actually dismissed or a reward is actually earned from
 * the native layer - it invokes the supplied {@code Runnable} right after
 * requesting the native show call. Wiring a real native -> Java event (e.g.
 * a RoboVM {@code @Callback} static method invoked from
 * {@code TPAdsDelegate}) is the natural next step before shipping.
 */
public final class IosAdsService implements AdsService {

    private boolean interstitialLoaded;
    private boolean rewardedLoaded;

    public IosAdsService() {
        nativeInitialize();
    }

    @Override
    public void loadInterstitial() {
        nativeLoadInterstitial();
        interstitialLoaded = true;
    }

    @Override
    public void showInterstitialIfLoaded(Runnable onClosed) {
        if (interstitialLoaded) {
            nativeShowInterstitial();
            interstitialLoaded = false;
        }
        onClosed.run();
    }

    @Override
    public void loadRewarded() {
        nativeLoadRewarded();
        rewardedLoaded = true;
    }

    @Override
    public void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable) {
        if (!rewardedLoaded) {
            onNotAvailable.run();
            return;
        }
        nativeShowRewarded();
        rewardedLoaded = false;
        onReward.run();
    }

    @Override
    public void showBanner() {
        nativeShowBanner();
    }

    @Override
    public void hideBanner() {
        nativeHideBanner();
    }

    @Bridge(symbol = "ads_initialize")
    private static native void nativeInitialize();

    @Bridge(symbol = "ads_loadInterstitial")
    private static native void nativeLoadInterstitial();

    @Bridge(symbol = "ads_showInterstitial")
    private static native void nativeShowInterstitial();

    @Bridge(symbol = "ads_loadRewarded")
    private static native void nativeLoadRewarded();

    @Bridge(symbol = "ads_showRewarded")
    private static native void nativeShowRewarded();

    @Bridge(symbol = "ads_showBanner")
    private static native void nativeShowBanner();

    @Bridge(symbol = "ads_hideBanner")
    private static native void nativeHideBanner();
}
