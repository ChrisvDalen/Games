package com.moneyfirst.wardrobesort.ios;

import com.moneyfirst.wardrobesort.AdsService;
import org.robovm.rt.bro.Bro;
import org.robovm.rt.bro.annotation.Bridge;
import org.robovm.rt.bro.annotation.Callback;
import org.robovm.rt.bro.annotation.Library;
import org.robovm.rt.bro.ptr.FunctionPtr;

import java.lang.reflect.Method;

/**
 * Real {@link AdsService} implementation for iOS. Binds to the plain C
 * functions exposed by {@code native/AdsBridge.h}/{@code .m} - a small
 * Objective-C shim around the Google Mobile Ads iOS SDK - via RoboVM's Bro
 * native-method mechanism ({@link Bridge} + {@link Bro#bind}), since no
 * generated RoboVM bindings for the Ads SDK exist.
 *
 * <p>{@code AdsBridge.m} is compiled directly into the app binary (not a
 * separate dylib), so every {@link Bridge} here resolves against
 * {@link Library#INTERNAL}.
 *
 * <p>Native-to-Java callbacks use RoboVM's {@link FunctionPtr}/{@link Callback}
 * mechanism: {@link #interstitialClosedTrampoline()} and
 * {@link #rewardedResultTrampoline(int)} are static methods the RoboVM
 * compiler turns into real native function pointers, handed to the C side
 * and invoked directly from Objective-C when an ad is dismissed. Since the
 * C callback signatures carry no user-data/context parameter (matching
 * {@code AdsBridge.h}'s {@code AdsVoidCallback}/{@code AdsRewardCallback}
 * typedefs), only one interstitial and one rewarded presentation can be
 * in flight at a time - which is exactly how {@code AdPacingPolicy} and
 * {@code AdsBridge.m}'s own single pending-callback fields already assume
 * ads are shown (one full-screen ad at a time, never overlapped).
 */
public final class IosAdsService implements AdsService {

    static {
        Bro.bind(IosAdsService.class);
    }

    private static final FunctionPtr INTERSTITIAL_CLOSED_TRAMPOLINE =
        trampoline("interstitialClosedTrampoline");
    private static final FunctionPtr REWARDED_RESULT_TRAMPOLINE =
        trampoline("rewardedResultTrampoline", int.class);

    private static Runnable pendingInterstitialClosed;
    private static Runnable pendingOnReward;
    private static Runnable pendingOnNotAvailable;

    public IosAdsService() {
        ads_initialize();
    }

    @Override
    public void loadInterstitial() {
        ads_loadInterstitial();
    }

    @Override
    public void showInterstitialIfLoaded(Runnable onClosed) {
        pendingInterstitialClosed = onClosed;
        ads_showInterstitial(INTERSTITIAL_CLOSED_TRAMPOLINE);
    }

    @Override
    public void loadRewarded() {
        ads_loadRewarded();
    }

    @Override
    public void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable) {
        pendingOnReward = onReward;
        pendingOnNotAvailable = onNotAvailable;
        ads_showRewarded(REWARDED_RESULT_TRAMPOLINE);
    }

    @Override
    public void showBanner() {
        ads_showBanner();
    }

    @Override
    public void hideBanner() {
        ads_hideBanner();
    }

    // -- native/AdsBridge.h bindings (compiled into the app; see the class javadoc) --

    @Bridge(symbol = "ads_initialize")
    private static native void ads_initialize();

    @Bridge(symbol = "ads_loadInterstitial")
    private static native void ads_loadInterstitial();

    @Bridge(symbol = "ads_showInterstitial")
    private static native void ads_showInterstitial(FunctionPtr onClosed);

    @Bridge(symbol = "ads_loadRewarded")
    private static native void ads_loadRewarded();

    @Bridge(symbol = "ads_showRewarded")
    private static native void ads_showRewarded(FunctionPtr onResult);

    @Bridge(symbol = "ads_showBanner")
    private static native void ads_showBanner();

    @Bridge(symbol = "ads_hideBanner")
    private static native void ads_hideBanner();

    // -- Callback trampolines: invoked directly from Objective-C (see AdsBridge.m) --

    @Callback
    private static void interstitialClosedTrampoline() {
        Runnable callback = pendingInterstitialClosed;
        pendingInterstitialClosed = null;
        if (callback != null) {
            callback.run();
        }
    }

    @Callback
    private static void rewardedResultTrampoline(int earned) {
        Runnable onReward = pendingOnReward;
        Runnable onNotAvailable = pendingOnNotAvailable;
        pendingOnReward = null;
        pendingOnNotAvailable = null;
        if (earned != 0) {
            if (onReward != null) {
                onReward.run();
            }
        } else if (onNotAvailable != null) {
            onNotAvailable.run();
        }
    }

    private static FunctionPtr trampoline(String methodName, Class<?>... paramTypes) {
        try {
            Method method = IosAdsService.class.getDeclaredMethod(methodName, paramTypes);
            return new FunctionPtr(method);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
