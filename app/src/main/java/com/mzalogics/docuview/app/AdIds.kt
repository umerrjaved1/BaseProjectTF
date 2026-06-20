package com.mzalogics.docuview.app

/**
 * Created by Umer Javed
 * Senior Android Developer
 * Created on 11/06/2025 7:41 pm
 * Email: umerr8019@gmail.com
 */

import com.mzalogics.docuview.remoteconfig.RemoteConfigManager
import com.mzalogics.docuview.BuildConfig


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

//    App ID:
//
//    Splash native: ca-app-pub-1066830332084126/8134886369
//    Home Native: ca-app-pub-1066830332084126/4267793540

    private object RELEASE {
        const val APP_OPEN_AD_ID = "ca-app-pub-1066830332084126/3202123882"
        const val NATIVE_LANGUAGE_AD_ID = "ca-app-pub-1066830332084126/8779650554"
        const val NATIVE_ONBOARDING_AD_ID = "ca-app-pub-1066830332084126/5828287228"
        const val FULL_NATIVE_ONBOARDING_AD_ID = "ca-app-pub-1066830332084126/7039494794"
        const val BANNER_HOME_AD_ID = "ca-app-pub-1066830332084126/2017966937"
        const val NATIVE_SCREEN_AD_ID = "ca-app-pub-1066830332084126/4131228023"

        const val EXIT_BANNER_AD_ID = "ca-app-pub-1066830332084126/8044725754"
        const val INTERSTITIAL_AD_ID = "ca-app-pub-1066830332084126/1641630204"
        const val INTERSTITIAL_SPLASH_HF_AD_ID = "ca-app-pub-5972202469838280/5689554336"
        const val INTERSTITIAL_SPLASH_AD_ID = "ca-app-pub-5972202469838280/2630975291"


    }

    private fun getAdId(remoteId: String?, releaseId: String, debugId: String): String {
        return if (isDebug) debugId else remoteId ?: releaseId
    }

    fun getAppOpenAdId() = getAdId(
        remoteConfigs.getStartScreenConfig().appOpenAdID,
        RELEASE.APP_OPEN_AD_ID,
        DEBUG.APP_OPEN_AD_ID
    )



    fun getInterstitialAdID() = getAdId(
        remoteConfigs.getHomeScreenConfig().interstitialAdID,
        RELEASE.INTERSTITIAL_AD_ID,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialLanguageID() = getAdId(
        remoteConfigs.getLanguageScreenConfig().interstitialLanguageID,
        RELEASE.INTERSTITIAL_AD_ID,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialOnboardingID() = getAdId(
        remoteConfigs.getOnboardingScreenConfig().interstitialOnboardingID,
        RELEASE.INTERSTITIAL_AD_ID,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialSplashHfAdId() = getAdId(
        remoteConfigs.getStartScreenConfig().interstitialSplashHfAdID,
        RELEASE.INTERSTITIAL_SPLASH_HF_AD_ID,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialSplashAdId() = getAdId(
        remoteConfigs.getStartScreenConfig().interstitialSplashAdID,
        RELEASE.INTERSTITIAL_SPLASH_AD_ID,
        DEBUG.INTERSTITIAL_AD_ID
    )


    fun getNativeLanguageAdId() = getAdId(
        remoteConfigs.getLanguageScreenConfig().nativeLanguageID,
        RELEASE.NATIVE_LANGUAGE_AD_ID,
        DEBUG.NATIVE_AD_ID
    )


    fun getNativeOnboardingAdId() = getAdId(
        remoteConfigs.getOnboardingScreenConfig().nativeOnBoardingAdID,
        RELEASE.NATIVE_ONBOARDING_AD_ID,
        DEBUG.NATIVE_AD_ID
    )


    fun getFullNativeOnboardingAdId() = getAdId(
        remoteConfigs.getOnboardingScreenConfig().fullNativeOnBoardingAdID,
        RELEASE.FULL_NATIVE_ONBOARDING_AD_ID,
        DEBUG.NATIVE_AD_ID
    )

    fun getBannerAdIdExit() = getAdId(
        remoteConfigs.getHomeScreenConfig().bannerExitAdID,
        RELEASE.EXIT_BANNER_AD_ID,
        DEBUG.BANNER_AD_ID
    )

    fun getBannerHomeAdId() = getAdId(
        remoteConfigs.getHomeScreenConfig().bannerHomeAdID,
        RELEASE.BANNER_HOME_AD_ID,
        DEBUG.BANNER_AD_ID
    )

    fun getNativeAdId() = getAdId(
        remoteConfigs.getStartScreenConfig().splashNativeAdID,
        RELEASE.NATIVE_SCREEN_AD_ID,
        DEBUG.NATIVE_AD_ID
    )


    fun getInterNavigationAdId() = getAdId(
        remoteConfigs.getHomeScreenConfig().interstitialAdID,
        RELEASE.INTERSTITIAL_AD_ID,
        DEBUG.INTERSTITIAL_AD_ID
    )


}
