package com.professor.baseproject.app

/**
 * Created by Umer Javed
 * Senior Android Developer
 * Created on 11/06/2025 7:41 pm
 * Email: umerr8019@gmail.com
 */

import com.professor.baseproject.BuildConfig
import com.professor.baseproject.remoteconfig.RemoteConfigManager


object AdIds {

    private val isDebug = BuildConfig.DEBUG
    val remoteConfigs = RemoteConfigManager


    private object DEBUG {
        const val APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
        const val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
        const val BANNER_AD_ID = "ca-app-pub-3940256099942544/9214589741"
        const val NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
        const val REWARD_AD_ID = "ca-app-pub-3940256099942544/5224354917"
    }


    private object RELEASE {
        const val APP_OPEN_AD_ID = "ca-app-pub-6412217023250030/6557887029"
        const val NATIVE_LANGUAGE_AD_ID = "ca-app-pub-6412217023250030/7546183994"
        const val NATIVE_ONBOARDING_AD_ID = "ca-app-pub-6412217023250030/9334457121"
        const val FULL_NATIVE_ONBOARDING_AD_ID = "ca-app-pub-6412217023250030/9202802010"
        const val BANNER_HOME_AD_ID = "ca-app-pub-6412217023250030/9206750025"
        const val NATIVE_SCREEN_AD_ID = "ca-app-pub-6412217023250030/1455967109"
        const val BANNER_SWIPE_AD_ID = "ca-app-pub-6412217023250030/6816073173"
        const val BANNER_INFO_AD_ID = "ca-app-pub-6412217023250030/1241981164"
        const val BANNER_FAV_AD_ID = "ca-app-pub-6412217023250030/1663850663"
        const val EXIT_BANNER_AD_ID = "ca-app-pub-6412217023250030/6233102323"
        const val INTERSTITIAL_AD_ID = "ca-app-pub-6412217023250030/6724605656"


        const val APP_OPEN_RESUME_AD_ID = "ca-app-pub-6412217023250030/1854696007"
        const val INTERSTITIAL_WELCOME_AD_ID = "ca-app-pub-6412217023250030/5602369327"
        const val INTERSTITIAL_SPLASH_AD_ID = "ca-app-pub-6412217023250030/4142047024"

    }

    private fun getAdId(remoteId: String?, releaseId: String, debugId: String): String {
        return if (isDebug) debugId else remoteId ?: releaseId
    }

    fun getAppOpenAdId() = getAdId(
        remoteConfigs.getAdsConfig().appOpenAdID,
        RELEASE.APP_OPEN_AD_ID,
        DEBUG.APP_OPEN_AD_ID
    )

    fun getAppOpenAdIdResume() = getAdId(
        remoteConfigs.getAdsConfig().appOpenResumeAdID,
        RELEASE.APP_OPEN_RESUME_AD_ID,
        DEBUG.APP_OPEN_AD_ID
    )

    fun getInterstitialAdID() = getAdId(
        remoteConfigs.getAdsConfig().interstitialAdID,
        RELEASE.INTERSTITIAL_AD_ID,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialWelcomeAdId() = getAdId(
        remoteConfigs.getAdsConfig().interstitialWelcomeAdID,
        RELEASE.INTERSTITIAL_WELCOME_AD_ID,
        DEBUG.INTERSTITIAL_AD_ID
    )


    fun getInterstitialSplashAdId() = getAdId(
        remoteConfigs.getAdsConfig().interstitialSplashAdID,
        RELEASE.INTERSTITIAL_SPLASH_AD_ID,
        DEBUG.INTERSTITIAL_AD_ID
    )


    fun getNativeLanguageAdId() = getAdId(
        remoteConfigs.getAdsConfig().nativeLanguageID,
        RELEASE.NATIVE_LANGUAGE_AD_ID,
        DEBUG.NATIVE_AD_ID
    )


    fun getNativeOnboardingAdId() = getAdId(
        remoteConfigs.getAdsConfig().nativeOnBoardingAdID,
        RELEASE.NATIVE_ONBOARDING_AD_ID,
        DEBUG.NATIVE_AD_ID
    )


    fun getFullNativeOnboardingAdId() = getAdId(
        remoteConfigs.getAdsConfig().fullNativeOnBoardingAdID,
        RELEASE.FULL_NATIVE_ONBOARDING_AD_ID,
        DEBUG.NATIVE_AD_ID
    )

    fun getBannerAdIdExit() = getAdId(
        remoteConfigs.getAdsConfig().bannerExitAdID,
        RELEASE.EXIT_BANNER_AD_ID,
        DEBUG.BANNER_AD_ID
    )

    fun getBannerHomeAdId() = getAdId(
        remoteConfigs.getAdsConfig().bannerHomeAdID,
        RELEASE.BANNER_HOME_AD_ID,
        DEBUG.BANNER_AD_ID
    )


    fun getBannerInfoAdId() = getAdId(
        remoteConfigs.getAdsConfig().bannerInfoID,
        RELEASE.BANNER_INFO_AD_ID,
        DEBUG.BANNER_AD_ID
    )


    fun getNativeAdId() = getAdId(
        remoteConfigs.getAdsConfig().nativeAdID,
        RELEASE.NATIVE_SCREEN_AD_ID,
        DEBUG.NATIVE_AD_ID
    )


}
