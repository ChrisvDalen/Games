# Tower Peril (Spec B)

Pin-pull rescue puzzle with a light auto-battler merge/gacha meta-layer.
libGDX multi-module Maven project targeting Android (via the libGDX Android
backend) and iOS (via the RoboVM/MobiVM backend) - native code on both
platforms, no WebView. Package base `com.moneyfirst.towerperil`; groupId
`com.moneyfirst`; artifacts `tower-peril-core` / `tower-peril-android` /
`tower-peril-ios`.

```
spec-b-tower-peril/
  pom.xml            aggregator (packaging=pom), parent = ../pom.xml
  core/              pure-Java game logic + JUnit 5 tests (verified here)
  android/           AndroidApplication launcher, real AdMob + Play Billing
  ios/               IOSApplication.Delegate launcher, StoreKit IAP,
                     native Objective-C ads shim
```

## What's verified

`core` compiles and its full test suite passes:

```bash
mvn -pl spec-b-tower-peril/core -am test   # run from the repo root
```

Result at the time of writing: **74 tests, 0 failures, 0 errors** (see
`PhysicsResolverTest`, `PinPullLevelGeneratorTest`, `GachaMergeSystemTest`,
`AutoBattleResolverTest`, `AdPacingPolicyTest`, `SeasonPassStateTest`,
`PlayerEconomyTest`, `TowerPerilIapCatalogTest`).

`android/` and `ios/` are complete, standard, real-SDK-calling Maven
projects, but their packaging (`mvn package`, producing an APK / IPA) has
**not** been executed in this sandbox - it has no Android SDK and no
macOS/Xcode/RoboVM toolchain. Both must be built once on a machine (or CI
runner) that has the relevant toolchain before a store submission.

## A dependency-version discrepancy (and how it was resolved)

