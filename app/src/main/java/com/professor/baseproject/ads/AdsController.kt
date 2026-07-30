package com.professor.baseproject.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.professor.baseproject.BuildConfig
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.constants.AppConfigDefaults
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.umer_tf.ads.domain.analytics.AdType
import com.umer_tf.ads.domain.annotations.AdUnitIdValidator
import com.umer_tf.ads.domain.consent.AdsConsentGate
import com.umer_tf.ads.domain.core.AdMobManager
import com.umer_tf.ads.domain.core.AdsRequestConfig
import com.umer_tf.ads.domain.utils.AdsLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's single entry point to the ads library.
 *
 * Screens talk to this, not to [AdMobManager] directly, so the wiring below exists in exactly
 * one place:
 *
 * - **Premium** is read live from [AppPreferences.IS_PREMIUM] on every ad request, rather than
 *   snapshotted at startup. Billing resolves asynchronously, so a snapshot would serve ads to a
 *   subscriber for the whole session on any launch where Play answered late.
 * - **Ad events** go to [AdsAnalyticsBridge].
 * - **Remote ad rules** from `ad_rules` gate every format via [isEnabled].
 * - **Consent** is gathered before the SDK initializes.
 *
 * ### Call order
 * 1. [configure] from `MyApp.onCreate` - cheap, no network, no Activity needed.
 * 2. [start] from the splash Activity once Remote Config has resolved.
 *
 * Splitting them matters: [configure] installs the premium gate and the event listener, so it
 * must happen before anything can request an ad, while [start] needs an Activity for the consent
 * form and needs the remote rules to already be parsed.
 */
