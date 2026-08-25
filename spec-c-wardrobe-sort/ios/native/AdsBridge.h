//
//  AdsBridge.h
//  Wardrobe Sort
//
//  Small Objective-C shim exposing plain C functions around the Google
//  Mobile Ads iOS SDK (GADInterstitialAd / GADRewardedAd / GADBannerView),
//  bound from Java via RoboVM's Bro bridge mechanism (see
//  IosAdsService.java). This is the one part of the iOS module that
//  needs a real Xcode/macOS build (with the Google-Mobile-Ads-SDK
//  CocoaPod resolved, per robovm.xml) to compile and confirm end-to-end -
//  everything else in ios/ is standard RoboVM/libGDX wiring.
//
//  Ad unit IDs used internally by AdsBridge.m are Google's published iOS
//  TEST ad unit IDs (see the comments there). Replace them with this app's
//  real ad unit IDs before release.
//

#ifndef AdsBridge_h
#define AdsBridge_h

#ifdef __cplusplus
extern "C" {
#endif

/** Called with no arguments when a presented interstitial has been dismissed (or failed to present). */
typedef void (*AdsVoidCallback)(void);

/** Called with earned=1 if the player watched the rewarded video to completion and earned the reward, earned=0 otherwise (dismissed early / failed to present). */
typedef void (*AdsRewardCallback)(int earned);

/** Initializes the Google Mobile Ads SDK. Call once, e.g. from IOSLauncher.createApplication(). Safe to call more than once. */
void ads_initialize(void);

/** Starts loading an interstitial ad in the background. */
void ads_loadInterstitial(void);

/** Presents the currently-loaded interstitial, if any, from the app's root view controller. `onClosed` always fires exactly once, whether or not an ad was actually shown. */
void ads_showInterstitial(AdsVoidCallback onClosed);

/** Starts loading a rewarded video ad in the background. */
void ads_loadRewarded(void);

/** Presents the currently-loaded rewarded video, if any. `onResult` always fires exactly once, with earned=1 only if the reward was actually granted. */
void ads_showRewarded(AdsRewardCallback onResult);

/** Creates (if needed) and shows a banner ad, anchored to the bottom of the key window. */
void ads_showBanner(void);

/** Hides the banner ad view created by ads_showBanner, if any. */
void ads_hideBanner(void);

#ifdef __cplusplus
}
#endif

#endif /* AdsBridge_h */
