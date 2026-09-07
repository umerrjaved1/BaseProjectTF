package com.example.message.recovery.utils

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.ui.navigation.AppNavigator
import com.example.message.recovery.ui.navigation.AppRoute
import com.example.message.recovery.ui.navigation.Home
import com.example.message.recovery.ui.navigation.Language
import com.example.message.recovery.ui.navigation.Onboarding
import com.example.message.recovery.ui.navigation.Premium
import com.example.message.recovery.ui.navigation.Survey
import com.umer_tf.ads.domain.core.AdMobManager

object StartupNavigationManager {

    private const val TAG = "StartupNav"

    enum class Step {
        START, PREMIUM_AFTER_SPLASH, PREMIUM, LANGUAGE, ONBOARDING, SURVEY, PREMIUM_AFTER_SURVEY
    }

    fun getNextRoute(currentStep: Step, appPreferences: AppPreferences): AppRoute {
        val isLanguageSelected = appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED)
        val isOnboardingDone = appPreferences.getBoolean(AppPreferences.IS_ONBOARDING)
        val isPremium = AdMobManager.isPremium

        val isFirstFlow = !isOnboardingDone

        val showLanguage = isFirstFlow && !RemoteConfigManager.getAdRules().skipLanguageScreen && !isLanguageSelected
        val showOnboarding = isFirstFlow && !RemoteConfigManager.getAdRules().skipOnboardingScreen
        val showSurvey = isFirstFlow && !RemoteConfigManager.getAdRules().skipSurveyScreen

        val premiumConfig = RemoteConfigManager.getAdRules()
        val canShowPremium = !isPremium && !premiumConfig.skipPremiumScreen

        val showPremiumAfterSplash = if (isFirstFlow) {
            premiumConfig.showPremiumAfterSplash
        } else {
            premiumConfig.showPremiumAfterSplash || premiumConfig.showPremiumAfterSurvey
        } && canShowPremium

        val showPremiumAfterSurvey = isFirstFlow && premiumConfig.showPremiumAfterSurvey &&
            !premiumConfig.showPremiumAfterSplash && canShowPremium

        Log.d(
            TAG,
            """
            ================ StartupNavigation Diagnostics ================
            currentStep = $currentStep
            isFirstFlow = $isFirstFlow (isOnboardingDone = $isOnboardingDone)
            isLanguageSelected = $isLanguageSelected, isPremium = $isPremium
            RemoteConfig.premium:
                skipPremiumScreen = ${premiumConfig.skipPremiumScreen}
                showPremiumAfterSplash = ${premiumConfig.showPremiumAfterSplash}
                showPremiumAfterSurvey = ${premiumConfig.showPremiumAfterSurvey}
            Evaluated Decision Flags:
                canShowPremium = $canShowPremium
                showPremiumAfterSplash = $showPremiumAfterSplash
                showPremiumAfterSurvey = $showPremiumAfterSurvey
                showLanguage = $showLanguage
                showOnboarding = $showOnboarding
                showSurvey = $showSurvey
            ================================================================
            """.trimIndent()
        )

