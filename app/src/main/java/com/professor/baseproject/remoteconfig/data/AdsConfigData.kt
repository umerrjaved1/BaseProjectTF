package com.professor.baseproject.remoteconfig.data


data class AdsConfigData(
    val interstitialMaxTimer: Int? = null,
    val interstitialMinTimer: Int? = null,
    val interstitialCounter: Int? = null,
    val openAdResumeTimer: Int? = null,
    val firstOpenAdStrategy: Int = 2, // 0 = None, 1 = Interstitial, 2 = OpenApp
    val onBoardingCrossButtonVisible: Boolean = true,
    val onBoardingMonetizationStrategy: Int = 0, // 0 = Main, 1 = PremiumScreen, 2 = Interstitial->MainActivity
    val startupTime: Int = 8,
    val premiumCloseBtnDelay: Int = 0,
    val nativeConfig: List<NativeAdConfigData> = listOf(NativeAdConfigData()),
    val interstitialDelay: Int = 20,
    val appOpenAdID: String? = null,
    val appOpenResumeAdID: String? = null,
    val interstitialAdID: String? = null,
    val nativeOnBoardingAdID: String? = null,
    val fullNativeOnBoardingAdID: String? = null,
    val interstitialWelcomeAdID: String? = null,//change into Welcome
    val interstitialSplashAdID: String? = null,
    val bannerAdID: String? = null,
    val bannerHomeAdID: String? = null,
    val bannerExitAdID: String? = null,
    val nativeAdvancedID: String? = null,
    val nativeLanguageID: String? = null,
    val nativeAdID: String? = null,
    val bannerInfoID: String? = null,

    )
