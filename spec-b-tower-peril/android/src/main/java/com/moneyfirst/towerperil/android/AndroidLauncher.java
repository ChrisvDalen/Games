package com.moneyfirst.towerperil.android;

import android.os.Bundle;
import android.widget.FrameLayout;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.pay.android.googlebilling.PurchaseManagerGoogleBilling;
import com.moneyfirst.towerperil.ads.AdsService;
import com.moneyfirst.towerperil.economy.IapService;
import com.moneyfirst.towerperil.screens.TowerPerilGame;

/**
 * Android entry point. Wires {@link TowerPerilGame} with a real
 * {@link AndroidAdsService} (Google Mobile Ads SDK) and a real
 * {@link IapService} backed by gdx-pay's Google Play Billing implementation
 * ({@link PurchaseManagerGoogleBilling}, built on Billing Library v6+/
 * {@code com.android.billingclient.api}) - including subscription handling
 * for the season pass: {@code PurchaseManagerGoogleBilling} queries
 * {@code ProductDetails.SubscriptionOfferDetails} for any offer registered
 * with {@code OfferType.SUBSCRIPTION} (see {@link IapService#buildConfig()}),
 * so the season-pass product is purchased/restored through the exact same
 * {@code purchase}/{@code purchaseRestore} calls as every consumable gem
 * pack - no separate subscription code path is needed here.
 */
public class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;

        FrameLayout rootLayout = new FrameLayout(this);

        AdsService adsService = new AndroidAdsService(this, rootLayout);
        IapService iapService = new IapService(new PurchaseManagerGoogleBilling(this), IapService.buildConfig());

        android.view.View gameView = initializeForView(new TowerPerilGame(adsService, iapService), config);
        rootLayout.addView(gameView);
        setContentView(rootLayout);
    }
}
