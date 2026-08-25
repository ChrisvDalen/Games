//
//  AdsBridge.m
//  Wardrobe Sort
//
//  See AdsBridge.h. Requires the Google-Mobile-Ads-SDK CocoaPod (declared in
//  robovm.xml) to compile.
//

#import "AdsBridge.h"
#import <UIKit/UIKit.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

// Google's published iOS TEST ad unit IDs
// (https://developers.google.com/admob/ios/test-ads). Safe to ship during
// development - they always serve test creatives. TODO before release:
// replace all three with this app's real ad unit IDs from the AdMob
// console (and GADApplicationIdentifier in Info.plist.xml).
static NSString * const kInterstitialAdUnitID = @"ca-app-pub-3940256099942544/4411468910";
static NSString * const kRewardedAdUnitID = @"ca-app-pub-3940256099942544/1712485313";
static NSString * const kBannerAdUnitID = @"ca-app-pub-3940256099942544/2934735716";

#pragma mark - Root view controller / window helpers

static UIWindow *ads_keyWindow(void) {
    if (@available(iOS 13.0, *)) {
        for (UIScene *scene in [UIApplication sharedApplication].connectedScenes) {
            if ([scene isKindOfClass:[UIWindowScene class]] && scene.activationState == UISceneActivationStateForegroundActive) {
                UIWindowScene *windowScene = (UIWindowScene *)scene;
                for (UIWindow *window in windowScene.windows) {
                    if (window.isKeyWindow) {
                        return window;
                    }
                }
            }
        }
    }
    return [UIApplication sharedApplication].delegate.window;
}

static UIViewController *ads_rootViewController(void) {
    return ads_keyWindow().rootViewController;
}

#pragma mark - Full-screen content delegate (interstitial + rewarded dismissal)

/**
 * One shared delegate object services whichever full-screen ad (interstitial
 * or rewarded) is currently presented. Since Wardrobe Sort's ad pacing never
 * shows an interstitial and a rewarded video at the same time, a single
 * instance is sufficient.
 */
@interface WSFullScreenContentDelegate : NSObject <GADFullScreenContentDelegate>
@property(nonatomic, assign) AdsVoidCallback pendingInterstitialClosedCallback;
@property(nonatomic, assign) AdsRewardCallback pendingRewardedResultCallback;
@property(nonatomic, assign) BOOL rewardEarnedThisPresentation;
@end

@implementation WSFullScreenContentDelegate

- (void)adDidDismissFullScreenContent:(id<GADFullScreenPresentingAd>)ad {
    [self fireCallbacksAfterDismissOrFailure];
}

- (void)ad:(id<GADFullScreenPresentingAd>)ad didFailToPresentFullScreenContentWithError:(NSError *)error {
    [self fireCallbacksAfterDismissOrFailure];
}

- (void)fireCallbacksAfterDismissOrFailure {
    if (self.pendingInterstitialClosedCallback != NULL) {
        AdsVoidCallback cb = self.pendingInterstitialClosedCallback;
        self.pendingInterstitialClosedCallback = NULL;
        cb();
    }
    if (self.pendingRewardedResultCallback != NULL) {
        AdsRewardCallback cb = self.pendingRewardedResultCallback;
        self.pendingRewardedResultCallback = NULL;
        cb(self.rewardEarnedThisPresentation ? 1 : 0);
        self.rewardEarnedThisPresentation = NO;
    }
}

@end

static WSFullScreenContentDelegate *ads_fullScreenDelegate(void) {
    static WSFullScreenContentDelegate *delegate = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        delegate = [[WSFullScreenContentDelegate alloc] init];
    });
    return delegate;
}

#pragma mark - State

static GADInterstitialAd *g_interstitialAd = nil;
static GADRewardedAd *g_rewardedAd = nil;
static GADBannerView *g_bannerView = nil;

#pragma mark - Public API

void ads_initialize(void) {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        [GADMobileAds.sharedInstance startWithCompletionHandler:nil];
    });
}

void ads_loadInterstitial(void) {
    GADRequest *request = [GADRequest request];
    [GADInterstitialAd loadWithAdUnitID:kInterstitialAdUnitID
                                 request:request
                       completionHandler:^(GADInterstitialAd *ad, NSError *error) {
        if (error != nil) {
            NSLog(@"WardrobeSort: interstitial failed to load: %@", error.localizedDescription);
            g_interstitialAd = nil;
            return;
        }
        g_interstitialAd = ad;
        ad.fullScreenContentDelegate = ads_fullScreenDelegate();
    }];
}

void ads_showInterstitial(AdsVoidCallback onClosed) {
    if (g_interstitialAd == nil) {
        if (onClosed != NULL) {
            onClosed();
        }
        return;
    }
    ads_fullScreenDelegate().pendingInterstitialClosedCallback = onClosed;
    UIViewController *root = ads_rootViewController();
    if (root == nil) {
        g_interstitialAd = nil;
        if (onClosed != NULL) {
            onClosed();
        }
        return;
    }
    [g_interstitialAd presentFromRootViewController:root];
    g_interstitialAd = nil; // GADInterstitialAd instances are single-use.
}

void ads_loadRewarded(void) {
    GADRequest *request = [GADRequest request];
    [GADRewardedAd loadWithAdUnitID:kRewardedAdUnitID
                             request:request
                   completionHandler:^(GADRewardedAd *ad, NSError *error) {
        if (error != nil) {
            NSLog(@"WardrobeSort: rewarded ad failed to load: %@", error.localizedDescription);
            g_rewardedAd = nil;
            return;
        }
        g_rewardedAd = ad;
        ad.fullScreenContentDelegate = ads_fullScreenDelegate();
    }];
}

void ads_showRewarded(AdsRewardCallback onResult) {
    if (g_rewardedAd == nil) {
        if (onResult != NULL) {
            onResult(0);
        }
        return;
    }
    UIViewController *root = ads_rootViewController();
    if (root == nil) {
        g_rewardedAd = nil;
        if (onResult != NULL) {
            onResult(0);
        }
        return;
    }
    ads_fullScreenDelegate().pendingRewardedResultCallback = onResult;
    ads_fullScreenDelegate().rewardEarnedThisPresentation = NO;
    [g_rewardedAd presentFromRootViewController:root
                        userDidEarnRewardHandler:^{
        ads_fullScreenDelegate().rewardEarnedThisPresentation = YES;
    }];
    g_rewardedAd = nil; // GADRewardedAd instances are single-use.
}

void ads_showBanner(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = ads_rootViewController();
        if (root == nil) {
            return;
        }
        if (g_bannerView == nil) {
            g_bannerView = [[GADBannerView alloc] initWithAdSize:GADAdSizeBanner];
            g_bannerView.adUnitID = kBannerAdUnitID;
            g_bannerView.rootViewController = root;

            CGFloat bannerWidth = g_bannerView.frame.size.width;
            CGFloat bannerHeight = g_bannerView.frame.size.height;
            CGRect screenBounds = root.view.bounds;
            g_bannerView.frame = CGRectMake(
                (screenBounds.size.width - bannerWidth) / 2.0,
                screenBounds.size.height - bannerHeight,
                bannerWidth, bannerHeight);
            g_bannerView.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleWidth;

            [root.view addSubview:g_bannerView];
            [g_bannerView loadRequest:[GADRequest request]];
        }
        g_bannerView.hidden = NO;
    });
}

void ads_hideBanner(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        g_bannerView.hidden = YES;
    });
}
