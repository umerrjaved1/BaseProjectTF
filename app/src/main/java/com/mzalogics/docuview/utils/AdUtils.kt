package com.mzalogics.docuview.utils

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
import com.mzalogics.docuview.R
import com.mzalogics.docuview.app.AdIds
import com.mzalogics.docuview.remoteconfig.RemoteConfigManager

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

    fun loadAndShowInterAdWithDialog(
        adMobManager: AdMobManager,
        activity: AppCompatActivity,
        adUnit: String,
        lifecycleScope: LifecycleCoroutineScope,
    ) {
        // Never show ads to premium users
        if (AdMobManager.isPremium || !RemoteConfigManager.shouldShowAds()) {
            activity.finish()
            return
        }
        // Frequency control check
        if (!AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)) {
            activity.finish()
            return
        }
        val loadingDialog = LoadingDialogUtil.create(activity)
        loadingDialog.showLoadingDialog()
        adMobManager.interstitialAdLoader.loadAd(adUnit) { isLoaded ->
            lifecycleScope.launch {
                loadingDialog.hideLoadingDialog()
                if (isLoaded && !activity.isFinishing && !activity.isDestroyed) {
                    // FIX: Show ad FIRST, finish inside callback.
                    // Previously finish() was called BEFORE showAd(), so the ad
                    // was displayed on an already-destroyed window → crash.
                    adMobManager.interstitialAdLoader.showAd(activity, adUnit) {
                        AdFrequencyControl.recordAdShown(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)
                        activity.finish()
                    }
                } else {
                    activity.finish()
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
        onNavigate: () -> Unit
    ) {
        // Never show ads to premium users
        if (AdMobManager.isPremium || !RemoteConfigManager.shouldShowAds()) {
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
            adMobManager.interstitialAdLoader.loadAndShowAd(
                activity,
                adUnit,
                true
            ) {
                lastInterAdTime = SystemClock.elapsedRealtime()
                AdFrequencyControl.recordAdShown(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)
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
        onComplete: (Boolean) -> Unit
    ) {
        // Never show ads to premium users
        if (AdMobManager.isPremium || !RemoteConfigManager.shouldShowAds()) {
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

        android.util.Log.d("Waterfall", "Attempting High Floor Ad (ID: $hfAdUnit)")
        adMobManager.interstitialAdLoader.loadAd(hfAdUnit) { isHfLoaded ->
            if (isHfLoaded) {
                android.util.Log.d("Waterfall", "High Floor Ad loaded successfully.")
                lifecycleScope.launch {
                    loadingDialog.hideLoadingDialog()
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        adMobManager.interstitialAdLoader.showAd(activity, hfAdUnit) {
                            AdFrequencyControl.recordAdShown(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)
                            onComplete(true)
                        }
                    } else {
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
                            android.util.Log.d("Waterfall", "Simple Ad loaded successfully.")
                            adMobManager.interstitialAdLoader.showAd(activity, normalAdUnit) {
                                AdFrequencyControl.recordAdShown(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)
                                onComplete(true)
                            }
                        } else {
                            android.util.Log.d("Waterfall", "Simple Ad failed. Proceeding without ad.")
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
        nativeAdConfigIndex: Int = 0,
        forceLoadNew: Boolean = false,
        onAdLoaded: ((Boolean) -> Unit)? = null
    ) {
        if (AdMobManager.isPremium || !RemoteConfigManager.shouldShowAds()) {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            frameLayout.visibility = android.view.View.GONE
            onAdLoaded?.invoke(false)
            return
        }
        if (!AdFrequencyControl.canShowAd(frameLayout.context, AdUnitFrequencyController.UNIT_NATIVE)) {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            frameLayout.visibility = android.view.View.GONE
            onAdLoaded?.invoke(false)
            return
        }
        
        val nativeConfig = RemoteConfigManager.getAdsConfig().nativeConfig.getOrNull(nativeAdConfigIndex)
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
            // it.backgroundColor can be applied if needed
        }

        if (!forceLoadNew && adMobManager.nativeAdLoader.isAdLoaded()) {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            frameLayout.visibility = android.view.View.VISIBLE

            adMobManager.nativeAdLoader.showLoadedAd(builder.build(), adUnitId)
            nativeConfig?.callActionButtonColor?.let { color ->
                applyCtaBgFallback(frameLayout, color)
            }
            AdFrequencyControl.recordAdShown(frameLayout.context, AdUnitFrequencyController.UNIT_NATIVE)
            onAdLoaded?.invoke(true)
            return
        }

        frameLayout.visibility = android.view.View.GONE
        shimmerFrameLayout.visibility = android.view.View.VISIBLE
        shimmerFrameLayout.startShimmer()

        adMobManager.nativeAdLoader.loadAndShow(
            adUnitId,
            builder.build()
        ) { success ->
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            if (success) {
                frameLayout.visibility = android.view.View.VISIBLE
                nativeConfig?.callActionButtonColor?.let { color ->
                    applyCtaBgFallback(frameLayout, color)
                }
                AdFrequencyControl.recordAdShown(frameLayout.context, AdUnitFrequencyController.UNIT_NATIVE)
            } else {
                frameLayout.visibility = android.view.View.GONE
            }
            onAdLoaded?.invoke(success)
        }
    }

    fun loadAndShowFullScreenNativeAdWithDialog(
        activity: AppCompatActivity,
        adUnitId: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (AdMobManager.isPremium || !RemoteConfigManager.shouldShowAds()) {
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
        val fallbackContainer = dialog.findViewById<android.widget.LinearLayout>(R.id.fallbackContainer)
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

        val nativeConfig = RemoteConfigManager.getAdsConfig().nativeConfig.getOrNull(0)
        nativeConfig?.let {
            builder.setAdTitleColor(it.heading)
            builder.setAdBodyColor(it.description)
            builder.setCtaTextColor(it.ctaText)
            builder.setCtaBgColor(it.callActionButtonColor)
        }

        val loader = com.umer_tf.ads.domain.ads.native_ad.NativeAd(activity)
        loader.loadAndShow(adUnitId, builder.build()) { success ->
            if (success) {
                btnCloseAd.visibility = android.view.View.VISIBLE
                AdFrequencyControl.recordAdShown(activity, AdUnitFrequencyController.UNIT_NATIVE)
            } else {
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
}
