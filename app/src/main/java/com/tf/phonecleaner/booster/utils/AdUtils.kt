package com.tf.phonecleaner.booster.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.SystemClock
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.LifecycleCoroutineScope
import com.facebook.shimmer.ShimmerFrameLayout
import com.umer_tf.ads.domain.ads.native_ad.NativeAdBuilder
import com.umer_tf.ads.domain.core.AdMobManager
import com.umer_tf.ads.domain.utils.LoadingDialogUtil
import com.tf.phonecleaner.booster.R
import com.tf.phonecleaner.booster.app.AdIds
import com.tf.phonecleaner.booster.remoteconfig.RemoteConfigManager
import com.tf.phonecleaner.booster.remoteconfig.data.NativeAdConfigData
import com.tf.phonecleaner.booster.app.AnalyticsManager

import kotlinx.coroutines.launch

/**

Created by Umer Javed
Senior Android Developer
Created on 02/09/2025 3:00 pm
Email: umerr8019@gmail.com

 */
object AdUtils {

    fun applyCtaBgFallback(
        adContainer: FrameLayout,
        colorValue: String,
        ctaButtonId: Int = R.id.ad_call_to_action
    ) {
        val parsedColor = runCatching { Color.parseColor(colorValue) }.getOrNull() ?: return
        adContainer.post {
            val cta = adContainer.findViewById<AppCompatButton>(ctaButtonId) ?: return@post
            val tint = ColorStateList.valueOf(parsedColor)
            cta.backgroundTintList = tint
            cta.supportBackgroundTintList = tint
        }
    }

    fun applyAdBackgroundColorFallback(
        adContainer: FrameLayout,
        colorValue: String
    ) {
        val parsedColor = runCatching { Color.parseColor(colorValue) }.getOrNull() ?: return
        adContainer.post {
            val clAd = adContainer.findViewById<android.view.View>(R.id.clAd)
            clAd?.setBackgroundColor(parsedColor)
            val llBottomPanel = adContainer.findViewById<android.view.View>(R.id.llBottomPanel)
            llBottomPanel?.setBackgroundColor(parsedColor)
        }
    }

