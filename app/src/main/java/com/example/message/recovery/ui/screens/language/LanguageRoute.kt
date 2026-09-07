package com.example.message.recovery.ui.screens.language

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.example.message.recovery.R
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.model.LanguageModel
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.ui.compose.NativeOrBannerAdSlot
import com.example.message.recovery.ui.navigation.AppNavigator
import com.example.message.recovery.ui.viewmodel.LanguageNav
import com.example.message.recovery.ui.viewmodel.LanguageViewModel
import com.example.message.recovery.utils.StartupNavigationManager
import com.umer_tf.ads.domain.ads.native_ad.NativeAdLayout
import com.umer_tf.ads.domain.core.AdMobManager

@Composable
fun LanguageRoute(
    fromStart: Boolean,
    navigator: AppNavigator,
    adMobManager: AdMobManager,
    analyticsManager: AnalyticsManager,
    appPreferences: AppPreferences,
    viewModel: LanguageViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current as AppCompatActivity
    val pleaseSelect = stringResource(R.string.please_select_a_language)

    var languages by remember { mutableStateOf<List<LanguageModel>>(emptyList()) }
    var selected by remember { mutableStateOf<LanguageModel?>(null) }
    var doneLabel by remember { mutableStateOf("Done") }
    var showExitDialog by remember { mutableStateOf(false) }

    val doneEn = stringResource(R.string.done_in_english)
    val doneAr = stringResource(R.string.done_in_arabic)
    val doneEs = stringResource(R.string.done_in_spanish)
    val doneIn = stringResource(R.string.done_in_indonesian)
    val doneFr = stringResource(R.string.done_in_french)
    val doneFa = stringResource(R.string.done_in_persian)
    val doneHi = stringResource(R.string.done_in_hindi)
    val doneRu = stringResource(R.string.done_in_russian)
    val donePt = stringResource(R.string.done_in_portuguese)
    val doneBn = stringResource(R.string.done_in_bengali)
    val doneTr = stringResource(R.string.done_in_turkish)

    fun doneFor(id: Int) = when (id) {
        1 -> doneAr
        2 -> doneEn
        3 -> doneEs
        4 -> doneIn
        5 -> doneFr
        6 -> doneFa
        7 -> doneHi
        8 -> doneRu
        9 -> donePt
        10 -> doneBn
        11 -> doneTr
        else -> doneEn
    }

    LaunchedEffect(Unit) {
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "language_activity")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.LNG_SCR_VIEW)
        val adRules = RemoteConfigManager.getAdRules()
        val languageList = languageList(activity)
        val savedLanguageId = appPreferences.getInt(AppPreferences.LANGUAGE_ID)
        val savedLanguage = languageList.find { it.id == savedLanguageId }
        val activeLanguage = savedLanguage ?: languageList.find { it.code == "en" } ?: languageList.first()
        languages = languageList
        viewModel.setSelectedLanguage(activeLanguage)
        selected = activeLanguage
        doneLabel = doneFor(activeLanguage.id)
    }

    LaunchedEffect(Unit) {
        viewModel.navigate.collect { nav ->
            when (nav) {
                LanguageNav.ONBOARDING -> {
                    StartupNavigationManager.navigateNextWithAd(
                        activity = activity,
                        navigator = navigator,
                        lifecycle = activity.lifecycle,
                        lifecycleScope = activity.lifecycleScope,
                        currentStep = StartupNavigationManager.Step.LANGUAGE,
                        appPreferences = appPreferences,
                        adMobManager = adMobManager,
                        analyticsManager = analyticsManager,
                    )
                }
                LanguageNav.MAIN -> {
                    if (fromStart) {
                        StartupNavigationManager.navigateNextWithAd(
                            activity = activity,
                            navigator = navigator,
                            lifecycle = activity.lifecycle,
                            lifecycleScope = activity.lifecycleScope,
                            currentStep = StartupNavigationManager.Step.LANGUAGE,
                            appPreferences = appPreferences,
                            adMobManager = adMobManager,
                            analyticsManager = analyticsManager,
                        )
                    } else {
                        navigator.pop()
                    }
                }
            }
        }
    }

    BackHandler {
        if (fromStart) showExitDialog = true else navigator.pop()
    }

    val adRules = RemoteConfigManager.getAdRules()
    val showLanguageNative = adRules.showLanguageNative1 && !AdMobManager.isPremium
    val adSlot: (@Composable (Modifier) -> Unit)? = if (showLanguageNative) {
        remember(adRules) {
            movableContentOf { slotModifier: Modifier ->
                NativeOrBannerAdSlot(
                    enabled = true,
                    nativeAdUnitId = AdIds.getNativeLanguageAdId(),
                    bannerAdUnitId = AdIds.getBannerLanguageAdId(),
                    nativeSlotKey = "language_native",
                    modifier = slotModifier,
                    layout = NativeAdLayout.fromOrDefault(
                        adRules.languageNativeLayout,
                        NativeAdLayout.LARGE_DEFAULT,
                    ),
                    nativeConfig = adRules.nativeConfig,
                    showMedia = true,
                    analyticsManager = analyticsManager,
                    eventPrefix = "lng_scr_native",
                )
            }
        }
    } else {
        null
    }

    LanguageScreen(
        languages = languages,
        selected = selected,
        doneLabel = doneLabel,
        showExitDialog = showExitDialog,
        adSlot = adSlot,
        onLanguageClick = { selectedLang ->
            selected = selectedLang
            viewModel.setSelectedLanguage(selectedLang)
            doneLabel = doneFor(selectedLang.id)
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.LNG_SELECTED)
        },
        onDone = {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, "btn_select_language_done")
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.LNG_SCR_NEXT)
            if (selected == null) {
                Toast.makeText(activity, pleaseSelect, Toast.LENGTH_SHORT).show()
                return@LanguageScreen
            }
            viewModel.setSelectedLanguage(selected!!)
            viewModel.onDoneClicked()
        },
        onExitConfirm = { activity.finishAffinity() },
        onExitDismiss = { showExitDialog = false },
    )
}

private fun languageList(activity: Activity): List<LanguageModel> = listOf(
    LanguageModel(2, R.drawable.flag_english, activity.getString(R.string.english), "en"),
    LanguageModel(1, R.drawable.flag_arabic, activity.getString(R.string.arabic), "ar"),
    LanguageModel(3, R.drawable.flag_spanish, activity.getString(R.string.spanish), "es"),
    LanguageModel(4, R.drawable.flag_indonesia, activity.getString(R.string.indonesian), "in"),
    LanguageModel(6, R.drawable.flag_persian, activity.getString(R.string.persian), "fa"),
    LanguageModel(7, R.drawable.flag_hindi, activity.getString(R.string.hindi), "hi"),
    LanguageModel(8, R.drawable.flag_russia, activity.getString(R.string.russian), "ru"),
    LanguageModel(9, R.drawable.flag_portuguese, activity.getString(R.string.portuguese), "pt"),
    LanguageModel(10, R.drawable.flag_bangla, activity.getString(R.string.bengali), "bn"),
    LanguageModel(11, R.drawable.flag_turkey, activity.getString(R.string.turkish), "tr"),
)
