package com.moneyfirst.pourperfect.ios;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.pay.ios.apple.PurchaseManageriOSApple;
import com.moneyfirst.pourperfect.PourPerfectGame;
import com.moneyfirst.pourperfect.iap.GdxPayIapService;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

/**
 * Real iOS entry point: wires {@link PourPerfectGame} with {@link IosAdsService} (a thin
 * Objective-C shim over the Google Mobile Ads iOS SDK - see {@code native/AdsBridge.h/.m}) and
 * {@link GdxPayIapService} (gdx-pay's Apple App Store/StoreKit backend), per
 * {@code docs/MONEY_FIRST_ARCHITECTURE.md}.
 *
 * <p><strong>Not buildable/verified in the sandbox this project was authored in</strong> (needs
 * macOS + Xcode + the RoboVM/MobiVM toolchain) - written against the real, documented
 * gdx-pay-iosrobovm-apple and libGDX RoboVM backend APIs, but needs that real toolchain to
 * compile-check and run. See {@code ../README.md}.
 */
public class IOSLauncher extends IOSApplication.Delegate {

    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.orientationPortrait = true;
        config.orientationLandscape = false;

        IosAdsService adsService = new IosAdsService();

        GdxPayIapService iapService = new GdxPayIapService(new PurchaseManageriOSApple());
        iapService.install();
        // PourPerfectGame.create() attaches the app's canonical LevelProgressionState to
        // iapService itself once Gdx.app (and its Preferences access) exists - see
        // GdxPayIapService.attachProgressionState javadoc.

        PourPerfectGame game = new PourPerfectGame(adsService, iapService);
        return new IOSApplication(game, config);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}
