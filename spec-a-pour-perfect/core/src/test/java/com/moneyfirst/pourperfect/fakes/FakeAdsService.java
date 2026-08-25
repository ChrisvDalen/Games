package com.moneyfirst.pourperfect.fakes;

import com.moneyfirst.pourperfect.ads.AdsService;

/** Deterministic {@link AdsService} test double: no real ad ever "loads" unless told to. */
public final class FakeAdsService implements AdsService {

    public int loadInterstitialCalls;
    public int loadRewardedCalls;
    public int showBannerCalls;
    public int hideBannerCalls;
    public int interstitialShowAttempts;
    public int rewardedShowAttempts;

    private boolean interstitialLoaded;
    private boolean rewardedLoaded;

    @Override
    public void loadInterstitial() {
        loadInterstitialCalls++;
        interstitialLoaded = true;
    }

    @Override
    public void showInterstitialIfLoaded(Runnable onClosed) {
        interstitialShowAttempts++;
        interstitialLoaded = false;
        onClosed.run(); // real implementations always fire this, loaded or not - see AdsService javadoc
    }

    @Override
    public void loadRewarded() {
        loadRewardedCalls++;
        rewardedLoaded = true;
    }

    @Override
    public void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable) {
        rewardedShowAttempts++;
        if (rewardedLoaded) {
            rewardedLoaded = false;
            onReward.run();
        } else {
            onNotAvailable.run();
        }
    }

    @Override
    public void showBanner() {
        showBannerCalls++;
    }

    @Override
    public void hideBanner() {
        hideBannerCalls++;
    }

    public boolean isInterstitialLoaded() {
        return interstitialLoaded;
    }

    public boolean isRewardedLoaded() {
        return rewardedLoaded;
    }

    public void setRewardedLoaded(boolean loaded) {
        this.rewardedLoaded = loaded;
    }
}
