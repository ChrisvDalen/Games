# Pour Perfect

Liquid-sort puzzle mobile game (libGDX, Android + iOS via RoboVM/MobiVM). Ads-volume
monetization: interstitial every 3rd solved level, rewarded video for extra tube / hint /
undo-all, banner on level-select. Part of the `money-first-specs` reactor at the repo root - see
`../docs/MONEY_FIRST_ARCHITECTURE.md` for the shared cross-spec architecture (`AdsService` /
`IapService` contracts, why libGDX+RoboVM, etc).

## Layout

```
spec-a-pour-perfect/
  pom.xml            parent: aggregates core/android/ios
  core/               pure-Java libGDX game logic + JUnit 5 tests - VERIFIED (see below)
  android/            Android launcher, real Google Mobile Ads SDK + Play Billing - NOT verified
  ios/                RoboVM launcher, real StoreKit IAP + native Ads SDK shim - NOT verified
  assets/             (nearly empty - rendering is procedural, see assets/README.md)
```

## What's verified here

`core` compiles and its full test suite passes, run from the repo root:

```bash
mvn -pl spec-a-pour-perfect/core -am test
```

113 JUnit 5 tests across 10 test classes, covering:

- `TubeTest` - pour legality (empty source, full target, color mismatch, self-pour, capping by
  target free space) and that pouring out of an already-complete tube is structurally legal
  (never useful, never forbidden - see `Tube`'s javadoc).
- `PuzzleSolverTest` - the BFS solver on hand-built states: already-won, one-move-from-won,
  a genuinely unsolvable configuration, a fully deadlocked (no legal moves) configuration, and
  that a zero search budget returns "no solution" rather than hanging.
- `LevelGeneratorTest` - **20 different random seeds**, each generated level solved end-to-end by
  an independent BFS pass and asserted to win; plus determinism (same seed -> identical level),
  exact per-color unit counts, the difficulty curve, and config validation.
- `GameSessionTest` - pour/undo/hint orchestration, undo-all, adding an extra tube mid-session,
  and that hint/undo credit counters never go negative no matter how many times they're consumed
  or granted a negative amount.
- `AdPacingPolicyTest` - interstitial fires on exactly the 3rd, 6th, 9th... solve (never more
  often), a custom cadence, and the stuck-player rewarded-offer trigger.
- `StreakTrackerTest` - day-boundary streak logic driven entirely by explicit epoch-day integers
  (never the real clock), including same-day replay, consecutive days, gaps, and best-streak
  persistence through a reset.
- `LevelProgressionStateTest`, `CafeProgressTest` - persisted-state save/load round-trips and
  non-negative credit/cup guards.
- `PourPerfectIapCatalogTest`, `GdxPayIapServiceTest` - each catalog product's `apply()` effect,
  and the real gdx-pay `PurchaseManager`/`PurchaseObserver` wiring exercised against a fake
  `PurchaseManager` that implements gdx-pay's actual interfaces (not just `core`'s own
  `IapService`), so this also validates our usage of the real gdx-pay-client API surface.

## What's NOT verified here

`android/` and `ios/` are complete, standard Maven modules calling real platform SDKs, but their
packaging step (`mvn package`) has **not** been run in this sandbox:

- `android/` needs the Android SDK (`ANDROID_HOME`) for `android-maven-plugin` to build an APK.
- `ios/` needs macOS + Xcode + the RoboVM/MobiVM toolchain to build an IPA - and, further, its
  native Objective-C shim (`ios/native/AdsBridge.h/.m`) needs to actually compile against the
  Google-Mobile-Ads-SDK CocoaPod. **This is the single piece of this whole spec with the most
  risk**: it was written against the documented Google Mobile Ads iOS SDK Objective-C API and
  RoboVM's documented `@Bridge` native-method mechanism, but neither could be compiled or run
  here. Everything else in `ios/` is standard, lower-risk RoboVM/libGDX/gdx-pay wiring.

Both module poms do parse and `mvn validate` cleanly from this sandbox (dependency coordinates
resolve, plugin configuration is well-formed) - `mvn -f spec-a-pour-perfect/pom.xml validate`
succeeds for all four modules (parent, core, android, ios). What's unverified is specifically the
actual native compilation/packaging step, not the Maven wiring around it.

## Two version corrections made to what the reactor pom specifies

Both are pinned locally (not by editing the shared root `pom.xml`, which the other two specs'
agents were working in parallel against) and are called out again inline as code comments:

- **`gdx-pay.version`**: the root pom declares `1.4.0`, which was verified against Maven Central
  (2026-08-25) to not exist for `com.badlogicgames.gdxpay:gdx-pay-client` (highest published
  version is `1.3.13`). Overridden to `1.3.13` in `spec-a-pour-perfect/pom.xml`'s own
  `<properties>` - every dependency still references `${gdx-pay.version}`, not a hardcoded
  literal, so bumping that one property is enough if the shared reactor pom is later corrected.
- **`gdx-pay-android-googleplay` -> `gdx-pay-android-googlebilling`**: the older artifact wraps
  Google's since-discontinued AIDL in-app-billing API (last released 2019); the maintained
  backend on top of the current Play Billing Library (matching this reactor's
  `billing-client.version`) is `gdx-pay-android-googlebilling`.

One structural correction: `robovm-maven-plugin` does not register a custom `ipa` Maven packaging
type (unlike `android-maven-plugin`'s `apk`) - `ios/pom.xml` uses plain `jar` packaging with the
plugin's `create-ipa` goal bound to the `package` phase, which is the standard RoboVM/MobiVM Maven
pattern.

## Before store submission

1. **Ad unit IDs**: replace every ID in `android/res/values/ad_unit_ids.xml`, the AdMob
   application ID meta-data in `android/AndroidManifest.xml`, the three ad unit ID constants in
   `ios/native/AdsBridge.m`, and the `GADApplicationIdentifier` in `ios/Info.plist.xml` - all
   currently use Google's published *test* IDs, which never earn real revenue.
2. **IAP product IDs**: `pp_remove_ads`, `pp_starter_pack`, `pp_cafe_expansion`
   (`PourPerfectIapCatalog`) need matching in-app products configured in both Play Console and
   App Store Connect before purchases will actually work.
3. **App icons / store art**: `android/res/drawable/ic_launcher.xml` is a placeholder procedural
   vector icon (consistent with the game's no-external-art-assets rendering approach) - replace
   with real, designer-made adaptive icon art per each store's icon guidelines, plus screenshots
   and store listing graphics.
4. **iOS signing**: `ios/pom.xml`'s `robovm.iosSignIdentity` property is left empty; set it (or
   pass `-Drobovm.iosSignIdentity=...`) on a real build machine with a valid signing identity.
5. Run `mvn -pl spec-a-pour-perfect/android -am package` and
   `mvn -pl spec-a-pour-perfect/ios -am package` at least once on machines with the Android SDK
   and Xcode/RoboVM respectively, before shipping either build.
