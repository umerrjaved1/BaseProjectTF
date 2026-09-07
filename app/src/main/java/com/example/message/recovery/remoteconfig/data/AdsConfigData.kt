package com.example.message.recovery.remoteconfig.data


data class AdRules(
    val interstitialMaxTimer: Int = 20,
    val interstitialMinTimer: Int = 30,
    val interstitialCounter: Int = 1,
    val showAppOpenAdOnResume: Boolean = true,
    val openAdResumeTimer: Int = 0,
    val enableExitNotification: Boolean = false,
    val exitNotificationTitle: String = "We miss you!",
    val exitNotificationDescription: String = "Come back.",

    val showAppOpenSplashAd: Boolean = true,
    val showSplashInterstitialAd: Boolean = true,
    val showFullScreenNativeSplashAd: Boolean = false,
    /** `app_open_then_inter` (default) or `inter_then_app_open`. */
    val splashAdLoadOrder: String = SplashAdLoadOrder.APP_OPEN_THEN_INTER,

    val showLanguageNative1: Boolean = true,
    val showLanguageNative2: Boolean = true,
    val showLanguageInterstitial: Boolean = true,

    val showOb1Native: Boolean = true,
    val showOb2Native: Boolean = true,
    val showOb3Native: Boolean = true,
    val showFullNativeOnBoarding: Boolean = true,
    val showOnboardingInterstitial: Boolean = false,
    val showOnboardingNativeMedia: Boolean = true,
    val fullNativeAdPosition: Int = 1,

    val showSurveyNative1: Boolean = true,
    val showSurveyNative2: Boolean = true,
    val showSurveyInterstitial: Boolean = true,

    val showBackInterstitial: Boolean = true,
    val showTabInterstitial: Boolean = true,
    val showHomeNative: Boolean = true,

    val showPremiumInterstitial: Boolean = true,

    val showGetStartedButton: Boolean = false,

    val skipLanguageScreen: Boolean = false,
    val skipOnboardingScreen: Boolean = false,
    val skipSurveyScreen: Boolean = false,
    val skipPremiumScreen: Boolean = false,

    val showObSlide1: Boolean = true,
    val showObSlide2: Boolean = true,
    val showObSlide3: Boolean = true,
    val showPremiumAfterSplash: Boolean = true,
    val showPremiumAfterSurvey: Boolean = true,
    val premiumCloseBtnDelay: Int = 0,
    val clickCountPremiumActivity: Int = 3,
    val showPremiumActivityOnResume: Boolean = true,
    val showPremiumActivityAfterThreeClick: Boolean = true,
    val nativeConfig: NativeAdConfigData = NativeAdConfigData(),

    /**
     * Which native ad design each slot renders, as AdsManager layout codes ("1a", "4a", "5a",
     * "large", …). Resolved through `NativeAdLayout.fromOrDefault`, so an unknown or absent value
     * falls back to the default rather than breaking the slot — which is what makes it safe to
     * change these from the Firebase console without shipping a build.
     */
    val languageNativeLayout: String = "large",
    val onboardingNativeLayout: String = "1a",
    val surveyNativeLayout: String = "large",
    val homeNativeLayout: String = "large",
)

object SplashAdLoadOrder {
    const val APP_OPEN_THEN_INTER = "app_open_then_inter"
    const val INTER_THEN_APP_OPEN = "inter_then_app_open"
}

data class AdIdsConfig(
    val appOpenAdID: String? = "ca-app-pub-5972202469838280/5581156399",
    val interstitialSplashAdID: String? = "ca-app-pub-5972202469838280/5263948857",
    val splashNativeAdID: String? = null,
    val nativeLanguageID: String? = "ca-app-pub-5972202469838280/7178711469",
    val nativeLanguage2ID: String? = "ca-app-pub-5972202469838280/8463872973",
    val interstitialLanguageID: String? = "ca-app-pub-5972202469838280/1979309967",
    val nativeOb1AdID: String? = "ca-app-pub-5972202469838280/8782034800",
    val nativeOb2AdID: String? = "ca-app-pub-5972202469838280/8782034800",
    val nativeOb3AdID: String? = "ca-app-pub-5972202469838280/8782034800",
    val nativeOb4AdID: String? = "ca-app-pub-5972202469838280/8782034800",
    val interstitialOnboardingID: String? = "ca-app-pub-5972202469838280/1979309967",
    val nativeSurvey1AdID: String? = "ca-app-pub-5972202469838280/3071945497",
    val nativeSurvey2AdID: String? = "ca-app-pub-5972202469838280/9119433389",
    val interstitialSurveyAdID: String? = "ca-app-pub-5972202469838280/1979309967",
    val interstitialAdID: String? = "ca-app-pub-5972202469838280/6189063429",
    val backInterstitialAdID: String? = "ca-app-pub-5972202469838280/5998848975",
    val openAdResumeID: String? = "ca-app-pub-5972202469838280/5581156399",
    val premiumBackInterstitialAdID: String? = "ca-app-pub-5972202469838280/6189063429",
    val nativeHomeAdID: String? = "ca-app-pub-5972202469838280/8084272059",
    val bannerLanguageID: String? = null,
    val bannerOnboardingID: String? = null,
    val bannerSurveyID: String? = null,
    val bannerHomeID: String? = null,
)
