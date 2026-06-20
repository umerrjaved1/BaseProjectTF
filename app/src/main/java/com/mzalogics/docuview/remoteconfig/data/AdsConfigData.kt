package com.mzalogics.docuview.remoteconfig.data

data class StartScreenConfig(
    val showAppOpenResumeAd: Boolean = true,
    val showAppOpenSplashAd: Boolean = true,
    val showSplashNativeAd: Boolean = true,
    val showWelcomeInterstitialAd: Boolean = true,
    val startupTime: Int = 8,
    val firstOpenAdStrategy: Int = 2,
    val splashAdPostNavigationStrategy: Int = 1,
    val showGetStartedButton: Boolean = true,
    val appOpenAdID: String? = null,
    val interstitialSplashHfAdID: String? = null,
    val interstitialSplashAdID: String? = null,
    val splashNativeAdID: String? = null,
    val interstitialWelcomeAdID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class LanguageScreenConfig(
    val showLanguageNative1: Boolean = true,
    val showLanguageNative2: Boolean = true,
    val showLanguageInterstitial: Boolean = false,
    val showLangNativeMedia: Boolean = false,
    val languageScreenInterstitialStrategy: Int = 0,
    val nativeLanguageID: String? = null,
    val interstitialLanguageID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class OnboardingScreenConfig(
    val showOb1Native: Boolean = true,
    val showOb2Native: Boolean = true,
    val showOb3Native: Boolean = true,
    val showOb4Native: Boolean = true,
    val showOb5Native: Boolean = true,
    val showFullNativeOnBoarding: Boolean = true,
    val showOnboardingInterstitial: Boolean = true,
    val onBoardingCrossButtonVisible: Boolean = true,
    val onBoardingMonetizationStrategy: Int = 0,
    val fullNativeAdPosition: Int = 1,
    val showOnboardingNativeMedia: Boolean = false,
    val nativeOnBoardingAdID: String? = null,
    val fullNativeOnBoardingAdID: String? = null,
    val interstitialOnboardingID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class HomeScreenConfig(
    val showHomeBanner: Boolean = true,
    val showExitBanner: Boolean = true,
    val showHomeNative: Boolean = true,
    val showAdvancedNative: Boolean = false,
    val showHomeInterstitial: Boolean = true,
    val showNativeMedia: Boolean = false,
    val bannerHomeAdID: String? = null,
    val bannerExitAdID: String? = null,
    val nativeAdID: String? = null,
    val nativeAdvancedID: String? = null,
    val interstitialAdID: String? = null,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData()
)

data class PremiumScreenConfig(
    val premiumCloseBtnDelay: Int = 3000,
    val clickCountPremiumActivity: Int = 3,
    val showPremiumActivityOnResume: Boolean = false,
    val showPremiumActivityAfterThreeClick: Boolean = false,
    val showPremiumInterstitial: Boolean = false
)

data class GlobalAdRulesConfig(
    val interstitialMaxTimer: Int = 20,
    val interstitialMinTimer: Int = 10,
    val interstitialCounter: Int = 2,
    val interstitialDelay: Int = 20,
    val showAppOpenAdOnResume: Boolean = true,
    val openAdResumeTimer: Int = 10,
    val maxAdsPerDay: Int = 50,
    val dailyResetHours: Int = 24,
    val maxAdsPerSession: Int = 10,
    val sessionCooldownSeconds: Int = 30,
    val interstitialMaxPerSession: Int = 5,
    val openAdMaxPerSession: Int = 3,
    val bannerMaxPerSession: Int = Int.MAX_VALUE,
    val bannerCooldownSeconds: Int = 0,
    val nativeMaxPerSession: Int = 100,
    val nativeCooldownSeconds: Int = 0
)
