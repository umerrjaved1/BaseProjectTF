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
        const val BEFORE_HOME_NATIVE = "ca-app-pub-5972202469838280/7178711469"
        const val BEFORE_HOME_INTER = "ca-app-pub-5972202469838280/2581085393"
        const val BEFORE_HOME_APP_OPEN = "ca-app-pub-5972202469838280/8856895302"
        const val BEFORE_HOME_FS_NATIVE = "ca-app-pub-5972202469838280/6193027345"

        const val AFTER_HOME_INTER = "ca-app-pub-5972202469838280/3406660447"
        const val AFTER_HOME_NATIVE = "ca-app-pub-5972202469838280/8084272059"
        const val APP_RESUME_AD_ID = "ca-app-pub-5972202469838280/5581156399"

        // Banner units. A banner request must use a banner unit: AdMob fails a request whose unit
        // format does not match the request type. The four banner getters below used to fall back
        // to AFTER_HOME_NATIVE, so every banner in release no-filled while debug — which uses
        // DEBUG.BANNER_AD_ID — looked correct. That killed the banner leg of NativeOrBannerAdSlot,
        // i.e. the fallback that exists precisely for when native does not fill.
        //
        // Blank on purpose: this AdMob account has no banner units yet, and inventing an id would
        // reintroduce the same silent failure. Both consumers skip a blank id — BannerAdSlot
        // returns early and NativeOrBannerAdSlot renders nothing — so an unconfigured banner
        // collapses the slot instead of burning a request. To turn the fallback on, either paste
        // real banner unit ids here, or set bannerLanguageID / bannerOnboardingID /
        // bannerSurveyID / bannerHomeID in remote config, which takes precedence.
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

    fun getNativeLanguage2AdId() = getAdId(
        ids().nativeLanguage2ID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getNativeOb1AdId() = getAdId(
        ids().nativeOb1AdID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getNativeOb2AdId() = getAdId(
        ids().nativeOb2AdID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getNativeOb3AdId() = getAdId(
        ids().nativeOb3AdID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getNativeOb4AdId() = getAdId(
        ids().nativeOb4AdID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getFullNativeOnboardingAdId() = getAdId(
        ids().nativeOb2AdID,
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
