package com.moneyfirst.pourperfect.android;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.moneyfirst.pourperfect.ads.AdsService;

/**
 * Real {@link AdsService} implementation using the Google Mobile Ads SDK for Android. Uses
 * Google's published TEST ad unit IDs (see {@code res/values/ad_unit_ids.xml}) by default -
 * replace them with the real, store-configured IDs before release (README.md checklist).
 *
 * <p>Not buildable/verified in the sandbox this project was authored in (no Android SDK) -
 * written against the documented Google Mobile Ads SDK Java API, but needs a real Android
 * SDK/emulator to compile-check and run.
 */
public final class AndroidAdsService implements AdsService {

    private static final String TAG = "PourPerfectAds";

    private final Activity activity;
    private final String interstitialAdUnitId;
    private final String rewardedAdUnitId;
    private final String bannerAdUnitId;

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private AdView bannerAdView;

    public AndroidAdsService(Activity activity) {
        this.activity = activity;
        this.interstitialAdUnitId = activity.getString(R.string.ad_unit_id_interstitial);
        this.rewardedAdUnitId = activity.getString(R.string.ad_unit_id_rewarded);
        this.bannerAdUnitId = activity.getString(R.string.ad_unit_id_banner);

        MobileAds.initialize(activity, initializationStatus ->
                Log.i(TAG, "Mobile Ads SDK initialized: " + initializationStatus));
    }

    @Override
    public void loadInterstitial() {
        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(activity, interstitialAdUnitId, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                interstitialAd = ad;
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                Log.w(TAG, "interstitial failed to load: " + error.getMessage());
                interstitialAd = null;
            }
        });
    }

    @Override
    public void showInterstitialIfLoaded(Runnable onClosed) {
        if (interstitialAd == null) {
            onClosed.run(); // see AdsService javadoc - onClosed always fires, loaded or not
            return;
        }
        InterstitialAd adToShow = interstitialAd;
        interstitialAd = null;
        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                onClosed.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                Log.w(TAG, "interstitial failed to show: " + adError.getMessage());
                onClosed.run();
            }
        });
        adToShow.show(activity);
    }

    @Override
    public void loadRewarded() {
        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(activity, rewardedAdUnitId, request, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                Log.w(TAG, "rewarded ad failed to load: " + error.getMessage());
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
        RewardedAd adToShow = rewardedAd;
        rewardedAd = null;
        OnUserEarnedRewardListener rewardListener = rewardItem -> onReward.run();
        adToShow.show(activity, rewardListener);
    }

    @Override
    public void showBanner() {
        activity.runOnUiThread(() -> {
            if (bannerAdView == null) {
                bannerAdView = new AdView(activity);
                bannerAdView.setAdSize(AdSize.BANNER);
                bannerAdView.setAdUnitId(bannerAdUnitId);

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                ((FrameLayout) activity.getWindow().getDecorView()).addView(bannerAdView, params);

                bannerAdView.loadAd(new AdRequest.Builder().build());
            }
            bannerAdView.setVisibility(android.view.View.VISIBLE);
        });
    }

    @Override
    public void hideBanner() {
        activity.runOnUiThread(() -> {
            if (bannerAdView != null) {
                bannerAdView.setVisibility(android.view.View.GONE);
            }
        });
    }
}
