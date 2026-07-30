package com.professor.baseproject.remoteconfig.data

import com.google.gson.annotations.SerializedName

/**
 * Per-placement AdMob unit ids, from the `ad_ids` parameter.
 *
 * Defaults are blank on purpose. A blank value means "use the id baked into
 * `res/values/ad_units.xml`", which is what serves ads on a first cold start before Remote
 * Config has ever resolved - see [com.professor.baseproject.ads.AdUnits]. Shipping the real
 * ids as Kotlin defaults here would make the resource file dead weight and give two places to
 * update when a unit is replaced.
 *
 * The JSON keys are snake_case to match the Firebase template, so every field is mapped
 * explicitly rather than relying on Gson's field-name matching.
 */
data class AdIdsConfig(
    @SerializedName("app_open") val appOpen: String = "",
    @SerializedName("banner") val banner: String = "",
    @SerializedName("exit_inter") val exitInterstitial: String = "",
    @SerializedName("home_inter") val homeInterstitial: String = "",
    @SerializedName("home_native") val homeNative: String = "",
    @SerializedName("interest_native") val interestNative: String = "",
    @SerializedName("language_native") val languageNative: String = "",
    @SerializedName("ob_native") val onboardingNative: String = ""
)

/**
 * Ad frequency, splash variants and startup-flow skips, from the `ad_rules` parameter.
 *
 * Every default is the intended shipping behaviour, so the app is correct with no Firebase
 * template at all. Turning something off is an explicit remote decision.
 */
data class AdRulesConfig(

    /**
     * Master ad kill switch. False stops every request across every format, skips consent
     * gathering, and skips SDK initialisation entirely.
     */
    val showAds: Boolean = true,

    /**
     * Minimum seconds between *capped* interstitials, applied by
     * [com.professor.baseproject.ads.InterstitialGate].
     *
     * Uncapped interstitials (a feature completing) ignore this and reset it, because they mark
     * a break in the user's task rather than an interruption of it.
     */
    val interstitialTimer: Long = 25,

    /** App-open ad when the app returns from background. */
    val showAppOpenAdOnResume: Boolean = true,

    // --- Exit notification ---------------------------------------------------------------------
    val enableExitNotification: Boolean = false,
    val exitNotificationTitle: String = "We miss you!",
    val exitNotificationDescription: String = "Come back and explore the live earth map.",

    /**
     * Banner reload interval, in seconds.
     *
     * AdMob requires at least 30s between manual refreshes; lower values are clamped by
     * [com.professor.baseproject.ads.BannerRefresher] rather than risking a policy breach.
     */
    val bannerRefreshSeconds: Long = 30,

    /**
     * Splash ad order.
     *
     * `1` = interstitial first, app-open as the fallback when no interstitial filled.
     * `2` = app-open first, interstitial as the fallback.
     *
     * Only one ad is ever shown; the second is a fallback for an unfilled request, not a second
     * impression.
     */
    val splashAdFlow: Int = 1,

    /**
     * Where the paywall sits relative to the splash ad.
     *
     * `1` = splash -> ad -> paywall.
     * `2` = splash -> paywall -> ad once the user closes or skips it.
     *
     * Flow 2 shows the paywall to a warmer user but delays the impression, and a user who buys
     * never sees the ad at all - which is correct, and why the two are worth measuring.
     */
    val splashFlow: Int = 1,

    /**
     * Upper bound on the splash, in seconds - not a fixed dwell. Clamped to 1..15 by
     * StartActivity so a bad remote value cannot strand users on the splash.
     */
    val startupTime: Int = 8,

    // --- Startup flow skips --------------------------------------------------------------------
    val skipLanguageScreen: Boolean = false,
    val skipOnboardingScreen: Boolean = false,
    val skipSurveyScreen: Boolean = false,

    // --- Onboarding slides ---------------------------------------------------------------------
    val showObSlide1: Boolean = true,
    val showObSlide2: Boolean = true,
    val showObSlide3: Boolean = true,

    /** Native ad per onboarding slide, so slides can be measured independently. */
    val showOb1Native: Boolean = true,
    val showOb2Native: Boolean = true,
    val showOb3Native: Boolean = true,

    /** Free-text note for whoever is editing the template in the console. Never read by code. */
    val comments: String = ""
) {

    /** Whether slide [index] (1-based) is present at all. */
    fun isObSlideEnabled(index: Int): Boolean = when (index) {
        1 -> showObSlide1
        2 -> showObSlide2
        3 -> showObSlide3
        else -> false
    }

    /** Whether slide [index] (1-based) carries a native ad. */
    fun isObNativeEnabled(index: Int): Boolean = when (index) {
        1 -> showOb1Native
        2 -> showOb2Native
        3 -> showOb3Native
        else -> false
    }
}

/**
 * Native ad palette, from the `native_config` parameter.
 *
 * One palette, not a light/dark pair - which is deliberate. Native ads are rendered with these
 * exact colours in both system themes, so what the console previews is what ships. A fork that
 * wants the card to follow the system theme should stop reading this and go back to
 * `NativeAdTheme.auto()`.
 */
data class NativeAdColors(
    val callActionButtonColor: String = "#254AA6",
    val backgroundColor: String = "#E0EAF6",
    val ctaText: String = "#FFFFFF",
    val heading: String = "#04061A",
    val description: String = "#6F6F6F"
)
