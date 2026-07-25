package com.tf.gpsmapcamera.remoteconfig.data

data class StartScreenConfig(
    val startupTime: Int = 8,
    val showGetStartedButton: Boolean = true
)

data class LanguageScreenConfig(
    val skipLanguageScreen: Boolean = false
)

data class OnboardingScreenConfig(
    val skipOnboardingScreen: Boolean = false,
    val showObSlide1: Boolean = true,
    val showObSlide2: Boolean = true,
    val showObSlide3: Boolean = true,
    val onBoardingCrossButtonVisible: Boolean = true
)

data class SurveyScreenConfig(
    val skipSurveyScreen: Boolean = false
)

data class PremiumScreenConfig(
    val skipPremiumScreen: Boolean = false,
    val premiumCloseBtnDelay: Int = 3000,
    val clickCountPremiumActivity: Int = 3,
    val showPremiumActivityOnResume: Boolean = false,
    val showPremiumActivityAfterThreeClick: Boolean = false
)

data class GlobalConfig(
    val enableExitNotification: Boolean = true,
    val exitNotificationTitle: String = "We miss you!",
    val exitNotificationDescription: String = "Come back and explore the live earth map."
)