The reactor root `pom.xml` (which this spec must not modify) pins
`gdx-pay.version` to `1.4.0`. As of this writing, Maven Central has never
published that version of `com.badlogicgames.gdxpay:gdx-pay-client` (or any
other `gdx-pay-*` artifact) - the newest real release is **1.3.13**
(confirmed against `repo.maven.apache.org`'s `maven-metadata.xml`).

Since editing the root `pom.xml` was out of scope, `spec-b-tower-peril/pom.xml`
overrides the inherited property:

```xml
<properties>
  <gdx-pay.version>1.3.13</gdx-pay.version>
</properties>
```

Every dependency declaration in `core`, `android` and `ios` still references
`${gdx-pay.version}` - nothing is hardcoded at the point of use - so bumping
this one line is all that's needed if/when a real `1.4.0` is published.
Sibling specs (A, C) depend on the same root property and will likely need
the same override.

While tracking this down, two related gdx-pay artifact-id corrections were
also needed (verified against Maven Central's actual directory listing, not
assumed from memory):

- Android backend is `gdx-pay-android-googlebilling` (one word, built on
  Google Play Billing Library v6+/`com.android.billingclient.api`, matching
  `${billing-client.version}` 7.1.1) - not `gdx-pay-android-google-play`
  (a different, older/deprecated artifact stuck at 0.12.1 on a defunct
  Billing v2 API).
- iOS backend is `gdx-pay-iosrobovm-apple` - not `gdx-pay-iosrobovm`.

Both are used correctly in `android/pom.xml` and `ios/pom.xml`.

### A second, related correction: `ios/pom.xml` uses `packaging=jar`, not `packaging=ipa`

Disassembling both the reactor-pinned `robovm-maven-plugin` 2.3.20 and the
newest available 2.3.26 (`META-INF/maven/plugin.xml` in each jar) shows the
plugin never registers `ipa` as a Maven packaging type at all - there's no
lifecycle-mapping/artifact-handler component for it. Declaring
`<packaging>ipa</packaging>` fails with `Unknown packaging: ipa` even for a
bare single-module project with no parent POM and no reactor involved at
all (verified directly, not just in this repo's tree - and it broke `mvn
-pl spec-b-tower-peril/core -am test` outright, since Maven's reactor graph
builder must parse every module's pom.xml up front). The plugin's actual,
documented usage - and what real libGDX+RoboVM project templates ship - is
standard `<packaging>jar</packaging>` (there **is** real Java to compile
here: `IOSLauncher`, `IosAdsService`) with the plugin's `create-ipa` goal
bound to the `package` phase via `<executions>`, which is exactly what
produces the `.ipa` as a build artifact. `ios/pom.xml` uses that working
form, with the reasoning documented inline.

## Gameplay implemented in `core`

- **Pin-pull rescue phase** (`rescue` package): `Grid`, `Pin`, `Character`,
  `Hazard`, a deterministic `PhysicsResolver.applyPull(Grid, Pin)` (pins sit
  at a line's edge; pulling one opens that whole row/column to the exit -
  characters nearer the exit than any hazard in their line slide out and are
  rescued, a hazard nearer the exit "catches" every character behind it in
  that pull, and anyone blocked only by a still-present pin from the other
  orientation just settles in place for a future pull), and a seeded
  `PinPullLevelGenerator` that brute-force-verifies (bounded by the small
  pin count per room, 3-5) a winning pull order exists before returning a
  level, retrying deterministically off the same seeded RNG otherwise.
- **Auto-battle phase** (`battle` package): `Unit` (rarity COMMON/RARE/
  EPIC/LEGENDARY x 4 types with a simple beats-cycle matchup table),
  `EnemyWave`, and `AutoBattleResolver.resolve(roster, wave, seed)` -
  roster power (with matchup multiplier) vs wave power, each with a small
  seeded +/-10% variance, fully deterministic for a given seed.
- **Gacha/merge** (`gacha` package): `GachaMergeSystem` - exact weighted
  rarity roll (60/25/12/3, cumulative-table `nextInt(100)`, no float drift)
  and merge-3-into-next-rarity, cascading through tiers, capped at
  LEGENDARY.
- **Ad pacing** (`ads` package): `AdsService` interface exactly matching
  `docs/MONEY_FIRST_ARCHITECTURE.md`, and `AdPacingPolicy` - interstitial
  after **every** pin-pull level (deliberately more aggressive than Spec
  A's every-3rd cadence), rewarded video for a battle "double loot" and a
  lost-character/failed-battle "revive".
- **Economy / IAP** (`economy` package): `PlayerEconomy` (gems + roster,
  every mutation guarded so the balance can never go negative),
  `SeasonPassState` (30-day renewal window, daily/weekly bonus-claim
  entitlements, pure function of caller-supplied epoch-day numbers so it's
  deterministically testable), `TowerPerilIapCatalog` enum
  (`STARTER_UNIT_PACK` / `GEMS_SMALL..HUGE` with an increasing bonus-%
  curve / `SEASON_PASS`, each with its own `apply(PlayerEconomy, long)`),
  and `IapService` - a thin wrapper around gdx-pay's `PurchaseManager`
  (built directly against the disassembled 1.3.13 API, not guessed) with
  `purchase`/`isOwned`/`restore`/`isSeasonPassActive`.
- **Screens** (`screens` package): `TowerPerilGame` (libGDX `Game`, takes
  `AdsService` + `IapService` in its constructor for platform injection and
  testability), `HubScreen`, `PinPullScreen`, `AutoBattleScreen`,
  `GachaScreen` - grid rendering and touch-to-pull, rescue-to-battle
  transition, animated battle resolution, gacha rolling - all drawn
  procedurally with `ShapeRenderer`/`BitmapFont`, no external art assets.

## Android module

`android/src/main/java/.../android/AndroidLauncher.java` wires
`TowerPerilGame` with:

- `AndroidAdsService` - real `com.google.android.gms.ads.*` calls
  (`MobileAds.initialize`, `InterstitialAd.load`/`show`,
  `RewardedAd.load`/`show`, an `AdView` banner), using Google's published
  TEST ad unit IDs by default (`ca-app-pub-3940256099942544/1033173712`
  interstitial, `/5224354917` rewarded, `/6300978111` banner - also mirrored
  in `res/values/ad_unit_ids.xml`).
- `IapService` wrapping gdx-pay's
  `com.badlogic.gdx.pay.android.googlebilling.PurchaseManagerGoogleBilling`,
  which itself talks to `com.android.billingclient.api.BillingClient`
  (Billing Library v6+/7.1.1). Subscriptions (the season pass) are handled
  automatically by this backend: any `Offer` registered with
  `OfferType.SUBSCRIPTION` in `IapService.buildConfig()` gets its
  `ProductDetails.SubscriptionOfferDetails` queried and purchased through
  the same `purchase(productId)`/`purchaseRestore()` calls as every
  consumable - no separate subscription-specific code path was needed.

`AndroidManifest.xml` declares `INTERNET`, `ACCESS_NETWORK_STATE`, and
`com.android.vending.BILLING`, plus the AdMob application id meta-data
(Google's TEST app id `ca-app-pub-3940256099942544~3347511713`, clearly
commented as needing replacement).

## iOS module

`ios/src/main/java/.../ios/IOSLauncher.java` wires `TowerPerilGame` with:

- `IapService` wrapping gdx-pay's
  `com.badlogic.gdx.pay.ios.apple.PurchaseManageriOSApple` (StoreKit).
  Auto-renewable subscriptions are just another `SKProduct` identifier to
  StoreKit, so `tp_season_pass` purchases/restores through the same calls
  as the gem packs once it's configured as an auto-renewable subscription
  in App Store Connect.
- `IosAdsService` - a Java class whose methods are bound via RoboVM's
  `@Bridge` native-method annotation to `extern "C"` functions
  (`ads_initialize`, `ads_loadInterstitial`, `ads_showInterstitial`,
  `ads_loadRewarded`, `ads_showRewarded`, `ads_showBanner`,
  `ads_hideBanner`) implemented in `native/AdsBridge.h`/`.m`, a small
  Objective-C shim around `GADInterstitialAd`/`GADRewardedAd`/
  `GADBannerView` from the `Google-Mobile-Ads-SDK` CocoaPod (declared in
  `robovm.xml`'s `<cocoapods>` section). Uses Google's real iOS TEST ad
  unit IDs (`ca-app-pub-3940256099942544/4411468910` interstitial,
  `/1712485313` rewarded, `/2934735716` banner), clearly commented as test
  IDs in `AdsBridge.m`.

`Info.plist.xml` sets the TEST `GADApplicationIdentifier`
(`ca-app-pub-3940256099942544~1458002511`), `NSUserTrackingUsageDescription`
(required since iOS 14.5 for any ad-serving app), and bundle id
`com.moneyfirst.towerperil`.

### Building the native ads bridge

`robovm.xml` references `native/libAdsBridge.a` in its `<libs>` - the
RoboVM Maven plugin links prebuilt static libraries/frameworks/pods rather
than compiling arbitrary Objective-C itself, so `native/AdsBridge.m` needs a
one-time (or CI) compile step before `mvn package` on a macOS/Xcode machine,
e.g.:

```bash
xcrun clang -c -fobjc-arc -arch arm64 \
  -isysroot "$(xcrun --sdk iphoneos --show-sdk-path)" \
  -Ipods/Headers/Public/Google-Mobile-Ads-SDK \
  ios/native/AdsBridge.m -o ios/native/AdsBridge.o
ar rcs ios/native/libAdsBridge.a ios/native/AdsBridge.o
```

(exact CocoaPods header search path depends on where `pod install`, run by
the RoboVM plugin itself, places `Google-Mobile-Ads-SDK`'s headers.)

### Known limitation: native ads shim has no real callback path yet

The `ads_*` C functions are intentionally fire-and-forget (no function
pointers/blocks cross the RoboVM native-method boundary in this first pass).
`IosAdsService` therefore calls its `onClosed`/`onReward` `Runnable`s right
after requesting the native show call, not after the real
`GADFullScreenContentDelegate` dismissal/reward event. This is flagged
explicitly as **the most fragile/incomplete piece of this spec** - a
follow-up should add a real native -> Java callback (e.g. a RoboVM
`@Callback`-annotated static method invoked from `TPAdsDelegate`) before
shipping a build that depends on rewarded-ad reward timing being accurate.
It's also the one piece of either platform module that, per the shared
architecture notes, needs a real device/simulator build to confirm at all.

## Known limitation: season-pass subscription handling

The IAP flow most likely to need real-device verification is the season
pass. `SeasonPassState` (pure Java, fully unit-tested) correctly models a
30-day renewal window and daily/weekly claim entitlements, and
`TowerPerilIapCatalog.SEASON_PASS.apply(...)` wires a successful purchase or
renewal into it - but the actual recurring-billing signal (Play Billing's
subscription renewal callback / StoreKit's subscription renewal
transaction) has to reach `SeasonPassState.renew(...)` from platform code
that isn't exercised by anything in this sandbox. Both platform backends
(`PurchaseManagerGoogleBilling`, `PurchaseManageriOSApple`) do handle
`OfferType.SUBSCRIPTION` offers natively, so the plumbing is in place - it
just hasn't been run against a real Play Console / App Store Connect
subscription product.

## TODOs before store submission

- Replace every TEST ad unit ID (Android `res/values/ad_unit_ids.xml` and
  `AndroidAdsService`; iOS `AdsBridge.m`) and both TEST AdMob application
  ids (`AndroidManifest.xml`, `Info.plist.xml`) with the real IDs from your
  AdMob console.
- Configure `tp_starter_units`, `tp_gems_small`, `tp_gems_medium`,
  `tp_gems_large`, `tp_gems_huge`, and the `tp_season_pass`
  auto-renewable subscription in both Play Console and App Store Connect,
  matching `TowerPerilIapCatalog`'s product ids exactly.
- Real app icons and store art (`ic_launcher` is a placeholder reference in
  `AndroidManifest.xml`; none is bundled here).
- **Gacha-odds legal disclosure**: Tower Peril's gem gacha (weighted
  COMMON/RARE/EPIC/LEGENDARY rolls) is a loot-box mechanic. A growing list
  of jurisdictions and both major app stores now require in-app disclosure
  of drop-rate odds (e.g. Apple App Store Review Guideline 3.1.1, Google
  Play's loot box disclosure policy, and South Korea/China/Belgium-style
  statutory requirements). `GachaMergeSystem`'s published weights
  (60/25/12/3) are exact and enforced in code, but no in-app odds disclosure
  UI exists yet - add one (and any per-region legal copy) before submitting
  to a store or region that requires it.
- Verify `mvn -pl spec-b-tower-peril/android -am package` and
  `mvn -pl spec-b-tower-peril/ios -am package` on machines with the Android
  SDK and macOS/Xcode/RoboVM respectively.