        var nextStep: Step? = currentStep
        while (true) {
            nextStep = nextStep?.let { getNextStepEnum(it) }
            Log.d(TAG, "Evaluating step: $nextStep")
            when (nextStep) {
                Step.PREMIUM_AFTER_SPLASH, Step.PREMIUM -> {
                    if (showPremiumAfterSplash) {
                        Log.i(TAG, "-> Resolved Next Screen: Premium (FROM_SPLASH)")
                        return Premium(fromSplash = true)
                    } else {
                        Log.d(TAG, "Skipping Premium after splash (showPremiumAfterSplash = false)")
                    }
                }
                Step.LANGUAGE -> {
                    if (showLanguage) {
                        Log.i(TAG, "-> Resolved Next Screen: Language")
                        return Language(fromStart = true)
                    } else {
                        Log.d(TAG, "Skipping Language (showLanguage = false)")
                    }
                }
                Step.ONBOARDING -> {
                    if (showOnboarding) {
                        Log.i(TAG, "-> Resolved Next Screen: Onboarding")
                        return Onboarding
                    } else {
                        Log.d(TAG, "Skipping Onboarding (showOnboarding = false)")
                    }
                }
                Step.SURVEY -> {
                    if (showSurvey) {
                        Log.i(TAG, "-> Resolved Next Screen: Survey")
                        return Survey()
                    } else {
                        Log.d(TAG, "Skipping Survey (showSurvey = false)")
                    }
                }
                Step.PREMIUM_AFTER_SURVEY -> {
                    if (showPremiumAfterSurvey) {
                        Log.i(TAG, "-> Resolved Next Screen: Premium (FROM_SURVEY)")
                        return Premium(fromSplash = true, fromSurvey = true)
                    } else {
                        Log.d(TAG, "Skipping Premium after survey (showPremiumAfterSurvey = false)")
                    }
                }
                else -> {
                    Log.i(TAG, "-> Resolved Next Screen: Home")
                    return Home
                }
            }
        }
    }

    private fun getNextStepEnum(current: Step): Step? {
        return when (current) {
            Step.START -> Step.PREMIUM_AFTER_SPLASH
            Step.PREMIUM_AFTER_SPLASH, Step.PREMIUM -> Step.LANGUAGE
            Step.LANGUAGE -> Step.ONBOARDING
            Step.ONBOARDING -> Step.SURVEY
            Step.SURVEY -> Step.PREMIUM_AFTER_SURVEY
            Step.PREMIUM_AFTER_SURVEY -> null
        }
    }

    fun preloadNextScreenAd(
        activity: Activity,
        currentStep: Step,
        appPreferences: AppPreferences,
        adMobManager: AdMobManager,
    ) {
        if (AdMobManager.isPremium) return

        val isLanguageSelected = appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED)
        val isOnboardingDone = appPreferences.getBoolean(AppPreferences.IS_ONBOARDING)

        val isFirstFlow = !isOnboardingDone

        val showLanguage = isFirstFlow && !RemoteConfigManager.getAdRules().skipLanguageScreen && !isLanguageSelected
        val showOnboarding = isFirstFlow && !RemoteConfigManager.getAdRules().skipOnboardingScreen
        val showSurvey = isFirstFlow && !RemoteConfigManager.getAdRules().skipSurveyScreen

        var nextStep: Step? = currentStep
        while (true) {
            nextStep = nextStep?.let { getNextStepEnum(it) } ?: return
            when (nextStep) {
                Step.LANGUAGE -> {
                    if (showLanguage) {
                        if (RemoteConfigManager.getAdRules().showLanguageNative1) {
                            Log.d(TAG, "Preloading Language Native Ad")
                            adMobManager.nativeAdLoader.loadAd(AdIds.getNativeLanguageAdId(), activity)
                        }
                        return
                    }
                }
                Step.ONBOARDING -> {
                    if (showOnboarding) {
                        if (RemoteConfigManager.getAdRules().showOb1Native) {
                            Log.d(TAG, "Preloading Onboarding Native Ad")
                            adMobManager.nativeAdLoader.loadAd(AdIds.getNativeOb1AdId(), activity)
                        }
                        return
                    }
                }
                Step.SURVEY -> {
                    if (showSurvey) {
                        if (RemoteConfigManager.getAdRules().showSurveyNative1) {
                            Log.d(TAG, "Preloading Survey Native Ad")
                            adMobManager.nativeAdLoader.loadAd(AdIds.getSurveyNative1AdId(), activity)
                        }
                        return
                    }
                }
                Step.PREMIUM_AFTER_SPLASH, Step.PREMIUM, Step.PREMIUM_AFTER_SURVEY -> return
                else -> return
            }
        }
    }

    fun navigateNextWithAd(
        activity: AppCompatActivity,
        navigator: AppNavigator,
        currentStep: Step,
        lifecycle: Lifecycle,
        lifecycleScope: LifecycleCoroutineScope,
        appPreferences: AppPreferences,
        adMobManager: AdMobManager,
        analyticsManager: AnalyticsManager,
    ) {
        val nextRoute = getNextRoute(currentStep, appPreferences)

        if (AdMobManager.isPremium) {
            Log.d(TAG, "User is Premium, skipping startup ad from step: $currentStep")
            navigator.replaceAll(nextRoute)
            return
        }

        var showAd = false
        var adUnitId = ""
        var eventPrefix = ""

        when (currentStep) {
            Step.LANGUAGE -> {
                if (RemoteConfigManager.getAdRules().showLanguageInterstitial) {
                    showAd = true
                    adUnitId = AdIds.getInterstitialLanguageID()
                    eventPrefix = "lng_int"
                }
            }
            Step.ONBOARDING -> {
                if (RemoteConfigManager.getAdRules().showOnboardingInterstitial) {
                    showAd = true
                    adUnitId = AdIds.getInterstitialOnboardingID()
                    eventPrefix = "getstarted_int"
                }
            }
            Step.SURVEY -> {
                if (RemoteConfigManager.getAdRules().showSurveyInterstitial) {
                    showAd = true
                    adUnitId = AdIds.getInterstitialSurveyID()
                    eventPrefix = "survey_int"
                }
            }
            else -> {}
        }

        Log.d(TAG, "navigateNextWithAd from $currentStep: showAd = $showAd, adUnitId = $adUnitId")

        if (showAd && adUnitId.isNotEmpty()) {
            AdUtils.loadAndShowAdWithTimer(
                activity = activity,
                lifecycle = lifecycle,
                lifecycleScope = lifecycleScope,
                adMobManager = adMobManager,
                adUnit = adUnitId,
                timeOut = 0L,
                analyticsManager = analyticsManager,
                eventNamePrefix = eventPrefix,
                ignoreFrequency = true,
            ) {
                Log.i(TAG, "startup ad finished, navigating to $nextRoute")
                navigator.replaceAll(nextRoute)
            }
        } else {
            navigator.replaceAll(nextRoute)
        }
    }
}
