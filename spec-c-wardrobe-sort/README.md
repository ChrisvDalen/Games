# Wardrobe Sort (Spec C)

Timed drag-and-drop outfit-matching game with a 2D avatar-compositing engine,
built on libGDX for Android (native, via the Android backend) and iOS
(native, via the RoboVM/MobiVM backend) - not a WebView wrapper. Monetization
play: purely cosmetic garment packs, a remove-ads unlock, an
avatar-personalization ("photo avatar") feature-flag unlock, an
interstitial-after-every-round ad, and a rewarded-video path for +10s time
extensions or outfit hints.

See `/home/user/Games/docs/MONEY_FIRST_ARCHITECTURE.md` at the repo root for
the shared cross-spec architecture (the `AdsService`/`IapService` contracts,
the three-module layout, and each spec's ad-pacing rule).

## What's verified

**`core` compiles and its full test suite passes**, run from the repo root:

```bash
mvn -f spec-c-wardrobe-sort/core/pom.xml test
```

```
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

75 real JUnit 5 tests cover every pure-logic class: `RoundGenerator`
(tray-always-completable across 20+ seeds, difficulty-scaling bounds),
`AvatarCompositor` (correct/wrong-slot/wrong-garment drop handling, win
detection), `RoundTimer` (single-use rewarded extension), `HintSystem`
(never negative, reveals a garment actually in the tray, never repeats a
slot in one round), `AdPacingPolicy` (interstitial fires after every round,
never twice for the same round), `WardrobeIapCatalog`/`PlayerUnlocks`
(including the explicit test that purchasing every cosmetic pack does not
change `RoundGenerator`'s output for a fixed seed), and `GdxPayIapService`
(wired against an in-memory fake `PurchaseManager`, since this module has no
real network/store access here).

**`android` and `ios` Java/Objective-C sources are complete, real code** -
not stubs - but this sandbox has no Android SDK, no macOS/Xcode, and its
network proxy blocks `maven.google.com` (needed for
`play-services-ads`/`billingclient`), so their packaging steps could not be
fully executed here. What *was* verified in this sandbox:

- Every `pom.xml`, `AndroidManifest.xml`, `res/values/*.xml`, `robovm.xml`,
  and `Info.plist.xml` is well-formed XML.
- `mvn -f spec-c-wardrobe-sort/ios/pom.xml compile` **succeeds** - `IOSLauncher.java`
  and `IosAdsService.java` compile cleanly against the real
  `robovm-rt`/`robovm-cocoatouch`/`gdx-backend-robovm`/`gdx-pay-iosrobovm-apple`
  jars from Maven Central, confirming the RoboVM `FunctionPtr`/`@Bridge`/`@Callback`
  native-binding API, `IOSApplication.Delegate`, and
  `PurchaseManageriOSApple` usage is all correct against the real classes
  (not just plausible-looking code).
- `mvn -f spec-c-wardrobe-sort/android/pom.xml compile` correctly resolves
  `wardrobe-sort-core`, `gdx-backend-android`, the three `gdx-platform`
  natives classifiers, and `gdx-pay-android-googlebilling` from Maven
  Central, and correctly registers the `apk` packaging type. It fails only
  at `com.google.android.gms:play-services-ads` / `com.android.billingclient:billing`,
  both `403 Forbidden` from this sandbox's proxy when reaching
  `maven.google.com` (those two artifacts are published on Google's Maven
  repository, not Maven Central - `android/pom.xml` declares that
  repository; see the `<repositories>` block there). This is a network
  policy limitation of this sandbox, not a defect in the POM.

**Run once on a real machine before store submission:**

```bash
mvn -pl spec-c-wardrobe-sort/android -am package   # needs ANDROID_HOME / Android SDK + network access to maven.google.com
mvn -pl spec-c-wardrobe-sort/ios -am package        # needs macOS + Xcode + CocoaPods + the RoboVM toolchain
```

### A pre-existing cross-spec reactor issue (not in this directory)

`mvn -pl spec-c-wardrobe-sort/core -am test`, run from the repo root exactly
as specified, currently fails at the project-parsing stage - **not because
of anything under `spec-c-wardrobe-sort/`** - because
`spec-b-tower-peril/ios/pom.xml` declares `<packaging>ipa</packaging>`,
which `robovm-maven-plugin` 2.3.20 does not register as a recognized Maven
packaging type (it ships no `META-INF/plexus/components.xml`, unlike
`android-maven-plugin`'s `apk`). Maven's reactor-wide project collection
reads every module reachable from the root aggregator's `<modules>` list up
front - before `-pl`/`-am` filter anything - so one unparseable sibling POM
blocks this exact command for the whole repo, regardless of what's in
`spec-c-wardrobe-sort/`.

This module's own `ios/pom.xml` hit the identical problem during
development and was fixed by using `<packaging>jar</packaging>` (with
`robovm:create-ipa` bound to the `package` phase - its own documented
default binding) instead of `<packaging>ipa</packaging>`; see the comment
in `ios/pom.xml`. The same fix would resolve
`spec-b-tower-peril/ios/pom.xml`, but that file is outside this directory
and out of scope for this task. Until it's fixed there too, verify this
spec's `core` module with:

```bash
mvn -f spec-c-wardrobe-sort/core/pom.xml test
```

which does not need to touch the sibling specs at all (confirmed above).

## Architecture

```
spec-c-wardrobe-sort/
  pom.xml              parent: aggregates core/android/ios
  core/                pure-Java libGDX ApplicationListener + game logic + JUnit tests
  android/             AndroidApplication launcher, real AdsService (Google Mobile
                        Ads SDK) + real IapService (gdx-pay Google Play Billing)
  ios/                 IOSApplication.Delegate launcher, real IapService (gdx-pay
                        Apple StoreKit) + real AdsService via a native
                        Objective-C shim (native/AdsBridge.h/.m) around the
                        Google Mobile Ads iOS SDK
```

`core` is 100% platform-independent: `WardrobeSortGame` (the libGDX `Game`)
takes an `AdsService` and an `IapService` in its constructor, so gameplay
code never touches a platform SDK directly, and every rule that affects
money (ad pacing, hint economy, cosmetic-pack gating) lives in `core` where
it's unit-tested rather than scattered across platform launchers.

Rendering is fully procedural - `render/AvatarRenderer` draws the avatar
silhouette and every garment as flat `ShapeRenderer` shapes (rect/circle/
triangle) in the garment's own RGBA color, keyed off `Garment.shapeId()`. No
external art assets are needed to play a full round.

### Gameplay classes (`core/src/main/java/com/moneyfirst/wardrobesort/`)

- `GarmentSlot`, `Garment`, `GarmentCatalog` - the domain model and the
  fixed base garment set (5 per slot) `RoundGenerator` draws from, plus the
  three cosmetic packs' garments (never drawn into a timed round).
- `OutfitTarget`, `Round`, `RoundGenerator` - seeded target-outfit + tray
  generation with difficulty scaling (decoys 2→12, timer 45s→15s as rounds
  progress), always tray-complete by construction.
- `RoundTimer` - countdown with a single-use rewarded "+10s" extension.
- `AvatarState`, `AvatarCompositor` - pure drop-resolution logic (correct
  match / wrong-slot-type / wrong-garment) and win detection.
- `HintSystem` - hint balance that never goes negative, reveals a real tray
  position, never repeats a slot within one round.
- `AdPacingPolicy`, `RewardedPurpose`, `AdsService` - the interstitial-after-
  every-round / rewarded-for-time-or-hint pacing rules, and the shared
  `AdsService` contract from `docs/MONEY_FIRST_ARCHITECTURE.md`.
- `WardrobeIapCatalog`, `ProductType`, `PlayerUnlocks`, `IapService`,
  `GdxPayIapService` - the five purchasable products, the aggregated
  owned/unlocked view, and the thin wrapper around gdx-pay's
  `PurchaseManager`.
- `WardrobeSortGame` + `screens/*` + `render/AvatarRenderer` +
  `input/DragDropController` - the actual playable game: hub (round-select +
  shop entry + banner zone) → round (drag-and-drop + timer bar + avatar) →
  results (win/timeout) → shop (cosmetic packs + remove ads + avatar
  personalization).

## IAP catalog

| Product | Type | Reference price | Unlocks |
|---|---|---|---|
| `ws_pack_streetwear` | cosmetic pack | €0.99 | 5 cosmetic garments (freeplay/custom-outfit only) |
| `ws_pack_formal` | cosmetic pack | €2.99 | 5 cosmetic garments |
| `ws_pack_seasonal_bundle` | cosmetic pack | €4.99 | 10 cosmetic garments (winter + summer, one bundle) |
| `ws_remove_ads` | feature unlock | €2.99 | `PlayerUnlocks.isRemoveAdsActive()` flag |
| `ws_avatar_personalization` | feature unlock | €4.99 | `PlayerUnlocks.isAvatarPersonalizationActive()` flag |

Cosmetic packs are purely cosmetic by construction: `RoundGenerator` only
ever draws from `GarmentCatalog.BASE`, never from a cosmetic pack's
garments, so owning every pack in the game cannot change a timed round's
target outfit, tray, decoy count, or timer for a given seed -
`WardrobeIapCatalogTest#purchasingCosmeticPackNeverChangesRoundGeneratorOutputForAFixedSeed`
asserts this directly.

`ws_avatar_personalization` unlocks only a feature flag here. The actual
"upload your own photo, we turn it into your avatar" pipeline (photo
capture/upload + background removal + compositing it into the layered
avatar) is **out of scope for this engine module** - it would need a
separate backend service (image upload endpoint, a background-removal
model or third-party API, and a way to get the processed cutout back into
`AvatarRenderer`). `ios/Info.plist.xml` already declares
`NSCameraUsageDescription`/`NSPhotoLibraryUsageDescription` so the app is
ready for that feature's usage-description requirements the day it's added,
but no image processing code exists in this module.

## TODOs before store submission

- **Ad unit IDs**: `android/res/values/ad_unit_ids.xml` and the constants at
  the top of `ios/native/AdsBridge.m` are Google's published *test* ad unit
  IDs. Replace all six (3 per platform) with this app's real AdMob ad unit
  IDs.
- **AdMob application ID**: the `com.google.android.gms.ads.APPLICATION_ID`
  meta-data in `android/AndroidManifest.xml` and `GADApplicationIdentifier`
  in `ios/Info.plist.xml` are both Google's published *test* app ID
  (`ca-app-pub-3940256099942544~3347511713`). Replace both with this app's
  real AdMob app ID.
- **IAP product IDs**: configure `ws_pack_streetwear`, `ws_pack_formal`,
  `ws_pack_seasonal_bundle`, `ws_remove_ads`, and `ws_avatar_personalization`
  as real in-app products in Google Play Console and App Store Connect,
  with exactly these product IDs (they're referenced verbatim by
  `WardrobeIapCatalog` and `GdxPayIapService`'s `PurchaseManagerConfig`).
- **Art**: every garment and the avatar silhouette are procedurally-drawn
  flat shapes (`render/AvatarRenderer`) - there is no real garment/avatar
  artwork, app icon, or store listing art yet. `android/res/mipmap-hdpi/ic_launcher.png`
  is a placeholder generated for this deliverable, not final launcher art.
- **Photo avatar pipeline**: as noted above, `ws_avatar_personalization`
  only ships the purchase/unlock/feature-gating plumbing. The actual photo
  upload + background removal + avatar-compositing pipeline needs a
  separate backend service built outside this module.
- **iOS native ads shim**: `native/AdsBridge.h`/`.m` +
  `IosAdsService.java`'s RoboVM `FunctionPtr`/`@Callback` bindings compile
  clean at the Java level (verified above) but the actual Objective-C
  compile-and-link step, and the CocoaPods `Google-Mobile-Ads-SDK`
  resolution declared in `robovm.xml`, need a real macOS + Xcode + CocoaPods
  + RoboVM toolchain to build and confirm end-to-end - this is the one part
  of this spec that is fundamentally unverifiable in this sandbox.
- **Android IAP/Ads dependency resolution**: verify
  `mvn -pl spec-c-wardrobe-sort/android -am package` on a machine with
  network access to `https://maven.google.com` (blocked by this sandbox's
  proxy) and a full Android SDK.
