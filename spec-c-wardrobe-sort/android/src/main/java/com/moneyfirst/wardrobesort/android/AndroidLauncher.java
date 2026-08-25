package com.moneyfirst.wardrobesort.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.ads.MobileAds;
import com.moneyfirst.wardrobesort.GdxPayIapService;
import com.moneyfirst.wardrobesort.IapService;
import com.moneyfirst.wardrobesort.WardrobeSortGame;

/**
 * Android entry point. Wires the real {@link AndroidAdsService} (Google
 * Mobile Ads SDK) and a {@link GdxPayIapService} built on gdx-pay's Google
 * Play Billing backend into {@link WardrobeSortGame}, then hands off to
 * libGDX.
 */
public class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;

        // Google Mobile Ads SDK must be initialized before any ad load/show call.
        MobileAds.initialize(this, initializationStatus -> { /* no-op: AndroidAdsService loads its own ads lazily */ });

        AndroidAdsService adsService = new AndroidAdsService(this);

        com.badlogic.gdx.pay.android.googlebilling.PurchaseManagerGoogleBilling purchaseManager =
            new com.badlogic.gdx.pay.android.googlebilling.PurchaseManagerGoogleBilling(this);
        IapService iapService = new GdxPayIapService(purchaseManager);

        WardrobeSortGame game = new WardrobeSortGame(adsService, iapService);
        initialize(game, config);
    }
}
