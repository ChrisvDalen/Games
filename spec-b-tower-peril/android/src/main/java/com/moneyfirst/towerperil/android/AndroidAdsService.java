package com.moneyfirst.towerperil.android;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.moneyfirst.towerperil.ads.AdsService;

/**
 * Real Google Mobile Ads SDK implementation of {@link AdsService}. Uses
 * Google's published TEST ad unit IDs by default - swap
 * {@link #INTERSTITIAL_AD_UNIT_ID}, {@link #REWARDED_AD_UNIT_ID} and
 * {@link #BANNER_AD_UNIT_ID} (or better, wire them from
 * {@code res/values/ad_unit_ids.xml}) for the real, store-configured ad unit
 * IDs before a release build.
 */
public final class AndroidAdsService implements AdsService {
    private static final String TAG = "TowerPerilAds";

    // Google's published TEST ad unit IDs (https://developers.google.com/admob/android/test-ads).
    // REPLACE with real ad unit IDs before release - see README.md.
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";

    private final Activity activity;
    private final FrameLayout rootLayout;

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private AdView bannerView;

    public AndroidAdsService(Activity activity, FrameLayout rootLayout) {
        this.activity = activity;
        this.rootLayout = rootLayout;
        MobileAds.initialize(activity, initializationStatus ->
                Log.i(TAG, "Mobile Ads SDK initialized: " + initializationStatus));
    }

    @Override
    public void loadInterstitial() {
        InterstitialAd.load(activity, INTERSTITIAL_AD_UNIT_ID, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.w(TAG, "Interstitial failed to load: " + loadAdError.getMessage());
                        interstitialAd = null;
                    }
                });
    }

    @Override
    public void showInterstitialIfLoaded(Runnable onClosed) {
        if (interstitialAd == null) {
            onClosed.run();
            return;
        }
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                loadInterstitial(); // pre-fetch the next one
                onClosed.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                interstitialAd = null;
                onClosed.run();
            }
        });
        interstitialAd.show(activity);
    }

    @Override
    public void loadRewarded() {
        RewardedAd.load(activity, REWARDED_AD_UNIT_ID, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.w(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                        rewardedAd = null;
                    }
                });
    }

    @Override
    public void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable) {
        if (rewardedAd == null) {
            onNotAvailable.run();
            return;
        }
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                loadRewarded(); // pre-fetch the next one
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                rewardedAd = null;
                onNotAvailable.run();
            }
        });
        rewardedAd.show(activity, rewardItem -> onReward.run());
    }

    @Override
    public void showBanner() {
        activity.runOnUiThread(() -> {
            if (bannerView == null) {
                bannerView = new AdView(activity);
                bannerView.setAdUnitId(BANNER_AD_UNIT_ID);
                bannerView.setAdSize(AdSize.BANNER);
                bannerView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.w(TAG, "Banner failed to load: " + adError.getMessage());
                    }
                });
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                rootLayout.addView(bannerView, params);
            }
            bannerView.setVisibility(android.view.View.VISIBLE);
            bannerView.loadAd(new AdRequest.Builder().build());
        });
    }

    @Override
    public void hideBanner() {
        activity.runOnUiThread(() -> {
            if (bannerView != null) {
                bannerView.setVisibility(android.view.View.GONE);
            }
        });
    }
}
