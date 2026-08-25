package com.moneyfirst.pourperfect.ios;

import com.moneyfirst.pourperfect.ads.AdsService;
import org.robovm.rt.bro.annotation.Bridge;

/**
 * Real {@link AdsService} implementation for iOS: every method is a thin call into
 * {@code native/AdsBridge.h/.m}, a small Objective-C shim around the Google Mobile Ads iOS SDK
 * (no generated RoboVM bindings exist for that SDK, hence the hand-written bridge). Native
 * methods are bound via RoboVM's {@code @Bridge} native-method linkage, RoboVM's documented
 * mechanism for calling arbitrary C functions - see
 * <a href="http://docs.robovm.mobidevelop.com/">RoboVM's "custom native code" docs</a>.
 *
 * <p>Async ad-close/reward results are delivered by polling (see {@code AdsBridge.h} for why) -
 * {@link #pollNativeCallbacks()} must be called once per frame, which {@link PollingApplicationListener}
 * does on this service's behalf so {@code core}'s {@code AdsService} interface itself never needs
 * a platform-specific per-frame hook.
 *
 * <p><strong>Not buildable/verified in the sandbox this project was authored in</strong> (needs
 * macOS + Xcode + the RoboVM/MobiVM toolchain, plus the native Objective-C shim to actually
 * compile against the Google-Mobile-Ads-SDK CocoaPod) - this is the one piece of the iOS module
 * that most needs a real device/simulator build to confirm. See {@code ../README.md}.
 */
public final class IosAdsService implements AdsService {

    private static final int REWARD_RESULT_NONE = 0;
    private static final int REWARD_RESULT_EARNED = 1;
    private static final int REWARD_RESULT_NOT_AVAILABLE = 2;

    private Runnable pendingInterstitialClosed;
    private Runnable pendingRewardEarned;
    private Runnable pendingRewardNotAvailable;

    public IosAdsService() {
        adsInitialize();
    }

    @Override
    public void loadInterstitial() {
        adsLoadInterstitial();
    }

    @Override
    public void showInterstitialIfLoaded(Runnable onClosed) {
        pendingInterstitialClosed = onClosed;
        adsShowInterstitial();
    }

    @Override
    public void loadRewarded() {
        adsLoadRewarded();
    }

    @Override
    public void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable) {
        pendingRewardEarned = onReward;
        pendingRewardNotAvailable = onNotAvailable;
        adsShowRewarded();
    }

    @Override
    public void showBanner() {
        adsShowBanner();
    }

    @Override
    public void hideBanner() {
        adsHideBanner();
    }

    /** Called once per frame by {@link PollingApplicationListener}; drains native completion flags. */
    void pollNativeCallbacks() {
        if (adsPollInterstitialClosed() != 0 && pendingInterstitialClosed != null) {
            Runnable callback = pendingInterstitialClosed;
            pendingInterstitialClosed = null;
            callback.run();
        }

        int rewardResult = adsPollRewardResult();
        if (rewardResult == REWARD_RESULT_EARNED && pendingRewardEarned != null) {
            Runnable callback = pendingRewardEarned;
            pendingRewardEarned = null;
            pendingRewardNotAvailable = null;
            callback.run();
        } else if (rewardResult == REWARD_RESULT_NOT_AVAILABLE && pendingRewardNotAvailable != null) {
            Runnable callback = pendingRewardNotAvailable;
            pendingRewardEarned = null;
            pendingRewardNotAvailable = null;
            callback.run();
        }
    }

    // -- Native bridge (native/AdsBridge.h/.m) -------------------------------------------------

    @Bridge(symbol = "ads_initialize")
    private static native void adsInitialize();

    @Bridge(symbol = "ads_loadInterstitial")
    private static native void adsLoadInterstitial();

    @Bridge(symbol = "ads_showInterstitial")
    private static native void adsShowInterstitial();

    @Bridge(symbol = "ads_pollInterstitialClosed")
    private static native int adsPollInterstitialClosed();

    @Bridge(symbol = "ads_loadRewarded")
    private static native void adsLoadRewarded();

    @Bridge(symbol = "ads_showRewarded")
    private static native void adsShowRewarded();

    @Bridge(symbol = "ads_pollRewardResult")
    private static native int adsPollRewardResult();

    @Bridge(symbol = "ads_showBanner")
    private static native void adsShowBanner();

    @Bridge(symbol = "ads_hideBanner")
    private static native void adsHideBanner();
}
