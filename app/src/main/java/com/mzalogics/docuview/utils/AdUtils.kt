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

    fun showAdWithTimer(
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


    fun loadAndShowNativeAd(adMobManager: AdMobManager, frameLayout: FrameLayout,shimmerFrameLayout: ShimmerFrameLayout) {
        if (AdMobManager.isPremium || RemoteConfigManager.shouldShowAds()) {
            if (!AdFrequencyControl.canShowAd(frameLayout.context, AdUnitFrequencyController.UNIT_NATIVE)) {
                return
            }
            adMobManager.nativeAdLoader.loadAndShow(
                AdIds.getNativeAdId(), NativeAdBuilder.Builder(
                    R.layout.native_ad_onboarding,
                    frameLayout,
                    shimmerFrameLayout
                ).setShowMedia(RemoteConfigManager.getOnBoardingNativeMedia())
                    .setShowBody(true)
                    .setIconEnabled(true)
                    .setShowRating(false)
                    .setShowMedia(true)
                    .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
                    .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
                    .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
                    .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
                    .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
                    //.setAdBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].backgroundColor)
                    .build()
            ) {
                AdFrequencyControl.recordAdShown(frameLayout.context, AdUnitFrequencyController.UNIT_NATIVE)
            }
        }
    }
}
