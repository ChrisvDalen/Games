package com.moneyfirst.pourperfect.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.pay.android.googlebilling.PurchaseManagerGoogleBilling;
import com.moneyfirst.pourperfect.PourPerfectGame;
import com.moneyfirst.pourperfect.iap.GdxPayIapService;

/**
 * Real Android entry point: wires {@link PourPerfectGame} with {@link AndroidAdsService} (Google
 * Mobile Ads SDK) and {@link GdxPayIapService} (gdx-pay's Google Play Billing backend), per
 * {@code docs/MONEY_FIRST_ARCHITECTURE.md}.
 *
 * <p><strong>Not buildable/verified in the sandbox this project was authored in</strong> (no
 * Android SDK) - written against the real, documented gdx-pay-android-googlebilling and Google
 * Mobile Ads SDK APIs, but needs a real Android SDK/emulator to compile-check and run. See
 * {@code ../README.md}.
 */
public class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;

        AndroidAdsService adsService = new AndroidAdsService(this);

        GdxPayIapService iapService = new GdxPayIapService(new PurchaseManagerGoogleBilling(this));
        iapService.install();
        // PourPerfectGame.create() attaches the app's canonical LevelProgressionState to
        // iapService itself once Gdx.app (and its Preferences access) exists - see
        // GdxPayIapService.attachProgressionState javadoc.

        PourPerfectGame game = new PourPerfectGame(adsService, iapService);
        initialize(game, config);
    }
}
