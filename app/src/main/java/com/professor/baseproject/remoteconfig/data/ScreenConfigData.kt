package com.professor.baseproject.remoteconfig.data

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

/**
 * Defaults here are neutral placeholders. They are the copy a fork ships if Remote
 * Config has not been populated yet, so they must not name a specific product —
 * exitNotificationDescription used to read "Come back and explore the live earth map."
 */
data class GlobalConfig(
    val enableExitNotification: Boolean = true,
    val exitNotificationTitle: String = "We miss you!",
    val exitNotificationDescription: String = "Come back and pick up where you left off."
)