@Singleton
class AdsController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val adUnits: AdUnits,
    private val analyticsBridge: AdsAnalyticsBridge
) {

    // The Hilt @ApplicationContext *is* the Application instance; AdMobManager keys its
    // singleton on it.
    private val application: Application get() = context as Application

    val manager: AdMobManager get() = AdMobManager.getInstance(application)

    val units: AdUnits get() = adUnits

    private var isStarted = false

    /**
     * Whether this *process* started before the user had ever reached the main screen.
     *
     * Snapshotted in [configure] (from `MyApp.onCreate`) rather than read live, because
     * `MainActivity.onCreate` writes [AppPreferences.IS_FIRST_RUN_COMPLETE] the moment main is
     * reached. Reading the preference at trigger time therefore always reports "not the first
     * session" — the flag is already true before the user can tap anything — and the first-session
     * interstitial would never fire once in the app's life.
     */
    var isFirstSession: Boolean = false
        private set

    /**
     * Installs the gates and listeners. Safe and cheap; performs no network work and shows no UI.
     */
    fun configure() {
        isFirstSession = !appPreferences.getBoolean(AppPreferences.IS_FIRST_RUN_COMPLETE)
        AdsLog.isEnabled = BuildConfig.DEBUG
        // Malformed unit ids should fail loudly in debug and be skipped in release rather than
        // crashing a user's session.
        AdUnitIdValidator.strictMode = BuildConfig.DEBUG

        manager
            .setPremiumProvider { appPreferences.getBoolean(AppPreferences.IS_PREMIUM) }
            .setAdEventListener(analyticsBridge)
            .setAppOpenAdStartId(adUnits.appOpen)
            .setAppOpenAdResumeId(adUnits.appOpen)
            // App-open resume ads are shown automatically by the library, off
            // ProcessLifecycleOwner - it does not know which screen the user is on. Left
            // suppressed so one cannot appear over the splash, language picker, onboarding or
            // survey. Call [allowResumeAds] once the user reaches the main screen.
            .setSplash(true)
            .setRequestConfig(
                AdsRequestConfig(
                    // This base project is not child-directed. A fork targeting Play Families
                    // MUST set these - see AdsRequestConfig.forChildDirectedApp().
                    tagForChildDirectedTreatment = false,
                    tagForUnderAgeOfConsent = false
                )
            )
        Log.d(TAG, "configure: premium gate, event listener and request config installed")
    }

    /**
     * Gathers consent, applies the remote ad rules, then initializes the AdMob SDK.
     *
     * Call once from the splash Activity, after `RemoteConfigManager.fetchRemoteConfig` has
     * completed, so the caps below reflect the fetched values rather than the defaults.
     *
     * @param onReady invoked with whether ads may be requested. False means the user declined
     *   consent, or the remote kill switch is off - in both cases carry on without ads.
     */
    fun start(activity: Activity, onReady: ((Boolean) -> Unit)? = null) {
        applyRemoteRules()

        if (isStarted) {
            onReady?.invoke(areAdsEnabled())
            return
        }
        isStarted = true

        if (!areAdsEnabled()) {
            // Kill switch is off. Skip consent and initialization entirely - there is nothing to
            // ask permission for if no request will ever be made.
            Log.d(TAG, "start: ads disabled remotely, skipping consent and SDK init")
            onReady?.invoke(false)
            return
        }

        manager.gatherConsent(activity, isTest = FORCE_CONSENT_FORM_FOR_TESTING) { canRequestAds ->
            Log.d(TAG, "start: consent settled, canRequestAds=$canRequestAds")
            if (!canRequestAds) {
                Log.w(
                    TAG,
                    "start: consent NOT granted - every ad request will be blocked. " +
                        "If this is unexpected, check that a UMP message is published in " +
                        "AdMob > Privacy & messaging for this app."
                )
            }
            onReady?.invoke(canRequestAds)
        }
    }

    /**
     * Initializes the AdMob SDK without going through consent.
     *
     * Needed because [start] is only reached on the online startup path. If the app starts offline
     * and regains network later, nothing would otherwise have initialized the SDK and every request
     * in that session would fail against an uninitialized instance.
     *
     * Requests are still gated by consent, so this cannot serve an ad on its own.
     */
    fun ensureInitialized() {
        manager.initialize()
    }

    /**
     * Logs why ads are or are not being served right now.
     *
     * "No ads are showing" has five separate causes that all look identical from the outside, so
     * without this the only way to tell them apart is to read the library's source. Call it from a
     * debug menu or after [start].
     */
    fun diagnose() {
        val rules = RemoteConfigManager.getAdRules()
        Log.i(
            TAG,
            """
            |--- ads diagnostics ---
            |premium (blocks all)   : $isPremium
            |remote showAds         : ${rules.showAds}
            |consent enforced       : ${AdsConsentGate.isEnforced}
            |consent canRequestAds  : ${AdsConsentGate.canRequestAds}
            |gate allows requests   : ${AdsConsentGate.allowsAdRequests()}
            |splash ad flow / flow  : ${rules.splashAdFlow} / ${rules.splashFlow}
            |interstitial cap       : ${rules.interstitialTimer}s
            |banner refresh         : ${rules.bannerRefreshSeconds}s
            |resume app-open ads    : ${rules.showAppOpenAdOnResume}
            |resume ads allowed     : ${!isResumeSuppressed}
            |unit (home inter)      : ${adUnits.homeInterstitial}
            |debug build            : ${BuildConfig.DEBUG} (test ad units in use)
            |-----------------------
            """.trimMargin()
        )
    }

    private var isResumeSuppressed = true

    /**
     * Set by the splash when `ad_rules.splashFlow == 2`, meaning the paywall goes first and
     * the interstitial is owed once the user closes it.
     *
     * A flag rather than `startActivityForResult`: the paywall is reached through
     * StartupNavigationManager, which the splash does not launch directly, so there is no result to
     * wait on. Whoever consumes it must clear it - see `PremiumActivity`.
     */
    @JvmField
    var pendingSplashInterstitial: Boolean = false

    /**
     * Pushes the current `ad_rules` values into the library. Called by [start], and safe to call
     * again after a later Remote Config refresh.
     *
     * The library's own counter/threshold capping comes from [AppConfigDefaults] rather than
     * Remote Config - the app's triggers go through [InterstitialGate], and that gate's
     * `interstitialTimer` is the knob worth tuning remotely.
     */
    fun applyRemoteRules() {
        val rules = RemoteConfigManager.getAdRules()
        manager
            .setInterstitialCounter(AppConfigDefaults.INTERSTITIAL_COUNTER)
            .setInterstitialAdMinTime(AppConfigDefaults.INTERSTITIAL_MIN_TIME_SECS)
            .setInterstitialAdMaxTime(AppConfigDefaults.INTERSTITIAL_MAX_TIME_SECS)
            .setOpenAdResumeTime(AppConfigDefaults.OPEN_AD_RESUME_TIME_SECS)
            .setInterstitialDialogDelay(AppConfigDefaults.INTERSTITIAL_DIALOG_DELAY_MS)
            .setShouldShowResumeAd(rules.showAds && rules.showAppOpenAdOnResume)
        Log.d(
            TAG,
            "applyRemoteRules: showAds=${rules.showAds}, " +
                "interstitialTimer=${rules.interstitialTimer}s, " +
                "resumeAppOpen=${rules.showAppOpenAdOnResume}"
        )
    }

    /**
     * Lets app-open resume ads start showing. Call from the main screen's `onCreate`, once the
     * user is past the startup flow.
     *
     * Until this is called no resume ad can appear, which is why app-open ads will look like they
     * "do not work" in a fresh fork. That is the intended default: the library shows resume ads
     * from a process-lifecycle observer with no knowledge of the current screen, and one appearing
     * over onboarding is worse than one not appearing at all.
     */
    fun allowResumeAds() {
        if (!RemoteConfigManager.getAdRules().showAppOpenAdOnResume) return
        manager.setSplash(false)
        isResumeSuppressed = false
        Log.d(TAG, "allowResumeAds: app-open resume ads enabled")
    }

    /** Suppresses app-open resume ads again, e.g. while a purchase flow is in front. */
    fun suppressResumeAds() {
        manager.setSplash(true)
        isResumeSuppressed = true
    }

    /** Master remote kill switch: `ad_rules.showAds`. */
    fun areAdsEnabled(): Boolean = RemoteConfigManager.getAdRules().showAds

    /**
     * Whether [adType] may be requested right now.
     *
     * Check this before building a container or starting a shimmer. The library's own gate
     * (premium / offline / consent) still runs on every request, so skipping this call cannot
     * leak an ad - it just avoids laying out a slot that will never be filled.
     *
     * `ad_rules` has one master switch rather than a flag per format. The old per-format flags
     * were never used to turn a single format off, and a resume app-open ad is the one case that
     * genuinely needed its own control - which it has, in `showAppOpenAdOnResume`.
     */
    fun isEnabled(adType: AdType): Boolean {
        val rules = RemoteConfigManager.getAdRules()
        if (!rules.showAds) return false
        return when (adType) {
            AdType.APP_OPEN_RESUME -> rules.showAppOpenAdOnResume
            else -> true
        }
    }

    /** True when this user should never see an ad. */
    val isPremium: Boolean get() = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)

    private companion object {
        const val TAG = "AdsController"

        /**
         * Forces the UMP consent form to appear, by pretending the device is in the EEA.
         *
         * **Off deliberately, including in debug.** Turning it on resets stored consent and forces a
         * form on every launch - and if no UMP message is published under
         * AdMob > Privacy & messaging, the form fails to load, consent is never granted, and every
         * ad request is silently blocked. Which looks exactly like "ads are broken".
         *
         * Flip to true only to test the consent flow itself, once a message is published.
         */
        const val FORCE_CONSENT_FORM_FOR_TESTING = false
    }
}
