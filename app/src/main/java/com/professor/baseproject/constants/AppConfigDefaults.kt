package com.professor.baseproject.constants

/**
 * Behaviour that used to be remotely configurable and no longer is.
 *
 * Remote Config was cut back to three parameters - `ad_ids`, `ad_rules` and `native_config`.
 * The features below kept working; they just read a constant now instead of a key nobody was
 * flipping. Changing any of them needs a release.
 *
 * If one of these turns out to need tuning in production, promote it back into
 * [com.professor.baseproject.remoteconfig.data.AdRulesConfig] rather than adding a fourth
 * parameter - the point of the three-key layout is that there is one place to look.
 */
object AppConfigDefaults {

    // --- Splash ------------------------------------------------------------------------------
    /**
     * Require an explicit "Get started" tap before leaving the splash.
     *
     * False means the splash advances on its own as soon as startup work finishes.
     */
    const val SHOW_GET_STARTED_BUTTON = false

    // --- Onboarding --------------------------------------------------------------------------
    const val ONBOARDING_CROSS_BUTTON_VISIBLE = true

    // --- Paywall -----------------------------------------------------------------------------
    /** Drop PremiumActivity out of the startup sequence entirely. */
    const val SKIP_PREMIUM_SCREEN = false

    /** How long the paywall's close button (and Back) stay disabled. */
    const val PREMIUM_CLOSE_BTN_DELAY_MS = 3000

    /** Interactions before the paywall is offered, when [SHOW_PREMIUM_AFTER_CLICKS] is on. */
    const val PREMIUM_CLICK_COUNT = 3

    const val SHOW_PREMIUM_ON_RESUME = false
    const val SHOW_PREMIUM_AFTER_CLICKS = false

    // --- Remote block / minimum version ------------------------------------------------------
    /**
     * Hard-stop the app on launch.
     *
     * Was the emergency brake for a broken release, and is now inert by definition: flipping it
     * requires the very release it was meant to avoid. Kept wired so re-promoting it to Remote
     * Config is a one-line change rather than restoring a deleted code path.
     */
    const val KILL_SWITCH_ENABLED = false

    /** Force an update for builds below this versionCode. 0 disables the check. */
    const val MIN_SUPPORTED_VERSION_CODE = 0

    // --- Interstitial capping, inside the ads library -----------------------------------------
    /**
     * These feed `AdMobManager`'s own counter/threshold capping, which is separate from
     * [com.professor.baseproject.ads.InterstitialGate]'s `interstitialTimer` cap. The gate is
     * what the app's own triggers go through; these are the library's backstop.
     */
    const val INTERSTITIAL_COUNTER = 3
    const val INTERSTITIAL_MIN_TIME_SECS = 30L
    const val INTERSTITIAL_MAX_TIME_SECS = 120L

    /** Seconds backgrounded before an app-open resume ad may show. */
    const val OPEN_AD_RESUME_TIME_SECS = 5L

    /**
     * How long the loading dialog stays up before a full-screen ad appears. Stops the ad from
     * landing under a finger that is still mid-tap.
     */
    const val INTERSTITIAL_DIALOG_DELAY_MS = 1500L

    // --- Interstitial triggers ----------------------------------------------------------------
    /** First session only: interstitial when the user opens a feature from home. */
    const val HOME_FIRST_SESSION_FEATURE_INTERSTITIAL = true

    /** Interstitial on home back press, before the exit dialog. */
    const val HOME_BACK_INTERSTITIAL = true

    /** Interstitial when a feature finishes or background processing completes. */
    const val FEATURE_COMPLETE_INTERSTITIAL = true

    // --- Notifications -----------------------------------------------------------------------
    const val NOTIFICATION_INITIAL_DELAY_HOURS = 24L
    const val NOTIFICATION_REPEAT_INTERVAL_HOURS = 24L
    const val ENABLE_REPEATING_NOTIFICATIONS = false
}
