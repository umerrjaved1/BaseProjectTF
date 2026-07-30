# BaseProject — Android app template

Template used to spin up new Android apps. It ships the parts every app needs and nothing
app-specific: startup flow, language picker, onboarding, survey, paywall + Play Billing,
Firebase Remote Config, in-app update, Room scaffolding, and a Retrofit stack.

All branding is **placeholder** (`app_name` is literally `BaseProject`). Everything you must
replace is listed in [Spinning up a new app](#spinning-up-a-new-app).

- `namespace` == `applicationId` == `com.professor.baseproject` — keep them equal.
- UI is **Views + ViewBinding**. Not Compose.
- Single `:app` module.

---

## Prerequisites

| Requirement | Notes |
|---|---|
JDK 17 | `jvmToolchain(17)`; Android Studio's bundled JBR works |
Android SDK, `compileSdk`/`targetSdk` 37 | matches AGP 9.2.1 |
GitHub Packages credentials | **required** — see below |

### GitHub Packages credentials (build fails without these)

The ads library `com.umer_tf.ads:ads` is resolved from a private GitHub Packages repo
declared in [`settings.gradle.kts`](settings.gradle.kts). Add to `local.properties`
(gitignored, never commit):

```properties
gpr.user=<your-github-username>
gpr.key=<a-github-PAT-with-read:packages>
```

> **Gotcha:** if the artifact is already in your local Gradle cache the build succeeds
> *without* these keys, so a machine that has built before will not reveal the problem. A
> fresh clone, a new machine, or `--refresh-dependencies` will fail with
> `Could not resolve com.umer_tf.ads:ads`. Set them before onboarding anyone new.

Optional, only for API-backed forks:

```properties
API_KEY=...
BASE_URL=https://api.example.com/
```

Both are exposed as `BuildConfig` fields. If unset, `API_KEY` is the literal string
`"null"` and `BASE_URL` falls back to a placeholder host. That is harmless while nothing
injects `ApiService` — Hilt providers are lazy — but set them before using the network layer.

---

## Build variants

```bash
./gradlew :app:assembleDebug          # day-to-day
./gradlew :app:assembleMinifiedDebug  # R8-processed AND debug-signed → runnable locally
./gradlew :app:assembleRelease        # R8-processed (see Signing below)
./gradlew :app:testDebugUnitTest      # JVM tests
./gradlew :app:connectedDebugAndroidTest   # instrumented; includes the Room check
```

**Run `minifiedDebug` before every release.** `release` is minified but *unsigned*, so the
shrunk code path is easy to never actually execute. Several ProGuard rules hardcode the
package name (see [ProGuard](#proguard)) and fail silently in release only —
`minifiedDebug` is how you catch that.

All build types share one `applicationId`, so **debug and release overwrite each other on
a device**. This is deliberate: `app/google-services.json` declares exactly one client, and
the Google Services plugin hard-fails on any other id. To install side by side, register
`com.professor.baseproject.debug` in Firebase, re-download `google-services.json`, then set
`applicationIdSuffix = ".debug"` on the `debug` build type.

### Signing

`release` currently sets **no** `signingConfig`, and the `signingConfigs.release` block
points at `../keystore/AppKeyStore.jks` while the file on disk is `keystore/AppKeyStore`
(no extension). Release output is therefore unsigned until you wire this up.

> **Security note.** `keystore/AppKeyStore` and `keystore/password.txt` are committed, and
> the store/key passwords are in plaintext in `app/build.gradle.kts`. The same key is used
> for every app built from this template, so a single leak affects the whole portfolio, and
> the password is in git history — deleting the file later does not remove it. If Play App
> Signing is enabled this is an upload key and is recoverable; if not, it is the app signing
> key and cannot be rotated. This is a known, accepted trade-off in this repo.

---

## Spinning up a new app

There is no automation yet — this is a manual checklist. Work top to bottom.

### 1. Identity

| # | What | Where |
|---|---|---|
1 | `namespace` and `applicationId` (**keep identical**) | `app/build.gradle.kts` (2 lines) |
2 | Source package directory | `app/src/main/java/com/professor/baseproject/` → rename, plus `app/src/test/...` and `app/src/androidTest/...` |
3 | `rootProject.name` | `settings.gradle.kts` |
4 | `versionCode` / `versionName` | `app/build.gradle.kts` — not automated; bump by hand each release |

Renaming the package also requires updating:

- `app/proguard-rules.pro` — 5 rules hardcode the package (`di`, `model`, `data.source.api`,
  `remoteconfig.data`, `databinding`). **They stop matching silently after a rename**, and R8
  then obfuscates your Gson/Room models — a release-only crash. Prefer `@Keep` on the classes
  themselves; see `model/DataModel.kt`.
- `app/src/main/res/xml/shortcuts.xml` — `targetPackage` and `targetClass`. Resource XML gets
  no `manifestPlaceholder` substitution, so these are hardcoded.
- `app/src/main/res/layout/activity_start.xml` — `tools:context`.

### 2. Firebase

5. Create the Firebase project, register the Android app with your `applicationId`, download
   `google-services.json` → `app/google-services.json` (**replace** the committed one).
6. Upload the Remote Config values — see [Remote Config](#remote-config) and [Ads](#ads).

### 3. Branding and copy

All in `app/src/main/res/values/strings.xml` unless noted:

| # | Key | Current placeholder |
|---|---|---|
7 | `app_name` | `BaseProject` |
8 | `splash_description` | `Your app tagline goes here` |
9 | `onboarding_title_1..3` | `Onboarding headline one/two/three` |
10 | `survey_feature_1..5` | `Feature one` … `Feature five` |
11 | `exit_notification_title` / `_description` | generic |
12 | `pro_features_subtitle` | `Unlock every premium feature` |
13 | `privacy_policy_url`, `terms_url` | point at terafort.com — **replace, these are legally significant** |
14 | `admob_app_id`, `facebook_app_id`, `facebook_client_token` | belong to the template's accounts |
15 | Launcher icons | `res/mipmap-*` (5 densities) + `mipmap-anydpi-v26/` + `values/ic_launcher_background.xml` |
16 | App art | `drawable/ob_1..3`, `iap_slide_1..3`, `img_uninstall`, `app_icon`, `ic_live_earth*` (names are legacy) |

Locale set lives in **two** places that must agree: `androidResources.localeFilters` in
`app/build.gradle.kts` and `res/xml/locales_config.xml` (referenced from the manifest as
`android:localeConfig`).

### 4. Billing

17. Define your plans in **one** place: [`iab/BillingCatalog.kt`](app/src/main/java/com/professor/baseproject/iab/BillingCatalog.kt).
    Each `BillingPlan` carries `sku`, `basePlanId`, `offerId`, `fallbackPrice`, `periodsPerYear`.
    The underlying ids live in `constants/Constants.kt`.
18. `fallbackPrice` appears in the **free-trial disclaimer** when Play prices haven't loaded.
    Keep it in step with the real price — it is legally significant copy.
19. Add one-time products (e.g. lifetime unlock) to `BillingCatalog.oneTimeSkus` so they get
    queried and acknowledged. Unacknowledged purchases are auto-refunded by Play after 3 days.

`BillingCatalogTest` guards these invariants — run `testDebugUnitTest` after editing.

### 5. Verify

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assembleMinifiedDebug
```

Then check by hand: app name and icon, onboarding copy, paywall prices from Play, dark mode
toggle in Settings, and the language picker.

---

## Architecture

```
app/src/main/java/com/professor/baseproject/
├── app/           MyApp (Application), AppPreferences, AnalyticsManager, NotificationWorker
├── iab/           Play Billing: AppBillingClient (singleton), BillingCatalog, models
├update/           In-app update manager + dialogs
├── remoteconfig/  RemoteConfigManager, keys, config data classes
├── data/          Room (db/), repository/, source/ (local + remote + Retrofit api/)
├── cache/         File cache manager
├── di/            Hilt modules (DiModule, NetworkModule)
├── ui/            base/, screens/, fragments/, viewmodel/, bottomsheets/
└── utils/         StatusBarUtils, StartupNavigationManager, Extensions, UIState
```

**Startup flow.** `StartActivity` → `StartupNavigationManager` decides the next screen from
Remote Config plus three independent preference flags:

| Flag | Written by | Meaning |
|---|---|---|
`IS_LANGUAGE_SELECTED` | `LanguageViewModel` | language chosen |
`IS_ONBOARDING` | `OnboardingActivity` | onboarding finished |
`IS_SURVEY_DONE` | `SurveyActivity` | survey finished |
`IS_FIRST_RUN_COMPLETE` | `MainActivity` | reached main at least once |

Keep these separate. They were once a single flag, and because any step can be disabled from
Remote Config, nothing wrote it and onboarding replayed on **every launch**. Use
`IS_FIRST_RUN_COMPLETE` for "is the user past first-run?" — never a per-step flag.

**Entitlement.** `MyApp.refreshEntitlement()` is the *only* writer of `IS_PREMIUM`, and it
writes only when Play actually answered (`EntitlementStatus.querySucceeded`). Entitlement is
derived from **purchases**, never from the product-details list. Do not add a second writer.

**System bars.** `StatusBarUtils.applyEdgeToEdge()` runs for every Activity from `MyApp` and
owns bar appearance via `enableEdgeToEdge()`. Do **not** set `window.statusBarColor` /
`navigationBarColor` — they are ignored from API 35 onward. Screens wanting immersive bars
implement the `FullscreenScreen` marker interface.

**Dark mode.** Supported. `values-night/` holds colours and themes; all theme parents are
`Material3.DayNight`. Toggle lives in Settings; default is `MODE_NIGHT_FOLLOW_SYSTEM`.
`SettingsViewModel.THEME_MODE`/`DEFAULT_MODE` are the single source — `MyApp` reads the same
constants.

---

## Remote Config

**Three parameters. That is the whole surface.** Reference JSON lives in
[`remote_config_default/`](remote_config_default) — paste each file into the parameter of the
same name:

| File | Parameter key | Holds |
|---|---|---|
`ad_ids.json` | `ad_ids` | one AdMob unit id per placement |
`ad_rules.json` | `ad_rules` | ad frequency, splash variants, startup-flow skips, onboarding slides |
`native_config.json` | `native_config` | the native ad palette (5 colours) |

`RemoteConfigKeys` is the authoritative list. Anything else that used to be remote — the
kill switch, minimum supported version, paywall rules, notification timings, per-format ad
toggles — is now a compile-time constant in `constants/AppConfigDefaults.kt`. The features all
still work; they just need a release to change. A remote key nobody flips is a key that drifts
from the code reading it.

**Defaults.** These three parameters deliberately have **no** entries in
`res/xml/remote_config_defaults.xml`: `RemoteConfigManager` falls back to the Kotlin data-class
defaults in `remoteconfig/data/RemoteConfigData.kt` when a fetched value is blank. Don't
duplicate them, or the two will drift.

**Ad unit ids** are the one exception with a second source, on purpose. `ad_ids` is blank until
the first fetch resolves, so `AdUnits` falls back to `res/values/ad_units.xml` — that is what
serves on a first cold start and offline. Keep it in step with `ad_ids.json`. Debug builds
ignore both and always serve Google's test ids.

**Placements.** `NativePlacement` and `InterstitialPlacement` (in `ads/AdUnits.kt`) map a screen
to its remote unit id. `AdsSlot.show(..., placement = ...)` and
`InterstitialGate.showCapped(..., placement)` are the entry points.

---

## Ads

Every screen talks to the wrappers in `ads/`, never to the ads library directly.

| Class | Owns |
|---|---|
`AdsController` | premium gate, consent, SDK init, `showAds` master switch, resume-ad suppression |
`AdUnits` | unit id per placement (`NativePlacement`, `InterstitialPlacement`) |
`AdsSlot` | inline slots: shimmer → native → banner fallback → collapse |
`InterstitialGate` | every interstitial, and the one time cap they share |
`SplashAdSequencer` | the splash ad's two variants |
`BannerRefresher` | the 30s banner reload timer, per container |
`NativeAdOverlayFragment` | full-screen native shown *between* screens, with a rectangle-banner fallback |

**The plan, and where each row lives.**

| Placement | Behaviour | Wired in |
|---|---|---|
Splash ad | `splashAdFlow` 1 = interstitial→app-open fallback, 2 = the reverse. One ad either way | `SplashAdSequencer` |
Splash navigation | `splashFlow` 1 = ad→paywall, 2 = paywall→ad on close | `StartActivity`, `PremiumActivity.handleClose` |
Language | small native, no media → banner | `LanguageActivity.loadAd` |
Onboarding | small banner-shaped native per slide → banner, one flag per slide | `OnboardingActivity.loadSlideAd` |
Interest CTA | medium native → 300x250 banner, full-screen, then navigate | `SurveyActivity` + `NativeAdOverlayFragment` |
Home, first session | interstitial when a feature is opened; capped | `HomeFragment` → `showFirstSessionFeature` |
Home, feature done | interstitial that **bypasses** and resets the cap | `HomeFragment` → `showOnFeatureComplete` |
Home, Back | interstitial before exit; capped | `MainActivity.exitWithAd` → `showOnExit` |
Home / Settings | small native, no media → banner | `HomeFragment`, `SettingsFragment` |
App resume | app-open ad on return from background | `MainActivity.onCreate` → `allowResumeAds()` |

**Two things are easy to get wrong.**

*Interstitials must never gate the action.* `InterstitialGate`'s `onDone` always runs — after the
ad is dismissed, or immediately when no ad shows. Hang navigation off it and never off the call
returning, or an unfilled request strands the user.

*Resume ads are suppressed until `allowResumeAds()`.* The library shows them from a
process-lifecycle observer with no idea which screen is in front, so app-open ads will look
broken in a fresh fork until main is reached. That is the intended default: one appearing over
onboarding is worse than one not appearing.

*"First session" is `AdsController.isFirstSession`*, snapshotted in `MyApp.onCreate`. Do not read
`IS_FIRST_RUN_COMPLETE` at trigger time — `MainActivity.onCreate` sets it before the user can tap
anything, so the first-session interstitial would never fire.

## ProGuard

`isMinifyEnabled = true` on `release` and `minifiedDebug`. Rules of note:

- Five rules hardcode the package name — see [Identity](#1-identity).
- All 9 ad mediation adapters have broad `-keep class ... { *; }` rules, which limits
  shrinking on the largest dependency group.
- Rules for libraries that are **not** dependencies were removed; don't re-add them
  speculatively.

---

## Testing

```
app/src/test/       BillingCatalogTest, ConvertersTest        (JVM)
app/src/androidTest/ AppDatabaseTest                          (device/emulator)
```

`AppDatabaseTest` is load-bearing: opening the database at all fails if the Room KSP
compiler is not wired up, which is exactly how a previously-missing `room-compiler` went
unnoticed. Room's exported schemas (`app/schemas/`) are on the androidTest asset path so
`MigrationTestHelper` can find them — **commit that directory**.

---

## Known gaps

Accepted trade-offs, not oversights. Listed so nobody rediscovers them:

- **No CI.** Nothing enforces build/lint/test on a fork.
- **Shared signing key and Firebase project** across apps until swapped per fork.
- **`versionCode` is manual.**
- **Single module.** Shared code is copied per fork, so a fix here does not reach apps
  already forked — it must be re-applied by hand.
- **Room has one demo consumer only** (`HomeViewModel`/`HomeFragment`). Keep a consumer so a
  broken persistence setup surfaces during development.
- **`assets/` is empty** although `LocalSource` reads from it, so that offline fallback
  currently always returns an empty list.
- **Settings reuses the `home_native` unit.** `ad_ids` has no separate settings entry; add one
  and a `NativePlacement` if the two need separate reporting.
- **The home interstitial triggers hang off the demo buttons.** `HomeFragment` is a placeholder,
  so "opened a feature" is *add sample row* and "feature finished" is *clear rows*. A fork
  replacing that screen should move both calls rather than delete them.
- **`nav_time_stamp`** in the bottom nav has no destination of its own; it routes to Settings.
- **No server-side purchase verification.** Entitlement is only as trustworthy as the local
  Play response.
- **`enableSplit = false`** for language, so every user downloads all 10 locales. Required by
  the in-app language picker.
