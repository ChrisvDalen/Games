// AdsBridge.h
//
// Thin C shim around the Google-Mobile-Ads-SDK CocoaPod (GADInterstitialAd /
// GADRewardedAd / GADBannerView) so RoboVM's Java-side IosAdsService can call
// into it via plain `extern "C"` functions bound with RoboVM's @Bridge
// native-method mechanism - RoboVM has no generated Objective-C bindings for
// the Ads SDK, so a hand-written shim is the standard way to reach it.
//
// See README.md "Building the native ads bridge" for how AdsBridge.m gets
// compiled into native/libAdsBridge.a before `mvn package` runs (declared in
// robovm.xml's <libs>).

#ifndef TOWERPERIL_ADS_BRIDGE_H
#define TOWERPERIL_ADS_BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

/** Must be called once at app start (MobileAds start SDK). */
void ads_initialize(void);

void ads_loadInterstitial(void);

/** Presents the last-loaded interstitial, if any, from the root view controller. */
void ads_showInterstitial(void);

void ads_loadRewarded(void);

/** Presents the last-loaded rewarded ad, if any. */
void ads_showRewarded(void);

/** Adds (or re-shows) a bottom-anchored banner view and loads an ad into it. */
void ads_showBanner(void);

void ads_hideBanner(void);

#ifdef __cplusplus
}
#endif

#endif /* TOWERPERIL_ADS_BRIDGE_H */
