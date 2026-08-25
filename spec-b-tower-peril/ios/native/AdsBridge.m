// AdsBridge.m
//
// Implementation of the extern "C" functions declared in AdsBridge.h, built
// on the Google-Mobile-Ads-SDK CocoaPod's GADInterstitialAd / GADRewardedAd /
// GADBannerView APIs.
//
// NOTE on callbacks (see README.md "iOS native ads shim - known limitation"):
// the C surface here is intentionally fire-and-forget (no function pointers
// or blocks cross the RoboVM native-method boundary). ads_showInterstitial()
// and ads_showRewarded() present synchronously-launched, asynchronously-
// dismissed ad UI; this shim tracks "loaded" state internally and reloads
// the next ad on dismissal, but it does not call back into Java when the
// user actually closes the ad or earns a reward. IosAdsService.java's
// onClosed/onReward Runnables therefore fire immediately after requesting
// the show, not after the real GAD delegate callback. Wiring a true
// native -> Java callback (e.g. via a RoboVM @Callback static method invoked
// from these delegates) is a natural next step before shipping - flagged
// explicitly as the most fragile/incomplete piece of this module.

#import <UIKit/UIKit.h>
#import <GoogleMobileAds/GoogleMobileAds.h>
#import "AdsBridge.h"

// Google's published TEST ad unit IDs (https://developers.google.com/admob/ios/test-ads).
// REPLACE with real ad unit IDs before release - see README.md.
static NSString *const kInterstitialAdUnitId = @"ca-app-pub-3940256099942544/4411468910";
static NSString *const kRewardedAdUnitId = @"ca-app-pub-3940256099942544/1712485313";
static NSString *const kBannerAdUnitId = @"ca-app-pub-3940256099942544/2934735716";

@interface TPAdsDelegate : NSObject <GADFullScreenContentDelegate, GADBannerViewDelegate>
@end

@implementation TPAdsDelegate

- (void)adDidDismissFullScreenContent:(nonnull id<GADFullScreenPresentingContent>)ad {
    // See the file-level NOTE: a production build should notify Java here.
}

- (void)ad:(nonnull id<GADFullScreenPresentingContent>)ad
    didFailToPresentFullScreenContentWithError:(nonnull NSError *)error {
    NSLog(@"TowerPeril ads: failed to present full screen content: %@", error);
}

@end

static TPAdsDelegate *sDelegate;
static GADInterstitialAd *sInterstitialAd;
static GADRewardedAd *sRewardedAd;
static GADBannerView *sBannerView;

static UIViewController *TPRootViewController(void) {
    UIWindow *keyWindow = nil;
    for (UIWindow *window in [UIApplication sharedApplication].windows) {
        if (window.isKeyWindow) {
            keyWindow = window;
            break;
        }
    }
    if (keyWindow == nil) {
        keyWindow = [UIApplication sharedApplication].windows.firstObject;
    }
    return keyWindow.rootViewController;
}

void ads_initialize(void) {
    if (sDelegate == nil) {
        sDelegate = [TPAdsDelegate new];
    }
    [GADMobileAds.sharedInstance startWithCompletionHandler:^(GADInitializationStatus *status) {
        NSLog(@"TowerPeril ads: Mobile Ads SDK initialized");
    }];
}

void ads_loadInterstitial(void) {
    GADRequest *request = [GADRequest request];
    [GADInterstitialAd loadWithAdUnitID:kInterstitialAdUnitId
                                 request:request
                       completionHandler:^(GADInterstitialAd *ad, NSError *error) {
        if (error != nil) {
            NSLog(@"TowerPeril ads: interstitial failed to load: %@", error);
            sInterstitialAd = nil;
            return;
        }
        ad.fullScreenContentDelegate = sDelegate;
        sInterstitialAd = ad;
    }];
}

void ads_showInterstitial(void) {
    UIViewController *root = TPRootViewController();
    if (sInterstitialAd != nil && root != nil) {
        [sInterstitialAd presentFromRootViewController:root];
        sInterstitialAd = nil;
        ads_loadInterstitial(); // pre-fetch the next one
    }
}

void ads_loadRewarded(void) {
    GADRequest *request = [GADRequest request];
    [GADRewardedAd loadWithAdUnitID:kRewardedAdUnitId
                             request:request
                   completionHandler:^(GADRewardedAd *ad, NSError *error) {
        if (error != nil) {
            NSLog(@"TowerPeril ads: rewarded ad failed to load: %@", error);
            sRewardedAd = nil;
            return;
        }
        ad.fullScreenContentDelegate = sDelegate;
        sRewardedAd = ad;
    }];
}

void ads_showRewarded(void) {
    UIViewController *root = TPRootViewController();
    if (sRewardedAd != nil && root != nil) {
        [sRewardedAd presentFromRootViewController:root
                                userDidEarnRewardHandler:^{
            NSLog(@"TowerPeril ads: user earned reward");
        }];
        sRewardedAd = nil;
        ads_loadRewarded(); // pre-fetch the next one
    }
}

void ads_showBanner(void) {
    UIViewController *root = TPRootViewController();
    if (root == nil) {
        return;
    }
    if (sBannerView == nil) {
        sBannerView = [[GADBannerView alloc] initWithAdSize:GADAdSizeBanner];
        sBannerView.adUnitID = kBannerAdUnitId;
        sBannerView.rootViewController = root;
        sBannerView.delegate = sDelegate;
        sBannerView.translatesAutoresizingMaskIntoConstraints = NO;
        [root.view addSubview:sBannerView];

        [NSLayoutConstraint activateConstraints:@[
            [sBannerView.centerXAnchor constraintEqualToAnchor:root.view.centerXAnchor],
            [sBannerView.bottomAnchor constraintEqualToAnchor:root.view.safeAreaLayoutGuide.bottomAnchor]
        ]];
    }
    sBannerView.hidden = NO;
    [sBannerView loadRequest:[GADRequest request]];
}

void ads_hideBanner(void) {
    sBannerView.hidden = YES;
}
