package com.professor.baseproject.utils

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.ui.screens.LanguageActivity
import com.professor.baseproject.ui.screens.MainActivity
import com.professor.baseproject.ui.screens.OnboardingActivity
import com.professor.baseproject.ui.screens.PremiumActivity
import com.professor.baseproject.ui.screens.SurveyActivity

object StartupNavigationManager {

    enum class Step {
        START, PREMIUM, LANGUAGE, ONBOARDING, SURVEY
    }

    /**
     * Calculates the next screen to show in the startup sequence.
     */
    fun getNextIntent(context: Context, currentStep: Step, appPreferences: AppPreferences): Intent {
        val isLanguageSelected = appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED)
        val isOnboardingDone = appPreferences.getBoolean(AppPreferences.IS_ONBOARDING)
        // Falls back to IS_ONBOARDING so installs that completed the old combined
        // flow are not shown the survey again after an update.
        val isSurveyDone = appPreferences.getBoolean(AppPreferences.IS_SURVEY_DONE, isOnboardingDone)
        val isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)

        val showLanguage =
            !RemoteConfigManager.getLanguageScreenConfig().skipLanguageScreen && !isLanguageSelected
        val showOnboarding =
            !RemoteConfigManager.getOnboardingScreenConfig().skipOnboardingScreen && !isOnboardingDone
        val showSurvey =
            !RemoteConfigManager.getSurveyScreenConfig().skipSurveyScreen && !isSurveyDone
        val showPremium =
            !isPremium && !RemoteConfigManager.getPremiumScreenConfig().skipPremiumScreen

        // Evaluate from current step onwards
        var nextStep: Step? = currentStep
        while (true) {
            nextStep = nextStep?.let { getNextStepEnum(it) }
            when (nextStep) {
                Step.PREMIUM -> {
                    // StartActivity has special logic for premium
                    if (showPremium) {
                        return Intent(
                            context,
                            PremiumActivity::class.java
                        ).putExtra(Constants.EXTRA_PREMIUM_FROM_SPLASH, true)
                    }
                    // If premium is skipped, the loop will just continue with nextStep = Step.PREMIUM,
                    // and the next iteration will fetch getNextStepEnum(Step.PREMIUM), returning the next step.
                }

                Step.LANGUAGE -> if (showLanguage) return Intent(
                    context,
                    LanguageActivity::class.java
                ).apply { putExtra(Constants.EXTRA_LANGUAGE_FROM_START, true) }

                Step.ONBOARDING -> if (showOnboarding) return Intent(
                    context,
                    OnboardingActivity::class.java
                )

                Step.SURVEY -> if (showSurvey) return Intent(context, SurveyActivity::class.java)
                else -> return Intent(context, MainActivity::class.java)
            }
        }
    }

    private fun getNextStepEnum(current: Step): Step? {
        return when (current) {
            Step.START -> Step.PREMIUM
            Step.PREMIUM -> Step.LANGUAGE
            Step.LANGUAGE -> Step.ONBOARDING
            Step.ONBOARDING -> Step.SURVEY
            Step.SURVEY -> null
        }
    }

    /**
     * Navigates to the next screen in the startup sequence and finishes [activity].
     */
    fun navigateNext(
        activity: AppCompatActivity,
        currentStep: Step,
        appPreferences: AppPreferences,
        onBeforeNavigate: (() -> Unit)? = null
    ) {
        val nextIntent = getNextIntent(activity, currentStep, appPreferences)
        onBeforeNavigate?.invoke()
        activity.startActivity(nextIntent)
        activity.finish()
    }
}
