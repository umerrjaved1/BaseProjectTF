package com.professor.baseproject.ads

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.CrashReporter
import com.umer_tf.ads.domain.analytics.AdEventListener
import com.umer_tf.ads.domain.analytics.AdLoadFailure
import com.umer_tf.ads.domain.analytics.AdRevenueInfo
import com.umer_tf.ads.domain.analytics.AdType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes every ad event from the ads library into this app's own reporting.
 *
 * The library logs its own Firebase events, so this deliberately does **not** re-log the same
 * thing under a second name. It adds what the library cannot know about:
 *
 * - **Meta / Facebook** ad-revenue events, via [AnalyticsManager.logFacebookEvent]. AdMob's
 *   paid-event callback is the only source of per-impression revenue, so without this hook
 *   there is no way to report it to any attribution SDK.
 *   Swap in AppsFlyer / Adjust here if the fork uses one - this is the single place to do it.
 * - **Crash breadcrumbs**, so a crash report shows which ad was on screen. Full-screen ads are
 *   a common crash context and previously left no trace at all.
 *
 * Failures are logged as non-fatals only when they come from the SDK. A
 * [AdLoadFailure.CODE_LIBRARY] failure means the library refused the request itself - premium
 * user, offline, consent not granted - which is normal operation, not something to report.
 */
@Singleton
class AdsAnalyticsBridge @Inject constructor(
    private val analyticsManager: AnalyticsManager,
    private val crashReporter: CrashReporter
) : AdEventListener {

    override fun onAdImpression(adUnitId: String, adType: AdType) {
        crashReporter.breadcrumb(TAG, "impression ${adType.revenueName}")
    }

    override fun onAdShowed(adUnitId: String, adType: AdType) {
        // The impression signal for full-screen formats; they have no onAdImpression here.
        crashReporter.breadcrumb(TAG, "showed ${adType.revenueName}")
        crashReporter.setKey(KEY_LAST_AD, adType.revenueName)
    }

    override fun onAdDismissed(adUnitId: String, adType: AdType) {
        crashReporter.breadcrumb(TAG, "dismissed ${adType.revenueName}")
    }

    override fun onAdClicked(adUnitId: String, adType: AdType) {
        crashReporter.breadcrumb(TAG, "clicked ${adType.revenueName}")
    }

    override fun onAdFailedToLoad(adUnitId: String, adType: AdType, failure: AdLoadFailure) {
        if (failure.code == AdLoadFailure.CODE_LIBRARY) {
            // Suppressed by the library (premium / offline / no consent) - expected.
            crashReporter.breadcrumb(TAG, "suppressed ${adType.revenueName}: ${failure.message}")
            return
        }
        crashReporter.breadcrumb(
            TAG,
            "failed ${adType.revenueName}: ${failure.code} ${failure.message}"
        )
    }

    /**
     * Called on a background thread, possibly often. Keep it cheap.
     */
    override fun onAdRevenuePaid(info: AdRevenueInfo) {
        val bundle = Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, info.value)
            putString(FirebaseAnalytics.Param.CURRENCY, info.currencyCode)
            putString("ad_format", info.adType.revenueName)
            putString("ad_unit_id", info.adUnitId)
            putString("value_precision", info.precision.name)
        }
        // Meta attributes ad revenue from this event; AdMob's paid callback is the only place
        // the value is available.
        analyticsManager.logFacebookEvent(EVENT_AD_REVENUE, bundle)
    }

    private companion object {
        const val TAG = "Ads"
        const val KEY_LAST_AD = "last_fullscreen_ad"
        const val EVENT_AD_REVENUE = "ad_revenue"
    }
}
