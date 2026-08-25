package com.moneyfirst.wardrobesort.ios;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.pay.ios.apple.PurchaseManageriOSApple;
import com.moneyfirst.wardrobesort.GdxPayIapService;
import com.moneyfirst.wardrobesort.IapService;
import com.moneyfirst.wardrobesort.WardrobeSortGame;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

/**
 * iOS entry point. Wires the real {@link IosAdsService} (the
 * {@code native/AdsBridge} Objective-C shim around the Google Mobile Ads
 * iOS SDK) and a {@link GdxPayIapService} built on gdx-pay's Apple App Store
 * (StoreKit) backend into {@link WardrobeSortGame}, then hands off to
 * libGDX/RoboVM.
 */
public class IOSLauncher extends IOSApplication.Delegate {

    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.orientationLandscape = false;
        config.orientationPortrait = true;

        IosAdsService adsService = new IosAdsService();

        IapService iapService = new GdxPayIapService(new PurchaseManageriOSApple());

        WardrobeSortGame game = new WardrobeSortGame(adsService, iapService);
        return new IOSApplication(game, config);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}
