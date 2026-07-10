package com.tf.gpsmapcamera.utils

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
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.remoteconfig.data.NativeAdConfigData
import com.tf.gpsmapcamera.app.AnalyticsManager
import androidx.activity.OnBackPressedCallback

import kotlinx.coroutines.launch

/**

Created by Umer Javed
Senior Android Developer
Created on 02/09/2025 3:00 pm
Email: umerr8019@gmail.com

 */
object AdUtils {

    /**
     * Master switch for ads. Checks both premium status and global remote config.
     */
    fun areAdsEnabled(): Boolean {
        if (AdMobManager.isPremium) return false
        return RemoteConfigManager.getShowAds()
    }

    fun applyCtaBgFallback(
        adContainer: FrameLayout,
        colorValue: String,
        ctaButtonId: Int = R.id.ad_call_to_action
    ) {
        AdUIHelper.applyCtaBgFallback(adContainer, colorValue, ctaButtonId)
    }

    fun applyAdBackgroundColorFallback(
        adContainer: FrameLayout,
        colorValue: String
    ) {
        AdUIHelper.applyAdBackgroundColorFallback(adContainer, colorValue)
    }

    /**
     * Solid, guaranteed fallback specifically for ViewPager2 Native Ads (Onboarding) and Splash
     * It bypasses the SDK builder completely and forcibly applies all colors immediately when the NativeAdView inflates.
     */
    fun solidApplyNativeAdColors(
        adContainer: FrameLayout,
        nativeConfig: NativeAdConfigData?
    ) {
        AdUIHelper.solidApplyNativeAdColors(adContainer, nativeConfig)
    }

    fun loadAndShowInterAdWithDialog(
        adMobManager: AdMobManager,
        activity: AppCompatActivity,
        adUnit: String,
        lifecycleScope: LifecycleCoroutineScope,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        ignoreFrequency: Boolean = false,
        onComplete: (Boolean) -> Unit = {}
    ) {   // Never show ads to premium users
        if (!areAdsEnabled()) {
            activity.finish()
            return
        }
        // Frequency control check
        if (!ignoreFrequency && !AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)) {
            activity.finish()
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
                        analyticsManager?.logMetaAdImpression()
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
        timeOut: Long = RemoteConfigManager.getGlobalAdRulesConfig().interstitialMinTimer * 1000L,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        ignoreFrequency: Boolean = false,
        onNavigate: () -> Unit
    ) {
        // Never show ads to premium users
        if (!areAdsEnabled()) {
            onNavigate()
            return
        }

        // Frequency control check
        if (!ignoreFrequency && !AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)) {
            onNavigate()
            return
        }

        val currentTime = SystemClock.elapsedRealtime()
        if (ignoreFrequency || currentTime - lastInterAdTime > timeOut) {
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
                        analyticsManager?.logMetaAdImpression()
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
        if (!areAdsEnabled()) {
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
                            analyticsManager?.logMetaAdImpression()
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
                                analyticsManager?.logMetaAdImpression()
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
        showInfoIcon: Boolean = true,
        nativeConfig: NativeAdConfigData? = null,
        forceLoadNew: Boolean = false,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        onAdLoaded: ((Boolean) -> Unit)? = null
    ) {
        if (!areAdsEnabled()) {
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
            frameLayout.findViewById<android.widget.ImageView>(R.id.iv_ad_info)?.visibility =
                if (showInfoIcon) android.view.View.VISIBLE else android.view.View.GONE
            AdFrequencyControl.recordAdShown(
                frameLayout.context,
                AdUnitFrequencyController.UNIT_NATIVE
            )
            onAdLoaded?.invoke(true)
            return
        }

        frameLayout.visibility = android.view.View.GONE
        if (forceLoadNew) {
            frameLayout.removeAllViews()
        }
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
                    analyticsManager?.logMetaAdImpression()
                }
                frameLayout.visibility = android.view.View.VISIBLE
                nativeConfig?.callActionButtonColor?.let { color ->
                    applyCtaBgFallback(frameLayout, color)
                }
                nativeConfig?.backgroundColor?.let { color ->
                    applyAdBackgroundColorFallback(frameLayout, color)
                }
                frameLayout.findViewById<android.widget.ImageView>(R.id.iv_ad_info)?.visibility =
                    if (showInfoIcon) android.view.View.VISIBLE else android.view.View.GONE
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
        if (!areAdsEnabled()) {
            onComplete(false)
            return
        }
        if (!AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_NATIVE)) {
            onComplete(false)
            return
        }

        val dialog = android.app.Dialog(activity, R.style.Theme_App)
        dialog.setContentView(R.layout.layout_full_native_ad)
        dialog.setCancelable(false)

        dialog.window?.let {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            it.navigationBarColor = android.graphics.Color.TRANSPARENT
        }

        val adFrame = dialog.findViewById<FrameLayout>(R.id.adFrame)
        val shimmerFbAd = dialog.findViewById<ShimmerFrameLayout>(R.id.shimmerFbAd)
        val fallbackContainer =
            dialog.findViewById<android.widget.LinearLayout>(R.id.fallbackContainer)
        val btnContinue = dialog.findViewById<android.widget.TextView>(R.id.btnContinue)
        val btnCloseAd = dialog.findViewById<android.widget.ImageView>(R.id.btnCloseAd)

        dialog.show()

        btnCloseAd.setOnClickListener {
            dialog.dismiss()
            onComplete(true)
        }

        btnContinue.setOnClickListener {
            dialog.dismiss()
            onComplete(false)
        }

        val builder = NativeAdBuilder.Builder(
            R.layout.full_native_ad_design,
            adFrame,
            shimmerFbAd
        ).setShowMedia(true).setShowBody(true).setShowRating(false).setIconEnabled(true)

        nativeConfig?.let {
            builder.setAdTitleColor(it.heading)
            builder.setAdBodyColor(it.description)
            builder.setCtaTextColor(it.ctaText)
            builder.setCtaBgColor(it.callActionButtonColor)
        }

        val loader = com.umer_tf.ads.domain.ads.native_ad.NativeAd(activity)
        eventNamePrefix?.let { prefix ->
            analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_request")
        }
        loader.loadAndShow(adUnitId, builder.build()) { success ->
            if (success) {
                eventNamePrefix?.let { prefix ->
                    analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_pass")
                    analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_view")
                    analyticsManager?.logMetaAdImpression()
                }
                btnCloseAd.visibility = android.view.View.VISIBLE
                AdFrequencyControl.recordAdShown(activity, AdUnitFrequencyController.UNIT_NATIVE)
                
                AdUtils.solidApplyNativeAdColors(adFrame, nativeConfig)
            } else {
                eventNamePrefix?.let { prefix ->
                    analyticsManager?.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${prefix}_fail")
                }
                shimmerFbAd.stopShimmer()
                shimmerFbAd.visibility = android.view.View.GONE
                adFrame.visibility = android.view.View.GONE
                fallbackContainer.visibility = android.view.View.VISIBLE
                btnContinue.visibility = android.view.View.VISIBLE
                btnCloseAd.visibility = android.view.View.VISIBLE

                dialog.dismiss()
                onComplete(false)
            }
        }
    }

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
        AdNavigationHelper.showBackPressInterstitial(
            activity, adMobManager, shouldShow, adId, analyticsManager, eventNamePrefix, ignoreFrequency, onAfterAd
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
        AdNavigationHelper.setupHardwareBackPressInterstitial(
            activity, adMobManager, shouldShow, adId, analyticsManager, eventNamePrefix, ignoreFrequency
        )
    }
}
