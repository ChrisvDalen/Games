//
//  AdsBridge.m
//  Pour Perfect
//
//  Implementation of the C shim declared in AdsBridge.h, backed by the Google Mobile Ads iOS SDK
//  (Google-Mobile-Ads-SDK CocoaPod - see robovm.xml). See AdsBridge.h for the polling design.
//
//  NOT compiled/verified in the sandbox this project was authored in (needs macOS + Xcode). See
//  ../../README.md's "known risks" section.
//
#import "AdsBridge.h"
#import <UIKit/UIKit.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

// TEST ad unit IDs (Google's published, publicly-documented iOS test units - see
// https://developers.google.com/admob/ios/test-ads). Replace with the real, store-configured IDs
// before release - see ../../README.md's before-submission checklist.
static NSString *const kInterstitialAdUnitID = @"ca-app-pub-3940256099942544/4411468910";
static NSString *const kRewardedAdUnitID = @"ca-app-pub-3940256099942544/1712485313";
static NSString *const kBannerAdUnitID = @"ca-app-pub-3940256099942544/2934735716";

typedef NS_ENUM(NSInteger, PPRewardResult) {
    PPRewardResultNone = 0,
    PPRewardResultEarned = 1,
    PPRewardResultNotAvailable = 2
};

@interface PPAdsBridgeController : NSObject <GADFullScreenContentDelegate>

@property(nonatomic, strong, nullable) GADInterstitialAd *interstitialAd;
@property(nonatomic, strong, nullable) GADRewardedAd *rewardedAd;
@property(nonatomic, strong, nullable) GADBannerView *bannerView;

@property(atomic, assign) BOOL interstitialClosedFlag;
@property(atomic, assign) PPRewardResult rewardResult;
@property(atomic, assign) BOOL rewardEarnedThisPresentation;

+ (instancetype)shared;
- (UIViewController *)rootViewController;

@end

@implementation PPAdsBridgeController

+ (instancetype)shared {
    static PPAdsBridgeController *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[PPAdsBridgeController alloc] init];
    });
    return instance;
}

- (UIViewController *)rootViewController {
    UIWindow *keyWindow = nil;
    for (UIScene *scene in UIApplication.sharedApplication.connectedScenes) {
        if ([scene isKindOfClass:[UIWindowScene class]]) {
            for (UIWindow *window in ((UIWindowScene *)scene).windows) {
                if (window.isKeyWindow) {
                    keyWindow = window;
                    break;
                }
            }
        }
    }
    return keyWindow.rootViewController;
}

- (void)loadInterstitial {
    GADRequest *request = [GADRequest request];
    __weak PPAdsBridgeController *weakSelf = self;
    [GADInterstitialAd loadWithAdUnitID:kInterstitialAdUnitID
                                 request:request
                       completionHandler:^(GADInterstitialAd *_Nullable ad, NSError *_Nullable error) {
        if (error) {
            NSLog(@"[PourPerfect] interstitial failed to load: %@", error.localizedDescription);
            weakSelf.interstitialAd = nil;
            return;
        }
        ad.fullScreenContentDelegate = weakSelf;
        weakSelf.interstitialAd = ad;
    }];
}

- (void)showInterstitial {
    if (self.interstitialAd == nil) {
        // Matches AdsService.showInterstitialIfLoaded's "onClosed always fires" contract.
        self.interstitialClosedFlag = YES;
        return;
    }
    [self.interstitialAd presentFromRootViewController:[self rootViewController]];
}

- (void)loadRewarded {
    GADRequest *request = [GADRequest request];
    __weak PPAdsBridgeController *weakSelf = self;
    [GADRewardedAd loadWithAdUnitID:kRewardedAdUnitID
                             request:request
                   completionHandler:^(GADRewardedAd *_Nullable ad, NSError *_Nullable error) {
        if (error) {
            NSLog(@"[PourPerfect] rewarded ad failed to load: %@", error.localizedDescription);
            weakSelf.rewardedAd = nil;
            return;
        }
        ad.fullScreenContentDelegate = weakSelf;
        weakSelf.rewardedAd = ad;
    }];
}

