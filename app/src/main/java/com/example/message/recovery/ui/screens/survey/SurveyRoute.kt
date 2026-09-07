package com.example.message.recovery.ui.screens.survey

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.ui.compose.NativeOrBannerAdSlot
import com.example.message.recovery.ui.navigation.AppNavigator
import com.example.message.recovery.utils.StartupNavigationManager
import com.umer_tf.ads.domain.ads.native_ad.NativeAdLayout
import com.umer_tf.ads.domain.core.AdMobManager

@Composable
fun SurveyRoute(
    fromHome: Boolean,
    navigator: AppNavigator,
    adMobManager: AdMobManager,
    analyticsManager: AnalyticsManager,
    appPreferences: AppPreferences,
    viewModel: SurveyViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current as AppCompatActivity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(fromHome) {
        viewModel.setFromHome(fromHome)
    }

    LaunchedEffect(Unit) {
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_survey")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.SURVEY_SCR_VIEW)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SurveyContract.Effect.ContinueStartup -> {
                    StartupNavigationManager.navigateNextWithAd(
                        activity = activity,
                        navigator = navigator,
                        lifecycle = activity.lifecycle,
                        lifecycleScope = activity.lifecycleScope,
                        currentStep = StartupNavigationManager.Step.SURVEY,
                        appPreferences = appPreferences,
                        adMobManager = adMobManager,
                        analyticsManager = analyticsManager,
                    )
                }
                SurveyContract.Effect.Close -> navigator.pop()
                SurveyContract.Effect.FinishAffinity -> activity.finishAffinity()
            }
        }
    }

    BackHandler { viewModel.onEvent(SurveyContract.Event.Back) }

    val adRules = RemoteConfigManager.getAdRules()
    val showNative = adRules.showSurveyNative1 && !AdMobManager.isPremium
    val adSlot: (@Composable (Modifier) -> Unit)? = if (showNative) {
        remember(adRules) {
            movableContentOf { slotModifier: Modifier ->
                NativeOrBannerAdSlot(
                    enabled = true,
                    nativeAdUnitId = AdIds.getSurveyNative1AdId(),
                    bannerAdUnitId = AdIds.getBannerSurveyAdId(),
                    nativeSlotKey = "survey_native",
                    modifier = slotModifier,
                    layout = NativeAdLayout.fromOrDefault(
                        adRules.surveyNativeLayout,
                        NativeAdLayout.LARGE_DEFAULT,
                    ),
                    nativeConfig = adRules.nativeConfig,
                    showMedia = true,
                    analyticsManager = analyticsManager,
                    eventPrefix = "survey_scr_native",
                )
            }
        }
    } else {
        null
    }

    SurveyScreen(
        state = uiState,
        adSlot = adSlot,
        onEvent = viewModel::onEvent,
        onAnalyticsToggle = {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.FEATURE_SELECTED)
        },
        onAnalyticsNext = {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.SURVEY_SCR_DONE)
        },
    )
}
