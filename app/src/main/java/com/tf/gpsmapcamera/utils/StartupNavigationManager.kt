package com.tf.gpsmapcamera.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.constants.Constants
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.ui.screens.LanguageActivity
import com.tf.gpsmapcamera.ui.screens.MainActivity
import com.tf.gpsmapcamera.ui.screens.OnboardingActivity
import com.tf.gpsmapcamera.ui.screens.PremiumActivity
import com.tf.gpsmapcamera.ui.screens.SurveyActivity
import com.umer_tf.ads.domain.core.AdMobManager

object StartupNavigationManager {

    enum class Step {
        START, PREMIUM, LANGUAGE, ONBOARDING, SURVEY
    }

    /**
     * Calculates the next screen to show in the startup sequence.
     */
    fun getNextIntent(context: Context, currentStep: Step, appPreferences: AppPreferences): Intent {
        val config = RemoteConfigManager.getStartScreenConfig()

        val isLanguageSelected = appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED)
        val isOnboardingDone = appPreferences.getBoolean(AppPreferences.IS_ONBOARDING)
        val isPremium = AdMobManager.isPremium

        val showLanguage = !RemoteConfigManager.getLanguageScreenConfig().skipLanguageScreen && !isLanguageSelected
        val showOnboarding = !RemoteConfigManager.getOnboardingScreenConfig().skipOnboardingScreen && !isOnboardingDone
        val showSurvey = !RemoteConfigManager.getSurveyScreenConfig().skipSurveyScreen && !isOnboardingDone // Assuming survey is part of onboarding flow
        val showPremium = !isPremium && !RemoteConfigManager.getPremiumScreenConfig().skipPremiumScreen

        // Evaluate from current step onwards
        var nextStep: Step? = currentStep
        while (true) {
            nextStep = nextStep?.let { getNextStepEnum(it) }
            when (nextStep) {
                Step.PREMIUM -> {
                    // StartActivity has special logic for premium
                    if (showPremium) {
                        return Intent(context, PremiumActivity::class.java).putExtra(Constants.EXTRA_PREMIUM_FROM_SPLASH, true)
                    }
                    // If premium is skipped, the loop will just continue with nextStep = Step.PREMIUM,
                    // and the next iteration will fetch getNextStepEnum(Step.PREMIUM), returning the next step.
                }
                Step.LANGUAGE -> if (showLanguage) return Intent(context, LanguageActivity::class.java).apply { putExtra(Constants.EXTRA_LANGUAGE_FROM_START, true) }
                Step.ONBOARDING -> if (showOnboarding) return Intent(context, OnboardingActivity::class.java)
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
     * Preloads the native ad for the *next* screen that will be shown.
     */
    fun preloadNextScreenAd(context: Context, currentStep: Step, appPreferences: AppPreferences, adMobManager: AdMobManager) {
        if (AdMobManager.isPremium) return

        val config = RemoteConfigManager.getStartScreenConfig()

        val isLanguageSelected = appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED)
        val isOnboardingDone = appPreferences.getBoolean(AppPreferences.IS_ONBOARDING)

        val showLanguage = !RemoteConfigManager.getLanguageScreenConfig().skipLanguageScreen && !isLanguageSelected
        val showOnboarding = !RemoteConfigManager.getOnboardingScreenConfig().skipOnboardingScreen && !isOnboardingDone
        val showSurvey = !RemoteConfigManager.getSurveyScreenConfig().skipSurveyScreen && !isOnboardingDone

        var nextStep: Step? = currentStep
        while (true) {
            nextStep = nextStep?.let { getNextStepEnum(it) } ?: return
            when (nextStep) {
                Step.LANGUAGE -> {
                    if (showLanguage) {
                        if (RemoteConfigManager.getLanguageScreenConfig().showLanguageNative1) {
                            adMobManager.nativeAdLoader.loadAd(AdIds.getNativeLanguageAdId())
                        }
                        return
                    }
                }
                Step.ONBOARDING -> {
                    if (showOnboarding) {
                        if (RemoteConfigManager.getOnboardingScreenConfig().showOb1Native) {
                            adMobManager.nativeAdLoader.loadAd(AdIds.getNativeOnboardingAdId())
                        }
                        return
                    }
                }
                Step.SURVEY -> {
                    if (showSurvey) {
                        // In SurveyActivity, the Ad ID used is getSurveyNative1AdId() but previously getNativeOnboardingAdId()
                        if (RemoteConfigManager.getSurveyScreenConfig().showSurveyNative1) {
                            adMobManager.nativeAdLoader.loadAd(AdIds.getSurveyNative1AdId())
                        }
                        return
                    }
                }
                Step.PREMIUM -> return // No native ad to preload for premium typically, or handled by interstitial
                else -> return
            }
        }
    }

    /**
     * Shows an interstitial ad based on the remote config of the *current* screen,
     * and then navigates to the next screen in the startup sequence.
     */
    fun navigateNextWithAd(
        activity: AppCompatActivity,
        currentStep: Step,
        appPreferences: AppPreferences,
        adMobManager: AdMobManager,
        analyticsManager: AnalyticsManager
    ) {
        val nextIntent = getNextIntent(activity, currentStep, appPreferences)
        
        if (AdMobManager.isPremium) {
            activity.startActivity(nextIntent)
            activity.finish()
            return
        }

        var showAd = false
        var adUnitId = ""
        var eventPrefix = ""

        when (currentStep) {
            Step.LANGUAGE -> {
                val config = RemoteConfigManager.getLanguageScreenConfig()
                if (config.showLanguageInterstitial) {
                    showAd = true
                    adUnitId = AdIds.getInterstitialLanguageID()
                    eventPrefix = "lng_int"
                }
            }
            Step.ONBOARDING -> {
                val config = RemoteConfigManager.getOnboardingScreenConfig()
                if (config.showOnboardingInterstitial) {
                    showAd = true
                    adUnitId = AdIds.getInterstitialOnboardingID()
                    eventPrefix = "getstarted_int"
                }
            }
            Step.SURVEY -> {
                val config = RemoteConfigManager.getSurveyScreenConfig()
                if (config.showSurveyInterstitial) {
                    showAd = true
                    adUnitId = AdIds.getInterstitialSurveyID()
                    eventPrefix = "survey_int"
                }
            }
            else -> {}
        }

        if (showAd && adUnitId.isNotEmpty()) {
            AdUtils.loadAndShowAdWithTimer(
                activity = activity,
                adMobManager = adMobManager,
                adUnit = adUnitId,
                timeOut = 0L,
                analyticsManager = analyticsManager,
                eventNamePrefix = eventPrefix,
                ignoreFrequency = true
            ) {
                activity.startActivity(nextIntent)
                activity.finish()
            }
        } else {
            activity.startActivity(nextIntent)
            activity.finish()
        }
    }
}
