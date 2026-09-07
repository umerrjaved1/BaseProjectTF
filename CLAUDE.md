# CLAUDE.md

Guidance for working in this repository. This is a **single-Activity Compose template** (Message Recovery as the sample product), not the old Vidwave multi-Activity downloader.

## Project

Single-module Android app (`:app`) — Kotlin, minSdk 26 / target+compile SDK 37, JDK 17. UI is Jetpack Compose. Hilt for DI, DataStore prefs, Firebase Remote Config for runtime toggles, AdMob via private `com.umer_tf.ads`.

Package / `applicationId`: `com.example.message.recovery`. Play Console **rejects** `com.example.*` — change `namespace` and `applicationId` in [app/build.gradle.kts](app/build.gradle.kts) and the matching `package_name` in `google-services.json` before a store upload.

## Build & run

```bash
./gradlew assembleDebug
```

```bash
./gradlew assembleRelease
```

There are no unit or instrumentation test source sets (`app/src/test`, `app/src/androidTest` do not exist). Do not tell the user to "run the tests". Verification is a Gradle assemble plus installing the app.

Copy [local.properties.example](local.properties.example) to `local.properties`. **Builds fail** without `gpr.user` / `gpr.key` — GitHub Packages credentials for `com.umer_tf.ads` (`maven.pkg.github.com/umerrjaved1/AdsManager`, see [settings.gradle.kts](settings.gradle.kts)). Never put those credentials in tracked `gradle.properties`.

Optional `API_KEY` / `BASE_URL` in `local.properties` become `BuildConfig` strings (empty if omitted).

Release is minified + resource-shrunk. APK/AAB names use `base.archivesName` (`Message-Recovery_vCode_…`). There is **no** `signingConfigs` block — add a local keystore via `local.properties` when you need a signed release. Do not commit `.jks` / `keystore/`.

Dependency versions live in [gradle/libs.versions.toml](gradle/libs.versions.toml) — add libraries there, not as literal coordinates.

Any new Hilt module, Gson-serialized remote-config class, or reflective model needs a matching keep rule in [app/proguard-rules.pro](app/proguard-rules.pro).

## Clone checklist (new app from this base)

1. Rotate any GitHub PAT that was ever committed; keep the new token only in `local.properties`.
2. Change `applicationId` / `namespace` (not `com.example.*` for Play).
3. Replace `app/google-services.json` with a Firebase project that matches the new package (the checked-in file still points at the Vidwave Firebase project).
4. Replace release AdMob units in [app/src/main/java/com/example/message/recovery/app/AdIds.kt](app/src/main/java/com/example/message/recovery/app/AdIds.kt) and `@string/admob_app_id`. Debug already uses Google test IDs.
5. Replace Play Billing SKUs in [app/src/main/java/com/example/message/recovery/constants/Constants.kt](app/src/main/java/com/example/message/recovery/constants/Constants.kt) (`vidwave_weekly` / `vidwave_yearly` are leftovers).
6. Set Firebase Remote Config `show_ads` (kill switch). In-app default is **on**; XML default is `true`. `AdUtils.areAdsEnabled()` is not-premium AND `RemoteConfigManager.getShowAds()`.
7. Rename `app_name`, icons, `Constants.APP_FOLDER`, and `archivesName`.

## Architecture

Single `MainActivity` is the launcher (`Theme.App.Starting`, `installSplashScreen()`, `launchMode=singleTop`). Typed Navigation Compose routes live in [ui/navigation/ScreenRoutes.kt](app/src/main/java/com/example/message/recovery/ui/navigation/ScreenRoutes.kt): Start → Language / Onboarding / Survey / Premium → Home / Settings.

- [AppNavHost.kt](app/src/main/java/com/example/message/recovery/ui/navigation/AppNavHost.kt) — `NavHost` + route composables.
- [AppNavigator.kt](app/src/main/java/com/example/message/recovery/ui/navigation/AppNavigator.kt) — `replaceAll` (pop to graph start, not `popUpTo(graph.id) { inclusive = true }`), tab switches, locale recreate (`prepareLocaleRecreate` + `navHostGeneration`). `MainActivity` wraps the host in `key(generation)` and must persist language prefs **before** `setApplicationLocales`.
- [CurrentNavDestination.kt](app/src/main/java/com/example/message/recovery/ui/navigation/CurrentNavDestination.kt) — app-open / resume exclusion by destination, not Activity class name.

Startup order is [utils/StartupNavigationManager.kt](app/src/main/java/com/example/message/recovery/utils/StartupNavigationManager.kt) (`getNextRoute` + `navigateNextWithAd`). First-run is `!IS_ONBOARDING`. **Do not** set `IS_ONBOARDING` until the user actually reaches Home (`MainActivity.onEnteredMain`). Setting it at the end of Onboarding skips Survey and post-survey Premium.

Screens are `*Route` composables under `ui/screens/{start,language,onboarding,survey,premium,main}`. Theme is `AppTheme` / `AppTypography` in `ui/theme`.

Activities extend `ui/base/BaseActivity`. ViewModels are `@HiltViewModel`. Dimensions use `sdp`/`ssp`. Localization: `en, ar, es, in, fa, hi, ru, pt, bn, tr` (`localeFilters`, language split disabled). Language switching uses `AppCompatDelegate.setApplicationLocales`, seeded in `MyApp.initializeLanguage()`.

Product feature (notification capture / recovery) lives under `notification/` plus Home/Settings. Feature work should stay in Compose routes, not new Activities.

### Remote config

`RemoteConfigManager` fetches once and parses JSON into `remoteconfig/data/*` (`AdRules`, `AdIdsConfig`, …), keyed by `RemoteConfigKeys`. Defaults: `res/xml/remote_config_defaults`. Sample payloads: [remote_config_default/](remote_config_default). Check the relevant config class before adding a toggle. `show_ads` is parsed in `parseFetchData()` (including when fetch fails, so XML defaults apply).

### Ads and premium

Ads go through `com.umer_tf.ads` plus mediation adapters. Unit IDs: `AdIds.kt` (`BuildConfig.DEBUG` vs release, some RC overrides). Show/hide: `AdUtils.areAdsEnabled()`. Splash uses `AdRules.splashAdLoadOrder` (`app_open_then_inter` or `inter_then_app_open`) with fallback between app-open and interstitial. Language / onboarding / survey / home native slots fall back to a banner via `NativeOrBannerAdSlot`. Tab interstitial and premium-back interstitial share `AdUtils.lastInterAdTime` and `interstitialMinTimer` (default 30s). `MyApp` handles app-open on resume using `CurrentNavDestination`.

Premium has two sources of truth: `AppPreferences.IS_PREMIUM` and `AdMobManager.isPremium`. `MyApp.onCreate` seeds the in-memory flag from prefs **before** billing verifies — do not remove that. Do **not** set `isPremium = false` on billing `onError` (that re-enables ads for payers). Do **not** `disconnect()` the shared `AppBillingClient` when leaving Premium — `MyApp` owns that singleton.

## Release notes

Version bumps are manual in `app/build.gradle.kts` (`versionCode` / `versionName`).
