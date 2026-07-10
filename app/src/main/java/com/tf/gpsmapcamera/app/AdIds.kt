package com.tf.gpsmapcamera.app

/**
 * Created by Umer Javed
 * Senior Android Developer
 * Created on 11/06/2025 7:41 pm
 * Email: umerr8019@gmail.com
 */


import com.tf.gpsmapcamera.BuildConfig
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager



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
        const val BEFORE_HOME_NATIVE = "ca-app-pub-5972202469838280/3977927541"
        const val BEFORE_HOME_INTER = "ca-app-pub-5972202469838280/2581085393"
        const val BEFORE_HOME_INTER_HF = "ca-app-pub-5972202469838280/6112159929"
        const val BEFORE_HOME_APP_OPEN = "ca-app-pub-5972202469838280/8856895302"
        const val BEFORE_HOME_FS_NATIVE = "ca-app-pub-5972202469838280/6193027345"

        const val AFTER_HOME_INTER = "ca-app-pub-5972202469838280/3406660447"
        const val AFTER_HOME_BANNER = "ca-app-pub-5972202469838280/9940700666"
        const val AFTER_HOME_NATIVE = "ca-app-pub-5972202469838280/8084272059"
        const val APP_RESUME_AD_ID = "ca-app-pub-5972202469838280/6962762078"
        const val AFTER_HOME_REW = "ca-app-pub-5972202469838280/8905152293"

    }

    private fun getAdId(remoteId: String?, releaseId: String, debugId: String): String {
        return if (isDebug) debugId else remoteId ?: releaseId
    }

    fun getAppOpenAdId() = getAdId(
        remoteConfigs.getStartScreenConfig().appOpenAdID,
        RELEASE.BEFORE_HOME_APP_OPEN,
        DEBUG.APP_OPEN_AD_ID
    )

    fun getAppResumeAdId() = getAdId(
        remoteConfigs.getGlobalAdRulesConfig().openAdResumeID,
        RELEASE.APP_RESUME_AD_ID,
        DEBUG.APP_OPEN_AD_ID
    )



    fun getInterstitialAdID() = getAdId(
        remoteConfigs.getHomeScreenConfig().interstitialAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialLanguageID() = getAdId(
        remoteConfigs.getLanguageScreenConfig().interstitialLanguageID,
        RELEASE.BEFORE_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialOnboardingID() = getAdId(
        remoteConfigs.getOnboardingScreenConfig().interstitialOnboardingID,
        RELEASE.BEFORE_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialSurveyID() = getAdId(
        remoteConfigs.getSurveyScreenConfig().interstitialSurveyAdID,
        RELEASE.BEFORE_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialSplashHfAdId() = getAdId(
        remoteConfigs.getStartScreenConfig().interstitialSplashHfAdID,
        RELEASE.BEFORE_HOME_INTER_HF,
        DEBUG.INTERSTITIAL_AD_ID
    )

    fun getInterstitialSplashAdId() = getAdId(
        remoteConfigs.getStartScreenConfig().interstitialSplashAdID,
        RELEASE.BEFORE_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )


    fun getNativeLanguageAdId() = getAdId(
        remoteConfigs.getLanguageScreenConfig().nativeLanguageID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getNativeLanguage2AdId() = getAdId(
        remoteConfigs.getLanguageScreenConfig().nativeLanguage2ID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )


    fun getNativeOnboardingAdId() = getAdId(
        remoteConfigs.getOnboardingScreenConfig().nativeOnBoardingAdID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )


    fun getFullNativeOnboardingAdId() = getAdId(
        remoteConfigs.getOnboardingScreenConfig().fullNativeOnBoardingAdID,
        RELEASE.BEFORE_HOME_FS_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getBannerAdIdExit() = getAdId(
        remoteConfigs.getHomeScreenConfig().bannerExitAdID,
        RELEASE.AFTER_HOME_BANNER,
        DEBUG.BANNER_AD_ID
    )

    fun getBannerHomeAdId() = getAdId(
        remoteConfigs.getHomeScreenConfig().bannerHomeAdID,
        RELEASE.AFTER_HOME_BANNER,
        DEBUG.BANNER_AD_ID
    )

    fun getNativeAdId() = getAdId(
        remoteConfigs.getStartScreenConfig().splashNativeAdID,
        RELEASE.BEFORE_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getHomeNativeAdId() = getAdId(
        remoteConfigs.getHomeScreenConfig().nativeAdID,
        RELEASE.AFTER_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )
    
    fun getSurveyNative1AdId() = getAdId(
        remoteConfigs.getSurveyScreenConfig().nativeSurvey1AdID,
        RELEASE.AFTER_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )
    
    fun getSurveyNative2AdId() = getAdId(
        remoteConfigs.getSurveyScreenConfig().nativeSurvey2AdID,
        RELEASE.AFTER_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )

    fun getUninstallNative1AdId() = getAdId(
        remoteConfigs.getUninstallScreenConfig().nativeUninstall1AdID,
        RELEASE.AFTER_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )
    
    fun getUninstallNative2AdId() = getAdId(
        remoteConfigs.getUninstallScreenConfig().nativeUninstall2AdID,
        RELEASE.AFTER_HOME_NATIVE,
        DEBUG.NATIVE_AD_ID
    )
    
    fun getInterstitialUninstallAdId() = getAdId(
        remoteConfigs.getUninstallScreenConfig().interstitialUninstallAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )


    // Home Screen Back Interstitial
    fun getHomeBackInterAdId() = getAdId(
        remoteConfigs.getHomeScreenConfig().backInterstitialAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    // Survey Screen Back Interstitial
    fun getSurveyBackInterAdId() = getAdId(
        remoteConfigs.getSurveyScreenConfig().backInterstitialAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    // Uninstall Screen Back Interstitial
    fun getUninstallBackInterAdId() = getAdId(
        remoteConfigs.getUninstallScreenConfig().backInterstitialAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

    // Premium Screen Back Interstitial
    fun getPremiumBackInterAdId() = getAdId(
        remoteConfigs.getPremiumScreenConfig().backInterstitialAdID,
        RELEASE.AFTER_HOME_INTER,
        DEBUG.INTERSTITIAL_AD_ID
    )

}
