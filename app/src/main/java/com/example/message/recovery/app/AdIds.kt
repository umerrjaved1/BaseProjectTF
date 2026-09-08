package com.example.message.recovery.app

/**
 * Created by Umer Javed
 * Senior Android Developer
 * Created on 11/06/2025 7:41 pm
 * Email: umerr8019@gmail.com
 */


import com.example.message.recovery.BuildConfig
import com.example.message.recovery.remoteconfig.RemoteConfigManager


object AdIds {

    private val isDebug = BuildConfig.DEBUG
    private val remoteConfigs = RemoteConfigManager

    private object DEBUG {
        const val APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
        const val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
        const val NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
        const val BANNER_AD_ID = "ca-app-pub-3940256099942544/9214589741"
    }

    private object RELEASE {
        const val BEFORE_HOME_NATIVE = ""
        const val BEFORE_HOME_INTER = ""
        const val BEFORE_HOME_APP_OPEN = ""
        const val BEFORE_HOME_FS_NATIVE = ""

        const val AFTER_HOME_INTER = ""
        const val AFTER_HOME_NATIVE = ""
        const val APP_RESUME_AD_ID = ""

        const val BEFORE_HOME_BANNER = ""
        const val AFTER_HOME_BANNER = ""
    }

    private fun getAdId(remoteId: String?, releaseId: String, debugId: String): String {
        return if (isDebug) debugId else remoteId ?: releaseId
    }

    private fun ids() = remoteConfigs.getAdIdsConfig()

    fun getAppOpenAdId() = getAdId(
        ids().appOpenAdID,
        RELEASE.BEFORE_HOME_APP_OPEN,
        DEBUG.APP_OPEN_AD_ID
    )

    fun getAppResumeAdId() = getAdId(
        ids().openAdResumeID,
        RELEASE.APP_RESUME_AD_ID,
        DEBUG.APP_OPEN_AD_ID
    )

    fun getInterstitialAdID() = getAdId(
        ids().interstitialAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialLanguageID() = getAdId(
        ids().interstitialLanguageID,
        RELEASE.BEFORE_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialOnboardingID() = getAdId(
        ids().interstitialOnboardingID,
        RELEASE.BEFORE_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialSurveyID() = getAdId(
        ids().interstitialSurveyAdID,
        RELEASE.BEFORE_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialSplashAdId() = getAdId(
        ids().interstitialSplashAdID,
        RELEASE.BEFORE_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getNativeLanguageAdId() = getAdId(
        ids().nativeLanguageID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getObNativeAdId() = getAdId(
        ids().nativeObAdID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getFullNativeOnboardingAdId() = getAdId(
        ids().nativeFSObAdID,
        RELEASE.BEFORE_HOME_FS_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getNativeAdId() = getAdId(
        ids().splashNativeAdID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getSurveyNative1AdId() = getAdId(
        ids().nativeSurvey1AdID,
        RELEASE.AFTER_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getSurveyNative2AdId() = getAdId(
        ids().nativeSurvey2AdID,
        RELEASE.AFTER_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getHomeBackInterAdId() = getAdId(
        ids().backInterstitialAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getHomeInterAd() = getAdId(
        ids().interstitialAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getPremiumBackInterAdId() = getAdId(
        ids().premiumBackInterstitialAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getNativeHomeAdId() = getAdId(
        ids().nativeHomeAdID,
        RELEASE.AFTER_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getBannerLanguageAdId() = getAdId(
        ids().bannerLanguageID,
        RELEASE.BEFORE_HOME_BANNER,
        DEBUG.BANNER_AD_ID
    )

    fun getBannerOnboardingAdId() = getAdId(
        ids().bannerOnboardingID,
        RELEASE.BEFORE_HOME_BANNER,
        DEBUG.BANNER_AD_ID
    )

    fun getBannerSurveyAdId() = getAdId(
        ids().bannerSurveyID,
        RELEASE.BEFORE_HOME_BANNER,
        DEBUG.BANNER_AD_ID
    )

    fun getBannerHomeAdId() = getAdId(
        ids().bannerHomeID,
        RELEASE.AFTER_HOME_BANNER,
        DEBUG.BANNER_AD_ID
    )

}