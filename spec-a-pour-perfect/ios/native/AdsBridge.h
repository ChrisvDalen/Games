//
//  AdsBridge.h
//  Pour Perfect
//
//  Thin Objective-C shim exposing the Google Mobile Ads iOS SDK (GADInterstitialAd /
//  GADRewardedAd / GADBannerView, from the Google-Mobile-Ads-SDK CocoaPod declared in
//  robovm.xml) as a set of plain C functions, so RoboVM's Java side (IosAdsService.java) can call
//  them via @Bridge-annotated native methods without needing generated RoboVM/MobiVM bindings for
//  the Ads SDK (none exist).
//
//  Async completion (an interstitial closing, a rewarded ad granting or failing to grant a
//  reward) is surfaced back to Java by POLLING rather than a native-to-Java callback: each event
//  sets a small internal flag/result that ads_pollInterstitialClosed()/ads_pollRewardResult()
//  return exactly once (consuming it) the next time Java asks. IosAdsService polls once per frame
//  (see PollingApplicationListener.java). This sidesteps needing to marshal Java function
//  pointers into native code for what is, in practice, a low-frequency event (an ad closing).
//
//  NOT compiled/verified in the sandbox this project was authored in (needs macOS + Xcode). See
//  ../../README.md's "known risks" section.
//
#import <Foundation/Foundation.h>

#ifdef __cplusplus
extern "C" {
#endif

// One-time setup: MobileAds start(), plus loading the first interstitial/rewarded ad.
void ads_initialize(void);

// Interstitial (test unit ca-app-pub-3940256099942544/4411468910 by default - see robovm.xml /
// Info.plist.xml comments; swap for the real ID before release).
void ads_loadInterstitial(void);
void ads_showInterstitial(void);
// Returns 1 exactly once (then resets to 0) the first poll after the shown interstitial closes
// (or immediately, if none was loaded when ads_showInterstitial was called - matching
// AdsService.showInterstitialIfLoaded's "onClosed always fires" contract). 0 otherwise.
int ads_pollInterstitialClosed(void);

// Rewarded video (test unit ca-app-pub-3940256099942544/1712485313 by default).
void ads_loadRewarded(void);
void ads_showRewarded(void);
// 0 = no result yet, 1 = reward earned (consumes/resets), 2 = not available / dismissed without
// earning a reward (consumes/resets). Exactly one non-zero result follows each ads_showRewarded
// call, matching AdsService.showRewardedIfLoaded's two-callback contract.
int ads_pollRewardResult(void);

// Banner (test unit ca-app-pub-3940256099942544/2934735716 by default), anchored to the bottom
// of the key window.
void ads_showBanner(void);
void ads_hideBanner(void);

#ifdef __cplusplus
}
#endif
