package com.example.message.recovery.ui.screens.onboarding

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.example.message.recovery.R
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.app.MyApp
import com.example.message.recovery.model.OnboardingItem
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.ui.compose.NativeOrBannerAdSlot
import com.example.message.recovery.ui.navigation.AppNavigator
import com.example.message.recovery.utils.AdUtils
import com.example.message.recovery.utils.StartupNavigationManager
import com.umer_tf.ads.domain.ads.native_ad.NativeAdLayout
import com.umer_tf.ads.domain.core.AdMobManager

@Composable
fun OnboardingRoute(
    navigator: AppNavigator,
    adMobManager: AdMobManager,
    analyticsManager: AnalyticsManager,
    appPreferences: AppPreferences,
) {
    val activity = LocalContext.current as AppCompatActivity
    DisposableEffect(Unit) {
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity.requestedOrientation = previous }
    }
    val title1 = stringResource(R.string.onboarding_title_1)
    val desc1 = stringResource(R.string.onboarding_desc_1)
    val title2 = stringResource(R.string.onboarding_title_2)
    val desc2 = stringResource(R.string.onboarding_desc_2)
    val title3 = stringResource(R.string.onboarding_title_3)
    val desc3 = stringResource(R.string.onboarding_desc_3)

    var pages by remember { mutableStateOf<List<OnboardingPage>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(0) }
    var showSmallNative by remember { mutableStateOf(false) }
    var hasNavigated by remember { mutableStateOf(false) }
    var fullNativeFrame by remember { mutableStateOf<android.widget.FrameLayout?>(null) }

    fun continueStartup() {
        if (hasNavigated) return
        hasNavigated = true
        StartupNavigationManager.navigateNextWithAd(
            activity = activity,
            navigator = navigator,
            lifecycle = activity.lifecycle,
            lifecycleScope = activity.lifecycleScope,
            currentStep = StartupNavigationManager.Step.ONBOARDING,
            appPreferences = appPreferences,
            adMobManager = adMobManager,
            analyticsManager = analyticsManager,
        )
    }

    LaunchedEffect(Unit) {
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_onboarding")
        val config = RemoteConfigManager.getAdRules()
        val items = mutableListOf<OnboardingItem>()
        if (config.showObSlide1) {
            items.add(OnboardingItem(title1, desc1, R.drawable.ob_msg_recovery_1))
        }
        if (config.showObSlide2) {
            items.add(OnboardingItem(title2, desc2, R.drawable.ob_msg_recovery_2))
        }
        if (config.showObSlide3) {
            items.add(OnboardingItem(title3, desc3, R.drawable.ob_msg_recovery_3))
        }
        if (items.isEmpty()) {
            continueStartup()
            return@LaunchedEffect
        }
        val hasAdPage = config.showFullNativeOnBoarding && !AdMobManager.isPremium
        val built = mutableListOf<OnboardingPage>()
        items.forEachIndexed { index, item ->
            if (hasAdPage && index == config.fullNativeAdPosition) {
                built.add(OnboardingPage.FullNativeAd)
            }
            built.add(OnboardingPage.Content(item, index))
        }
        if (hasAdPage && config.fullNativeAdPosition >= built.size) {
            built.add(OnboardingPage.FullNativeAd)
        }
        pages = built
    }

    fun updateSmallNativeVisibility(position: Int) {
        val adRules = RemoteConfigManager.getAdRules()
        val page = pages.getOrNull(position)
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "ob${position + 1}_view")
        if (page is OnboardingPage.FullNativeAd || AdMobManager.isPremium) {
            showSmallNative = false
            return
        }
        val itemIndex = (page as? OnboardingPage.Content)?.itemIndex ?: -1
        showSmallNative = when (itemIndex) {
            0 -> adRules.showOb1Native
            1 -> adRules.showOb2Native
            2 -> adRules.showOb3Native
            else -> false
        }
    }

    BackHandler {
        if (pages.isEmpty() || currentPage == 0) continueStartup() else currentPage -= 1
    }

    if (pages.isEmpty()) return

    val adRules = RemoteConfigManager.getAdRules()
    val contentIndex = (pages.getOrNull(currentPage) as? OnboardingPage.Content)?.itemIndex

    // ONE inline native for the whole onboarding flow, matching Language and Survey.
    //
    // This used to build a slot per slide — key "onboarding_native_$contentIndex" and a different
    // unit id per index — so a user paging through three slides requested three separate natives
    // and only ever saw one at a time. A single stable key means AdViewModel serves the ad it
    // already holds when the user pages on or back, so the flow costs one request.
    //
    // Hoisted out of the visibility check on purpose: a slide with showObNNative=false must not
    // discard this movableContentOf, or the ad view is re-inflated when the next slide shows it
    // again. The per-slide flags still decide visibility (see updateSmallNativeVisibility) —
    // they just no longer each buy their own ad.
    val onboardingAdSlot = remember(adRules) {
        movableContentOf { slotModifier: Modifier ->
            NativeOrBannerAdSlot(
                enabled = true,
                nativeAdUnitId = AdIds.getObNativeAdId(),
                bannerAdUnitId = AdIds.getBannerOnboardingAdId(),
                nativeSlotKey = ONBOARDING_NATIVE_SLOT_KEY,
                modifier = slotModifier,
                layout = NativeAdLayout.fromOrDefault(
                    adRules.onboardingNativeLayout,
                    NativeAdLayout.SMALL_1A,
                ),
                nativeConfig = adRules.nativeConfig,
                showMedia = adRules.showOnboardingNativeMedia,
                analyticsManager = analyticsManager,
                // One ad now means one funnel. Reporting it as ob1/ob2/ob3_native would count a
                // single request three times and log a "request" on slides that only re-render
                // the cached ad.
                eventPrefix = "ob_native",
            )
        }
    }

    val adSlot: (@Composable (Modifier) -> Unit)? =
        if (showSmallNative && contentIndex != null) onboardingAdSlot else null

    OnboardingScreen(
        pages = pages,
        currentPage = currentPage,
        adSlot = adSlot,
        onPageChanged = { pos ->
            currentPage = pos
            updateSmallNativeVisibility(pos)
        },
        onContinue = {
            when (val page = pages.getOrNull(currentPage)) {
                is OnboardingPage.Content -> {
                    if (currentPage < pages.lastIndex) currentPage += 1 else continueStartup()
                }
                OnboardingPage.FullNativeAd -> {
                    if (currentPage < pages.lastIndex) currentPage += 1
                }
                null -> continueStartup()
            }
        },
        // Nothing left to skip ahead to now that the permission page is gone -- Skip leaves
        // onboarding, and MainActivity does the permission asking on Home.
        onSkip = { continueStartup() },
        onFullNativeReady = { frame, shimmer ->
            if (fullNativeFrame === frame) return@OnboardingScreen
            fullNativeFrame = frame
            AdUtils.loadAndShowNativeAd(
                adMobManager = adMobManager,
                activity = activity,
                adUnitId = AdIds.getFullNativeOnboardingAdId(),
                layoutResId = R.layout.full_native_ad_design,
                frameLayout = frame,
                shimmerFrameLayout = shimmer,
                showMedia = true,
                forceLoadNew = false,
                analyticsManager = analyticsManager,
                eventNamePrefix = "ob_full_native",
                nativeConfig = RemoteConfigManager.getAdRules().nativeConfig,
            )
        },
    )
}


/**
 * Cache key for the one inline native shared by every onboarding slide. Stable by design: the key
 * is what AdViewModel caches the loaded ad under, so a single value is what keeps paging between
 * slides from requesting a new ad.
 */
private const val ONBOARDING_NATIVE_SLOT_KEY = "onboarding_native"
