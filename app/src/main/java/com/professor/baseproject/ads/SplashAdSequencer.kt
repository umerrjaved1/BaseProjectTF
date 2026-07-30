package com.professor.baseproject.ads

import android.app.Activity
import android.util.Log
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.umer_tf.ads.domain.analytics.AdType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the splash ad, per the ad plan's two variants.
 *
 * - **Variant 1** — interstitial first; if it does not fill, fall back to app-open.
 * - **Variant 2** — app-open first; if it does not fill, fall back to interstitial.
 *
 * Exactly **one** ad is shown either way. The second format is a fallback for an unfilled request,
 * not a second impression: showing both would be two full-screen ads before the user has seen the
 * app once.
 *
 * Selected by `ad_rules.splashAdFlow` so the two can be measured against each other without a
 * release.
 *
 * ### Timeouts
 * The splash cannot wait indefinitely on a fill. Each attempt is bounded by [LOAD_TIMEOUT_MS], and
 * [run] always calls back - on success, on no-fill, on timeout, or when ads are off. The splash's
 * own `splashMaxMs` ceiling still applies on top.
 */
@Singleton
class SplashAdSequencer @Inject constructor(
    private val adsController: AdsController
) {

    private var hasRun = false

    /**
     * Shows the splash ad, then calls [onComplete] exactly once.
     *
     * Only ever runs once per process; a second call completes immediately. The splash can be
     * recreated (rotation, update dialog) and the user should not get a second ad for it.
     *
     * @param onComplete invoked with true when an ad was shown. Navigation belongs here.
     */
    fun run(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (hasRun) {
            onComplete(false)
            return
        }
        hasRun = true

        val variant = RemoteConfigManager.getAdRules().splashAdFlow
        Log.d(TAG, "run: splash ad flow $variant")

        val done = SingleShot(onComplete)
        when (variant) {
            2 -> appOpenThenInterstitial(activity, done)
            else -> interstitialThenAppOpen(activity, done)
        }
    }

    // Variant 1
    private fun interstitialThenAppOpen(activity: Activity, done: SingleShot) {
        if (!adsController.isEnabled(AdType.INTERSTITIAL)) {
            showAppOpen(activity, done)
            return
        }
        val interstitial = adsController.manager.interstitialAdLoader
        interstitial.loadAdWithTimeOut(adsController.units.homeInterstitial, LOAD_TIMEOUT_MS) { loaded ->
            if (loaded && interstitial.isAdLoaded()) {
                interstitial.showAd(
                    activity = activity,
                    adUnitId = adsController.units.homeInterstitial,
                    onAdDismissed = { done.fire(true) },
                    onAdFailedToShow = { showAppOpen(activity, done) }
                )
            } else {
                Log.d(TAG, "variant 1: no interstitial, falling back to app open")
                showAppOpen(activity, done)
            }
        }
    }

    // Variant 2
    private fun appOpenThenInterstitial(activity: Activity, done: SingleShot) {
        if (!adsController.isEnabled(AdType.APP_OPEN_START)) {
            showInterstitial(activity, done)
            return
        }
        val appOpen = adsController.manager.appOpenAdLoader
        appOpen.loadAppOpenAd(activity.applicationContext) { loaded ->
            if (loaded && appOpen.isStartAdAvailable()) {
                appOpen.showAppOpenAdIfAvailable { shown ->
                    if (shown) done.fire(true) else showInterstitial(activity, done)
                }
            } else {
                Log.d(TAG, "variant 2: no app open ad, falling back to interstitial")
                showInterstitial(activity, done)
            }
        }
    }

    private fun showAppOpen(activity: Activity, done: SingleShot) {
        if (!adsController.isEnabled(AdType.APP_OPEN_START)) {
            done.fire(false)
            return
        }
        val appOpen = adsController.manager.appOpenAdLoader
        if (appOpen.isStartAdAvailable()) {
            appOpen.showAppOpenAdIfAvailable { shown -> done.fire(shown) }
            return
        }
        appOpen.loadAppOpenAd(activity.applicationContext) { loaded ->
            if (loaded && appOpen.isStartAdAvailable() && !activity.isFinishing) {
                appOpen.showAppOpenAdIfAvailable { shown -> done.fire(shown) }
            } else {
                done.fire(false)
            }
        }
    }

    private fun showInterstitial(activity: Activity, done: SingleShot) {
        if (!adsController.isEnabled(AdType.INTERSTITIAL)) {
            done.fire(false)
            return
        }
        adsController.manager.interstitialAdLoader.loadAndShowAd(
            activity = activity,
            adUnitId = adsController.units.homeInterstitial,
            showDialog = true,
            onAdLoaded = null,
            onAdDismissed = { done.fire(true) }
        )
    }

    /**
     * Guarantees the completion callback runs once.
     *
     * The fallback chains have several terminal paths and some of the SDK callbacks can fire more
     * than once; navigating twice would put two copies of the next screen on the stack.
     */
    private class SingleShot(private val onComplete: (Boolean) -> Unit) {
        private var fired = false
        fun fire(shown: Boolean) {
            if (fired) return
            fired = true
            onComplete(shown)
        }
    }

    private companion object {
        const val TAG = "SplashAdSequencer"

        /** Per-attempt fill deadline. The splash already has its own overall ceiling. */
        const val LOAD_TIMEOUT_MS = 8_000L
    }
}