    /**
     * Solid, guaranteed fallback specifically for ViewPager2 Native Ads (Onboarding) and Splash
     * It bypasses the SDK builder completely and forcibly applies all colors immediately when the NativeAdView inflates.
     */
    fun solidApplyNativeAdColors(
        adContainer: FrameLayout,
        nativeConfig: NativeAdConfigData?
    ) {
        if (nativeConfig == null) return

        val applyColors = {
            val view = adContainer
            // Background
            val parsedBgColor = runCatching { Color.parseColor(nativeConfig.backgroundColor) }.getOrNull()
            if (parsedBgColor != null) {
                view.findViewById<android.view.View>(R.id.clAd)?.setBackgroundColor(parsedBgColor)
                view.findViewById<android.view.View>(R.id.llBottomPanel)?.setBackgroundColor(parsedBgColor)
            }
            // CTA Background & Text Color
            val parsedCtaBg = runCatching { Color.parseColor(nativeConfig.callActionButtonColor) }.getOrNull()
            val parsedCtaText = runCatching { Color.parseColor(nativeConfig.ctaText) }.getOrNull()
            val cta = view.findViewById<AppCompatButton>(R.id.ad_call_to_action)
            if (cta != null) {
                if (parsedCtaBg != null) {
                    val tint = ColorStateList.valueOf(parsedCtaBg)
                    cta.backgroundTintList = tint
                    cta.supportBackgroundTintList = tint
                }
                if (parsedCtaText != null) {
                    cta.setTextColor(parsedCtaText)
                }
            }
            // Title Text Color
            val parsedTitle = runCatching { Color.parseColor(nativeConfig.heading) }.getOrNull()
            if (parsedTitle != null) {
                view.findViewById<android.widget.TextView>(R.id.ad_headline)?.setTextColor(parsedTitle)
            }
            // Body Text Color
            val parsedBody = runCatching { Color.parseColor(nativeConfig.description) }.getOrNull()
            if (parsedBody != null) {
                view.findViewById<android.widget.TextView>(R.id.ad_body)?.setTextColor(parsedBody)
            }
        }

        applyColors()

        adContainer.setOnHierarchyChangeListener(object : android.view.ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: android.view.View?, child: android.view.View?) {
                applyColors()
            }
            override fun onChildViewRemoved(parent: android.view.View?, child: android.view.View?) {}
        })
    }

    fun loadAndShowInterAdWithDialog(
        adMobManager: AdMobManager,
        activity: AppCompatActivity,
        adUnit: String,
        lifecycleScope: LifecycleCoroutineScope,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {   // Never show ads to premium users
        if (AdMobManager.isPremium) {
            onComplete(false)
            return
        }
        // Frequency control check
        if (!AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)) {
            onComplete(false)
            return
        }
        val loadingDialog = LoadingDialogUtil.create(activity)
        loadingDialog.showLoadingDialog()
        
        eventNamePrefix?.let { prefix ->
            analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_request")
        }
        
        adMobManager.interstitialAdLoader.loadAd(adUnit) { isLoaded ->
            lifecycleScope.launch {
                loadingDialog.hideLoadingDialog()
                if (isLoaded && !activity.isFinishing && !activity.isDestroyed) {
                    eventNamePrefix?.let { prefix ->
                        analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_pass")
                        analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_view")
                    }
                    // FIX: Show ad FIRST, finish inside callback.
                    // Previously finish() was called BEFORE showAd(), so the ad
                    // was displayed on an already-destroyed window → crash.
                    adMobManager.interstitialAdLoader.showAd(activity, adUnit) {
                        AdFrequencyControl.recordAdShown(
                            activity,
                            AdUnitFrequencyController.UNIT_INTERSTITIAL
                        )
                        onComplete(true)
                    }
                } else {
                    eventNamePrefix?.let { prefix ->
                        analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_fail")
                    }
                    onComplete(false)
                }
            }
        }
    }

    private var lastInterAdTime: Long = 0

    fun loadAndShowAdWithTimer(
        activity: AppCompatActivity,
        adMobManager: AdMobManager,
        adUnit: String = AdIds.getInterstitialAdID(),
        timeOut: Long = 30000,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        onNavigate: () -> Unit
    ) {
        // Never show ads to premium users
        if (AdMobManager.isPremium) {
            onNavigate()
            return
        }

        // Frequency control check
        if (!AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)) {
            onNavigate()
            return
        }

        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastInterAdTime > timeOut) {
            eventNamePrefix?.let { prefix ->
                analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_request")
            }
            adMobManager.interstitialAdLoader.loadAndShowAd(
                activity,
                adUnit,
                true
            ) { isShown ->
                if (isShown) {
                    eventNamePrefix?.let { prefix ->
                        analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_pass")
                        analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_view")
                    }
                    lastInterAdTime = SystemClock.elapsedRealtime()
                    AdFrequencyControl.recordAdShown(
                        activity,
                        AdUnitFrequencyController.UNIT_INTERSTITIAL
                    )
                } else {
                    eventNamePrefix?.let { prefix ->
                        analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_fail")
                    }
                }
                onNavigate()
            }
        } else {
            onNavigate()
        }
    }


    fun loadAndShowWaterfallInterAdWithDialog(
        activity: AppCompatActivity,
        adMobManager: AdMobManager,
        hfAdUnit: String,
        normalAdUnit: String,
        lifecycleScope: LifecycleCoroutineScope,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        // Never show ads to premium users
        if (AdMobManager.isPremium) {
            onComplete(false)
            return
        }
        // Frequency control check
        if (!AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)) {
            onComplete(false)
            return
        }

        val loadingDialog = LoadingDialogUtil.create(activity)
        loadingDialog.showLoadingDialog()

        eventNamePrefix?.let { prefix ->
            analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_request")
        }

        android.util.Log.d("Waterfall", "Attempting High Floor Ad (ID: $hfAdUnit)")
        adMobManager.interstitialAdLoader.loadAd(hfAdUnit) { isHfLoaded ->
            if (isHfLoaded) {
                android.util.Log.d("Waterfall", "High Floor Ad loaded successfully.")
                lifecycleScope.launch {
                    loadingDialog.hideLoadingDialog()
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        eventNamePrefix?.let { prefix ->
                            analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_pass")
                            analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_view")
                        }
                        adMobManager.interstitialAdLoader.showAd(activity, hfAdUnit) {
                            AdFrequencyControl.recordAdShown(
                                activity,
                                AdUnitFrequencyController.UNIT_INTERSTITIAL
                            )
                            onComplete(true)
                        }
                    } else {
                        eventNamePrefix?.let { prefix ->
                            analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_fail")
                        }
                        onComplete(false)
                    }
                }
            } else {
                android.util.Log.d("Waterfall", "High Floor Ad failed. Falling back to Simple Ad.")
                android.util.Log.d("Waterfall", "Attempting Simple Ad (ID: $normalAdUnit)")
                adMobManager.interstitialAdLoader.loadAd(normalAdUnit) { isNormalLoaded ->
                    lifecycleScope.launch {
                        loadingDialog.hideLoadingDialog()
                        if (isNormalLoaded && !activity.isFinishing && !activity.isDestroyed) {
                            eventNamePrefix?.let { prefix ->
                                analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_pass")
                                analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_view")
                            }
                            android.util.Log.d("Waterfall", "Simple Ad loaded successfully.")
                            adMobManager.interstitialAdLoader.showAd(activity, normalAdUnit) {
                                AdFrequencyControl.recordAdShown(
                                    activity,
                                    AdUnitFrequencyController.UNIT_INTERSTITIAL
                                )
                                onComplete(true)
                            }
                        } else {
                            eventNamePrefix?.let { prefix ->
                                analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_fail")
                            }
                            android.util.Log.d(
                                "Waterfall",
                                "Simple Ad failed. Proceeding without ad."
                            )
                            onComplete(false)
                        }
                    }
                }
            }
        }
    }

    fun loadAndShowNativeAd(
        adMobManager: AdMobManager,
        adUnitId: String,
        layoutResId: Int,
        frameLayout: FrameLayout,
        shimmerFrameLayout: ShimmerFrameLayout,
        showMedia: Boolean = true,
        nativeConfig: NativeAdConfigData? = null,
        forceLoadNew: Boolean = false,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        onAdLoaded: ((Boolean) -> Unit)? = null
    ) {
        if (AdMobManager.isPremium) {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            frameLayout.visibility = android.view.View.GONE
            onAdLoaded?.invoke(false)
            return
        }
        if (!AdFrequencyControl.canShowAd(
                frameLayout.context,
                AdUnitFrequencyController.UNIT_NATIVE
            )
        ) {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            frameLayout.visibility = android.view.View.GONE
            onAdLoaded?.invoke(false)
            return
        }

        val builder = NativeAdBuilder.Builder(
            layoutResId,
            frameLayout,
            shimmerFrameLayout
        ).setShowMedia(showMedia)
            .setShowBody(true)
            .setIconEnabled(true)
            .setShowRating(false)

        nativeConfig?.let {
            builder.setAdTitleColor(it.heading)
            builder.setAdBodyColor(it.description)
            builder.setCtaTextColor(it.ctaText)
            builder.setCtaBgColor(it.callActionButtonColor)
        }

        if (!forceLoadNew && adMobManager.nativeAdLoader.isAdLoaded()) {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            frameLayout.visibility = android.view.View.VISIBLE

            adMobManager.nativeAdLoader.showLoadedAd(builder.build(), adUnitId)
            nativeConfig?.callActionButtonColor?.let { color ->
                applyCtaBgFallback(frameLayout, color)
            }
            nativeConfig?.backgroundColor?.let { color ->
                applyAdBackgroundColorFallback(frameLayout, color)
            }
            AdFrequencyControl.recordAdShown(
                frameLayout.context,
                AdUnitFrequencyController.UNIT_NATIVE
            )
            onAdLoaded?.invoke(true)
            return
        }

        frameLayout.visibility = android.view.View.GONE
        shimmerFrameLayout.visibility = android.view.View.VISIBLE
        shimmerFrameLayout.startShimmer()
        
        eventNamePrefix?.let { prefix ->
            analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_request")
        }

        adMobManager.nativeAdLoader.loadAndShow(
            adUnitId,
            builder.build()
        ) { success ->
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            if (success) {
                eventNamePrefix?.let { prefix ->
                    analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_pass")
                    analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_view")
                }
                frameLayout.visibility = android.view.View.VISIBLE
                nativeConfig?.callActionButtonColor?.let { color ->
                    applyCtaBgFallback(frameLayout, color)
                }
                nativeConfig?.backgroundColor?.let { color ->
                    applyAdBackgroundColorFallback(frameLayout, color)
                }
                AdFrequencyControl.recordAdShown(
                    frameLayout.context,
                    AdUnitFrequencyController.UNIT_NATIVE
                )
            } else {
                eventNamePrefix?.let { prefix ->
                    analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_fail")
                }
                frameLayout.visibility = android.view.View.GONE
            }
            onAdLoaded?.invoke(success)
        }
    }

    fun loadAndShowFullScreenNativeAdWithDialog(
        activity: AppCompatActivity,
        adUnitId: String,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        nativeConfig: NativeAdConfigData? = null,
        onComplete: (Boolean) -> Unit
    ) {
        onComplete(false)
    }
}
