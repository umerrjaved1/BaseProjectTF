package com.mzalogics.docuview.remoteconfig.data


data class AdsConfigData(
    val interstitialMaxTimer: Int? = null,
    val interstitialMinTimer: Int? = null,
    val interstitialCounter: Int? = null,
    val openAdResumeTimer: Int? = null,
    val firstOpenAdStrategy: Int = 0, // 0 = OpenApp, 1 = Interstitial
    val onBoardingCrossButtonVisible: Boolean = true,
    val onBoardingMonetizationStrategy: Int = 0, // 0 = Main, 1 = PremiumScreen, 2 = Interstitial->MainActivity
    val startupTime: Int = 8,
    val premiumCloseBtnDelay: Int = 3000,
    val clickCountPremiumActivity : Int = 3,
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

    // Ad Frequency Control
    val maxAdsPerDay: Int = 50,              // Per-user daily cap
    val dailyResetHours: Int = 24,           // Reset interval in hours
    val maxAdsPerSession: Int = 10,          // Per-session cap
    val sessionCooldownSeconds: Int = 30,    // Min seconds between any two ads in a session
    val interstitialMaxPerSession: Int = 5,  // Per-unit: interstitial session cap (cooldown uses interstitialMaxTimer)
    val openAdMaxPerSession: Int = 3,        // Per-unit: open ad session cap (cooldown uses openAdResumeTimer)
    val bannerMaxPerSession: Int = Int.MAX_VALUE, // Per-unit: banner (effectively unlimited)
    val bannerCooldownSeconds: Int = 0,      // Per-unit: banner cooldown
    val nativeMaxPerSession: Int = Int.MAX_VALUE, // Per-unit: native ad session cap
    val nativeCooldownSeconds: Int = 0,      // Per-unit: native ad cooldown
)
