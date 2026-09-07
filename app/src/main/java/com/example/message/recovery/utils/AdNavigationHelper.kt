package com.example.message.recovery.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.message.recovery.app.AnalyticsManager
import com.umer_tf.ads.domain.core.AdMobManager

object AdNavigationHelper {

    /**
     * Show a timer-gated interstitial ad on back press and then run [onAfterAd] (e.g. finish()).
     * Gated by interstitialMinTimer. Safe for premium users.
     */
    fun showBackPressInterstitial(
        activity: AppCompatActivity,
        adMobManager: AdMobManager,
        lifecycle : Lifecycle,
        lifecycleScope : LifecycleCoroutineScope,
        shouldShow: Boolean,
        adId: String,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String = "back_inter",
        ignoreFrequency: Boolean = false,
        onAfterAd: () -> Unit
    ) {
        if (!shouldShow) {
            onAfterAd()
            return
        }

        AdUtils.loadAndShowAdWithTimer(
            activity = activity,
            adMobManager = adMobManager,
            adUnit = adId,lifecycle = lifecycle,
            lifecycleScope = lifecycleScope,

            analyticsManager = analyticsManager,
            eventNamePrefix = eventNamePrefix,
            ignoreFrequency = ignoreFrequency,
            onNavigate = onAfterAd
        )
    }


}
