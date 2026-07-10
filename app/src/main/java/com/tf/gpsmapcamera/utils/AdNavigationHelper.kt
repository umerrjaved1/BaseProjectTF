package com.tf.gpsmapcamera.utils

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.umer_tf.ads.domain.core.AdMobManager

object AdNavigationHelper {

    /**
     * Show a timer-gated interstitial ad on back press and then run [onAfterAd] (e.g. finish()).
     * Controlled by Global Ad Rules (AdFrequencyControl). Safe for premium users.
     */
    fun showBackPressInterstitial(
        activity: AppCompatActivity,
        adMobManager: AdMobManager,
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
            adUnit = adId,
            analyticsManager = analyticsManager,
            eventNamePrefix = eventNamePrefix,
            ignoreFrequency = ignoreFrequency,
            onNavigate = onAfterAd
        )
    }

    /**
     * Extension-like utility to setup hardware back press interstitial.
     */
    fun setupHardwareBackPressInterstitial(
        activity: AppCompatActivity,
        adMobManager: AdMobManager,
        shouldShow: Boolean,
        adId: String,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String = "back_inter",
        ignoreFrequency: Boolean = false
    ) {
        activity.onBackPressedDispatcher.addCallback(activity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Disable this callback so the next back press (programmatic) goes to the system or default handler
                this.isEnabled = false
                showBackPressInterstitial(
                    activity = activity,
                    adMobManager = adMobManager,
                    shouldShow = shouldShow,
                    adId = adId,
                    analyticsManager = analyticsManager,
                    eventNamePrefix = eventNamePrefix,
                    ignoreFrequency = ignoreFrequency
                ) {
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}
