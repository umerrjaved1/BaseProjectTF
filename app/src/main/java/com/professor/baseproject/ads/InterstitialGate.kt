package com.professor.baseproject.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.professor.baseproject.constants.AppConfigDefaults
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.umer_tf.ads.domain.analytics.AdType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every interstitial in the app goes through here, so the capping rules live in one place rather
 * than being re-derived at each trigger.
 *
 * ### The two kinds of interstitial
 * - [showCapped] — an *interruption*: the user tapped something and an ad appears first. Subject to
 *   the [com.professor.baseproject.remoteconfig.data.AdRulesConfig.interstitialTimer] cap.
 * - [showUncapped] — a *break*: a feature finished, so the user is between tasks. Ignores the cap
 *   and resets it. Use it only for genuine completion points; using it for interruptions removes
 *   the cap's entire purpose.
 *
 * ### Placements
 * [InterstitialPlacement] picks which remote ad unit is requested. The cap is shared across
 * placements on purpose — it exists to limit what the *user* sees, and they do not care which
 * unit an ad came from.
 *
 * The timer is tracked here rather than relying on the library's `showAdWithTimeAndCounter`. That
 * helper folds a counter and two thresholds into one expression, and its "max time" threshold
 * *forces* a show once passed — with a single remote cap and an explicit uncapped path, a plain
 * elapsed-time check is easier to reason about and cannot accidentally show on every call.
 */
@Singleton
class InterstitialGate @Inject constructor(
    private val adsController: AdsController
) {

    /** Uptime-based so a clock change cannot unlock the cap. Zero means "never shown". */
    private var lastShownUptimeMs = 0L

    private val capMs: Long
        get() = RemoteConfigManager.getAdRules().interstitialTimer.coerceAtLeast(0) * 1000L

    /** Seconds until the cap expires; 0 when an interstitial may show now. */
    val secondsUntilAllowed: Long
        get() {
            if (lastShownUptimeMs == 0L) return 0
            val remaining = capMs - (SystemClock.elapsedRealtime() - lastShownUptimeMs)
            return if (remaining <= 0) 0 else remaining / 1000
        }

    /** Whether a capped interstitial would be allowed right now. */
    fun isCapExpired(): Boolean = secondsUntilAllowed == 0L

    /**
     * Shows an interstitial if the time cap has expired.
     *
     * @param onDone always invoked exactly once - after the ad is dismissed, or immediately when no
     *   ad is shown. Navigation must hang off this, never off the call returning.
     */
    @JvmOverloads
    fun showCapped(
        activity: Activity,
        placement: InterstitialPlacement = InterstitialPlacement.HOME,
        onDone: () -> Unit
    ) {
        if (!isCapExpired()) {
            Log.d(TAG, "showCapped: suppressed, ${secondsUntilAllowed}s left on the cap")
            onDone()
            return
        }
        show(activity, placement, onDone)
    }

    /**
     * Shows an interstitial regardless of the cap, and resets the cap afterwards.
     *
     * For feature completion and background-processing completion only.
     */
    @JvmOverloads
    fun showUncapped(
        activity: Activity,
        placement: InterstitialPlacement = InterstitialPlacement.HOME,
        onDone: () -> Unit
    ) {
        Log.d(TAG, "showUncapped: bypassing the cap and resetting the timer")
        show(activity, placement, onDone)
    }

    /**
     * Interstitial on the way out of the app, before the exit dialog.
     *
     * Uses the `exit_inter` unit and is still capped, so backing out of a screen the user just
     * arrived at cannot chain an ad onto one they only just dismissed.
     */
    fun showOnExit(activity: Activity, onDone: () -> Unit) {
        if (!AppConfigDefaults.HOME_BACK_INTERSTITIAL) {
            onDone()
            return
        }
        showCapped(activity, InterstitialPlacement.EXIT, onDone)
    }

    /**
     * Interstitial when a feature finishes. Bypasses the cap, because it marks a natural break in
     * the user's task rather than an interruption of it.
     */
    fun showOnFeatureComplete(activity: Activity, onDone: () -> Unit) {
        if (!AppConfigDefaults.FEATURE_COMPLETE_INTERSTITIAL) {
            onDone()
            return
        }
        showUncapped(activity, InterstitialPlacement.HOME, onDone)
    }

    /**
     * First-session-only interstitial, for a feature opened from home.
     *
     * "First session" is [AdsController.isFirstSession] — the process in which the user first
     * reached main — not a live read of `IS_FIRST_RUN_COMPLETE`, which `MainActivity.onCreate`
     * has already set to true by the time any feature can be tapped. Still capped, so several
     * quick feature taps cannot chain ads.
     */
    fun showFirstSessionFeature(activity: Activity, onDone: () -> Unit) {
        if (!AppConfigDefaults.HOME_FIRST_SESSION_FEATURE_INTERSTITIAL ||
            !adsController.isFirstSession
        ) {
            onDone()
            return
        }
        showCapped(activity, InterstitialPlacement.HOME, onDone)
    }

    private fun show(
        activity: Activity,
        placement: InterstitialPlacement,
        onDone: () -> Unit
    ) {
        if (!adsController.isEnabled(AdType.INTERSTITIAL)) {
            onDone()
            return
        }

        var finished = false
        val finishOnce = {
            if (!finished) {
                finished = true
                // Stamped on dismissal, not on request: the cap should measure the gap between ads
                // the user actually saw, and a request that never filled is not one of them.
                lastShownUptimeMs = SystemClock.elapsedRealtime()
                onDone()
            }
        }

        adsController.manager.interstitialAdLoader.loadAndShowAd(
            activity = activity,
            adUnitId = adsController.units.interstitial(placement),
            showDialog = true,
            onAdLoaded = null,
            onAdDismissed = { finishOnce() }
        )
    }

    /** Preloads an interstitial so the next trigger does not wait on the network. */
    @JvmOverloads
    fun preload(placement: InterstitialPlacement = InterstitialPlacement.HOME) {
        if (!adsController.isEnabled(AdType.INTERSTITIAL)) return
        adsController.manager.interstitialAdLoader.loadAd(adsController.units.interstitial(placement))
    }

    /** True when an interstitial is cached and fresh. */
    fun isReady(): Boolean = adsController.manager.interstitialAdLoader.isAdLoaded()

    private companion object {
        const val TAG = "InterstitialGate"
    }
}