- (void)showRewarded {
    if (self.rewardedAd == nil) {
        self.rewardResult = PPRewardResultNotAvailable;
        return;
    }
    self.rewardEarnedThisPresentation = NO;
    __weak PPAdsBridgeController *weakSelf = self;
    [self.rewardedAd presentFromRootViewController:[self rootViewController]
                            userDidEarnRewardHandler:^{
        weakSelf.rewardEarnedThisPresentation = YES;
    }];
}

// -- GADFullScreenContentDelegate ------------------------------------------------------------

- (void)adDidDismissFullScreenContent:(id<GADFullScreenPresentingAd>)ad {
    if (ad == (id<GADFullScreenPresentingAd>)self.interstitialAd) {
        self.interstitialAd = nil;
        self.interstitialClosedFlag = YES;
    } else if (ad == (id<GADFullScreenPresentingAd>)self.rewardedAd) {
        self.rewardedAd = nil;
        self.rewardResult = self.rewardEarnedThisPresentation ? PPRewardResultEarned : PPRewardResultNotAvailable;
    }
}

- (void)ad:(id<GADFullScreenPresentingAd>)ad didFailToPresentFullScreenContentWithError:(NSError *)error {
    NSLog(@"[PourPerfect] ad failed to present: %@", error.localizedDescription);
    if (ad == (id<GADFullScreenPresentingAd>)self.interstitialAd) {
        self.interstitialAd = nil;
        self.interstitialClosedFlag = YES;
    } else if (ad == (id<GADFullScreenPresentingAd>)self.rewardedAd) {
        self.rewardedAd = nil;
        self.rewardResult = PPRewardResultNotAvailable;
    }
}

@end

#pragma mark - C bridge functions (declared in AdsBridge.h)

void ads_initialize(void) {
    [GADMobileAds.sharedInstance startWithCompletionHandler:nil];
}

void ads_loadInterstitial(void) {
    [[PPAdsBridgeController shared] loadInterstitial];
}

void ads_showInterstitial(void) {
    [[PPAdsBridgeController shared] showInterstitial];
}

int ads_pollInterstitialClosed(void) {
    PPAdsBridgeController *bridge = [PPAdsBridgeController shared];
    if (bridge.interstitialClosedFlag) {
        bridge.interstitialClosedFlag = NO;
        return 1;
    }
    return 0;
}

void ads_loadRewarded(void) {
    [[PPAdsBridgeController shared] loadRewarded];
}

void ads_showRewarded(void) {
    [[PPAdsBridgeController shared] showRewarded];
}

int ads_pollRewardResult(void) {
    PPAdsBridgeController *bridge = [PPAdsBridgeController shared];
    PPRewardResult result = bridge.rewardResult;
    if (result != PPRewardResultNone) {
        bridge.rewardResult = PPRewardResultNone;
    }
    return (int)result;
}

void ads_showBanner(void) {
    PPAdsBridgeController *bridge = [PPAdsBridgeController shared];
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *rootVC = [bridge rootViewController];
        if (bridge.bannerView == nil) {
            GADBannerView *bannerView = [[GADBannerView alloc] initWithAdSize:GADAdSizeBanner];
            bannerView.adUnitID = kBannerAdUnitID;
            bannerView.rootViewController = rootVC;

            CGFloat width = rootVC.view.bounds.size.width;
            CGFloat height = rootVC.view.bounds.size.height;
            bannerView.center = CGPointMake(width / 2.0, height - bannerView.frame.size.height / 2.0 - 8.0);

            [rootVC.view addSubview:bannerView];
            [bannerView loadRequest:[GADRequest request]];
            bridge.bannerView = bannerView;
        }
        bridge.bannerView.hidden = NO;
    });
}

void ads_hideBanner(void) {
    PPAdsBridgeController *bridge = [PPAdsBridgeController shared];
    dispatch_async(dispatch_get_main_queue(), ^{
        bridge.bannerView.hidden = YES;
    });
}
