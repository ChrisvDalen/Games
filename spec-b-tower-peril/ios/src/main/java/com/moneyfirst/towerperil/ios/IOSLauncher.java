package com.moneyfirst.towerperil.ios;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.pay.ios.apple.PurchaseManageriOSApple;
import com.moneyfirst.towerperil.ads.AdsService;
import com.moneyfirst.towerperil.economy.IapService;
import com.moneyfirst.towerperil.screens.TowerPerilGame;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

/**
 * iOS entry point. Wires {@link TowerPerilGame} with a real
 * {@link IosAdsService} (native Google Mobile Ads shim) and a real
 * {@link IapService} backed by gdx-pay's Apple StoreKit implementation
 * ({@link PurchaseManageriOSApple}) - including subscription handling for
 * the season pass: StoreKit auto-renewable subscriptions are just another
 * {@code SKProduct} identifier, so the same {@code purchase}/{@code
 * purchaseRestore} calls used for consumable gem packs also drive
 * {@code tp_season_pass} once it's configured as an auto-renewable
 * subscription in App Store Connect (see README.md).
 */
public final class IOSLauncher extends IOSApplication.Delegate {

    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();

        AdsService adsService = new IosAdsService();
        IapService iapService = new IapService(new PurchaseManageriOSApple(), IapService.buildConfig());

        return new IOSApplication(new TowerPerilGame(adsService, iapService), config);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}
