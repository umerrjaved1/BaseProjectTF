package com.tf.gpsmapcamera.remoteconfig.data

import com.google.gson.annotations.SerializedName

data class StartScreenConfig(
    val showAppOpenSplashAd: Boolean = true,
    val showSplashInterstitialAd: Boolean = true,
    val showFullScreenNativeSplashAd: Boolean = false,
    val showSplashNativeAd: Boolean = true,
    val startupTime: Int = 8,
    val showGetStartedButton: Boolean = true,
    @SerializedName("appopen_splash") val appOpenAdID: String? = null,
    @SerializedName("int_splash_hf") val interstitialSplashHfAdID: String? = null,
    @SerializedName("int_splash") val interstitialSplashAdID: String? = null,
    @SerializedName("native_splash") val splashNativeAdID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class LanguageScreenConfig(
    val skipLanguageScreen: Boolean = false,
    val showLanguageNative1: Boolean = true,
    val showLanguageNative2: Boolean = true,
    val showLanguageInterstitial: Boolean = true,
    @SerializedName("native_lng1") val nativeLanguageID: String? = null,
    @SerializedName("int_lng") val interstitialLanguageID: String? = null,
    @SerializedName("native_lng2") val nativeLanguage2ID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class OnboardingScreenConfig(
    val skipOnboardingScreen: Boolean = false,
    val showObSlide1: Boolean = true,
    val showObSlide2: Boolean = true,
    val showObSlide3: Boolean = true,
    val showOb1Native: Boolean = true,
    val showOb2Native: Boolean = true,
    val showOb3Native: Boolean = true,
    val showFullNativeOnBoarding: Boolean = true,
    val showOnboardingInterstitial: Boolean = true,
    val onBoardingCrossButtonVisible: Boolean = true,
    val fullNativeAdPosition: Int = 1,
    val showOnboardingNativeMedia: Boolean = false,
    @SerializedName("native_ob1") val nativeOnBoardingAdID: String? = null,
    @SerializedName("native_fs_ob1") val fullNativeOnBoardingAdID: String? = null,
    @SerializedName("int_onboarding") val interstitialOnboardingID: String? = null,
    @SerializedName("native_ob2") val nativeOb2AdID: String? = null,
    @SerializedName("native_fs_ob2") val nativeFsOb2AdID: String? = null,
    @SerializedName("native_ob3") val nativeOb3AdID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class HomeScreenConfig(
    val showHomeBanner: Boolean = true,
    val showExitBanner: Boolean = true,
    val showHomeNative: Boolean = true,
    val showBackInterstitial: Boolean = true,
    @SerializedName("banner_home") val bannerHomeAdID: String? = null,
    val bannerExitAdID: String? = null,
    @SerializedName("native_home") val nativeAdID: String? = null,
    @SerializedName("int_home") val interstitialAdID: String? = null,
    @SerializedName("rew_home") val rewardedHomeAdID: String? = null,
    @SerializedName("int_procross") val intProCrossAdID: String? = null,
    @SerializedName("int_back_home") val backInterstitialAdID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class SurveyScreenConfig(
    val skipSurveyScreen: Boolean = false,
    val showSurveyNative1: Boolean = true,
    val showSurveyNative2: Boolean = true,
    val showSurveyInterstitial: Boolean = false,
    @SerializedName("native_survey1") val nativeSurvey1AdID: String? = null,
    @SerializedName("native_survey2") val nativeSurvey2AdID: String? = null,
    @SerializedName("int_survey") val interstitialSurveyAdID: String? = null,
    @SerializedName("int_back_survey") val backInterstitialAdID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class UninstallScreenConfig(
    val showUninstallNative1: Boolean = true,
    val showUninstallNative2: Boolean = true,
    val showUninstallInterstitial: Boolean = false,
    val showBackInterstitial: Boolean = true,
    @SerializedName("native_uninstall1") val nativeUninstall1AdID: String? = null,
    @SerializedName("native_uninstall2") val nativeUninstall2AdID: String? = null,
    @SerializedName("int_uninstall") val interstitialUninstallAdID: String? = null,
    @SerializedName("int_back_uninstall") val backInterstitialAdID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class PremiumScreenConfig(
    val skipPremiumScreen: Boolean = false,
    val premiumCloseBtnDelay: Int = 3000,
    val clickCountPremiumActivity: Int = 3,
    val showPremiumActivityOnResume: Boolean = false,
    val showPremiumActivityAfterThreeClick: Boolean = false,
    val showPremiumInterstitial: Boolean = true,
    @SerializedName("int_back_premium") val backInterstitialAdID: String? = null
)

data class GlobalAdRulesConfig(
    val interstitialMaxTimer: Int = 20,
    val interstitialMinTimer: Int = 10,
    val interstitialCounter: Int = 2,
    val showAppOpenAdOnResume: Boolean = true,
    @SerializedName("appopen_resume") val openAdResumeID: String? = null,
    val openAdResumeTimer: Int = 5,
    val enableExitNotification: Boolean = false,
    val exitNotificationTitle: String = "We miss you!",
    val exitNotificationDescription: String = "Come back and explore the live earth map.",

    val sessionCooldownSeconds : Int = 0,
    val dailyResetHours : Int = 24,
    val maxAdsPerDay : Int = 0,
    val maxAdsPerSession : Int = 500,
    val interstitialMaxPerSession : Int = 10,
    val openAdMaxPerSession : Int = 100,
    val bannerMaxPerSession : Int = Integer.MAX_VALUE,
    val bannerCooldownSeconds : Int = 10,
    val  nativeMaxPerSession : Int = 50,
    val nativeCooldownSeconds : Int = 5,
)
