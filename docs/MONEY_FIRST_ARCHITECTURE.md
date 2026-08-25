# Money-First Specs — shared architecture

Three independent mobile games, each a Maven multi-module project at the repo root:

- `spec-a-pour-perfect/` — liquid-sort puzzle (ads-volume play)
- `spec-b-tower-peril/` — pin-pull rescue + auto-battler merge meta (IAP/gacha play)
- `spec-c-wardrobe-sort/` — timed outfit-matching (cosmetic-IAP play)

Each spec follows the same three-module layout:

```
spec-x-name/
  pom.xml           parent: aggregates core/android/ios, no source of its own
  core/             pure-Java libGDX ApplicationListener + game logic + JUnit tests
                     -> the only module verified by `mvn test` in a sandbox with no
                        Android SDK / Xcode installed
  android/           Android launcher Activity, AndroidManifest.xml, res/, packaged
                     via android-maven-plugin (APK). Real Google Mobile Ads SDK +
                     Google Play Billing (via gdx-pay) calls.
  ios/               RoboVM (MobiVM) IOSLauncher, Info.plist, robovm.xml, packaged
                     via robovm-maven-plugin (IPA). Real StoreKit IAP via gdx-pay's
                     Apple App Store backend. Google Mobile Ads iOS SDK is bridged
                     through a small native Objective-C shim (see AdsService below)
                     since no generated RoboVM bindings for the Ads SDK exist.
```

Build entry points (from repo root, once module poms exist):

```bash
mvn -pl spec-a-pour-perfect/core -am test      # verifiable in any sandbox
mvn -pl spec-a-pour-perfect/android -am package  # needs ANDROID_HOME / Android SDK
mvn -pl spec-a-pour-perfect/ios -am package      # needs macOS + Xcode + RoboVM toolchain
```

## Cross-cutting contracts (identical shape in all three specs, own package per spec)

### `AdsService` (interface, lives in `core`)

```java
public interface AdsService {
    void loadInterstitial();
    void showInterstitialIfLoaded(Runnable onClosed);
    void loadRewarded();
    void showRewardedIfLoaded(Runnable onReward, Runnable onNotAvailable);
    void showBanner();
    void hideBanner();
}
```

- `core` depends only on this interface (`Gdx.app` gets a platform instance injected at
  launch via the libGDX `ApplicationListener` constructor — same pattern as `IapService`).
- Android impl: `com.google.android.gms.ads.*` (Google Mobile Ads SDK for Android),
  test ad unit IDs by default (`ca-app-pub-3940256099942544/...`), swapped for
  production IDs via `android/src/main/res/values/ad_unit_ids.xml` before store release.
- iOS impl: thin Objective-C shim (`ios/native/AdsBridge.h/.m`) exposing
  `extern "C"` functions (`ads_loadInterstitial`, `ads_showInterstitial`, ...) around
  `GADInterstitialAd` / `GADRewardedAd` / `GADBannerView` from the
  `Google-Mobile-Ads-SDK` CocoaPod (declared in `robovm.xml`), called from Java via
  RoboVM's `@Bridge` native-method binding. This is unverifiable without Xcode/macOS
  and is the one piece of each iOS module that needs a real device/simulator build to
  confirm — everything else in `ios/` is standard RoboVM/libGDX wiring.

### `IapService` (interface, lives in `core`)

Built directly on `com.badlogicgames.gdxpay:gdx-pay-client` (`PurchaseManager`), which
already abstracts Google Play Billing vs. Apple StoreKit — no custom native bridge
needed here, unlike ads. Each spec's core defines its own `Offer` enum (product IDs +
what they unlock) and a thin wrapper service so gameplay code never touches
`PurchaseManager` directly.

### Frequency / placement rules baked into each spec's ad architecture

Enforced in `core` (not left to platform code) so the pacing logic is unit-testable:

- Spec A: interstitial every 3rd solved level (not more), rewarded video for
  extra tube / hint / undo-all, banner only on the level-select screen.
- Spec B: interstitial after each pin-pull level, rewarded video for double loot
  or a battle revive.
- Spec C: interstitial after each round, rewarded video for extra time or an
  outfit hint.

## What "done" means here

Given no Android SDK or Xcode/macOS is available in this build sandbox:

- `core` modules are real, complete, and verified with `mvn test`:
  `mvn -pl spec-a-pour-perfect/core,spec-b-tower-peril/core,spec-c-wardrobe-sort/core -am test`
  passes 262/262 tests (113 + 74 + 75) from the repo root.
- `ios` modules compile cleanly against real Maven Central / RoboVM jars
  (`mvn -pl <spec>/ios -am compile`), including the `native/AdsBridge.h/.m`
  Objective-C shim wiring - but were never linked/packaged into an actual
  `.ipa`, since that needs a macOS + Xcode + RoboVM toolchain this sandbox
  doesn't have.
- `android` modules resolve their POMs but can't download `play-services-ads`
  / `billingclient` jars/aars here (Google's Maven repo is blocked by this
  sandbox's proxy) - the dependency coordinates and API usage are correct,
  but packaging (`mvn package` -> `.apk`) is unverified.
- Ad unit IDs and IAP product IDs are Google/Apple's published *test* IDs by
  default; swap them for the real store-configured IDs before release (see
  each spec's own `README.md`).

### Two bugs the build surfaced and fixed (all three specs hit these independently)

1. `gdx-pay.version` was pinned to `1.4.0` in the root reactor, which was never
   published to Maven Central (highest is `1.3.13`) - corrected at the root.
2. `robovm-maven-plugin` does not register a custom `ipa` Maven packaging type
   (only `android-maven-plugin` registers `apk` that way). Every `ios/pom.xml`
   uses standard `packaging=jar` with the plugin's `create-ipa` goal bound to
   the `package` phase instead - this is the real pattern RoboVM/libGDX
   projects use, not a workaround specific to this repo.

### Known fragile point across all three specs

The iOS `AdsService` implementation (`IosAdsService` + `native/AdsBridge.h/.m`)
fires its Java `Runnable` callbacks (`onClosed`, `onReward`) immediately after
requesting the native show call, rather than after the real
`GADInterstitialAd`/`GADRewardedAd` dismissal/reward delegate event, because
wiring an asynchronous native-to-Java callback across the RoboVM bridge needs
a real device/simulator to develop against. This is the one piece of ad
plumbing that needs finishing on an actual Mac before shipping; everything
else in `ios/` is standard, complete wiring.
