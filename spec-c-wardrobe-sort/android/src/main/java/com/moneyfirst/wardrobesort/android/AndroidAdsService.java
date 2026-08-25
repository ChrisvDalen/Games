package com.moneyfirst.wardrobesort.android;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.moneyfirst.wardrobesort.AdsService;
import com.moneyfirst.wardrobesort.R;

/**
 * Real {@link AdsService} implementation on top of the Google Mobile Ads
 * SDK for Android. Ad unit ids come from {@code res/values/ad_unit_ids.xml}
 * - Google's published TEST ids by default; swap that file's values for the
 * real store-configured ids before release (see android/README notes and
 * the top-level spec README).
 *
 * <p>All Google Mobile Ads SDK calls must happen on the UI thread; libGDX's
 * render loop runs on a separate GL thread, so every entry point here hops
 * onto the activity's UI thread via {@link Activity#runOnUiThread}.
 */
public final class AndroidAdsService implements AdsService {

    private static final String TAG = "WardrobeSortAds";

    private final Activity activity;
    private final String interstitialAdUnitId;
    private final String rewardedAdUnitId;
    private final String bannerAdUnitId;

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private AdView bannerAdView;
    private FrameLayout bannerContainer;

    public AndroidAdsService(Activity activity) {
        this.activity = activity;
        this.interstitialAdUnitId = activity.getString(R.string.admob_interstitial_ad_unit_id);
        this.rewardedAdUnitId = activity.getString(R.string.admob_rewarded_ad_unit_id);
        this.bannerAdUnitId = activity.getString(R.string.admob_banner_ad_unit_id);
    }

    @Override
    public void loadInterstitial() {
        activity.runOnUiThread(() -> InterstitialAd.load(activity, interstitialAdUnitId, new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd ad) {
                    interstitialAd = ad;
                }

                @Override
                public void onAdFailedToLoad(LoadAdError error) {
                    Log.w(TAG, "Interstitial failed to load: " + error.getMessage());
                    interstitialAd = null;
                }
            }));
    }

    @Override
    public void showInterstitialIfLoaded(Runnable onClosed) {
        activity.runOnUiThread(() -> {
            InterstitialAd ad = interstitialAd;
            if (ad == null) {
                onClosed.run();
                return;
            }
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    interstitialAd = null;
                    onClosed.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError error) {
                    interstitialAd = null;
                    onClosed.run();
                }
            });
            ad.show(activity);
        });
    }

    @Override
    public void loadRewarded() {
        activity.runOnUiThread(() -> RewardedAd.load(activity, rewardedAdUnitId, new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd ad) {
                    rewardedAd = ad;
                }

                @Override
                public void onAdFailedToLoad(LoadAdError error) {
                    Log.w(TAG, "Rewarded ad failed to load: " + error.getMessage());
                    rewardedAd = null;
                }
            }));
    }

    @Override
    public void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable) {
        activity.runOnUiThread(() -> {
            RewardedAd ad = rewardedAd;
            if (ad == null) {
                onNotAvailable.run();
                return;
            }
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    rewardedAd = null;
                }

                @Override
                public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError error) {
                    rewardedAd = null;
                    onNotAvailable.run();
                }
            });
            ad.show(activity, rewardItem -> onReward.run());
        });
    }

    @Override
    public void showBanner() {
        activity.runOnUiThread(() -> {
            if (bannerAdView == null) {
                bannerAdView = new AdView(activity);
                bannerAdView.setAdSize(AdSize.BANNER);
                bannerAdView.setAdUnitId(bannerAdUnitId);

                bannerContainer = new FrameLayout(activity);
                FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
                activity.addContentView(bannerContainer, containerParams);

                FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
                bannerContainer.addView(bannerAdView, adParams);

                bannerAdView.loadAd(new AdRequest.Builder().build());
            }
            bannerContainer.setVisibility(android.view.View.VISIBLE);
            bannerAdView.setVisibility(android.view.View.VISIBLE);
        });
    }

    @Override
    public void hideBanner() {
        activity.runOnUiThread(() -> {
            if (bannerContainer != null) {
                bannerContainer.setVisibility(android.view.View.GONE);
            }
        });
    }
}
