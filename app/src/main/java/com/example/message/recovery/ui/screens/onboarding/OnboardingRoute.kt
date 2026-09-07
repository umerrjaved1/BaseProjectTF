package com.example.message.recovery.ui.screens.onboarding

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import com.example.message.recovery.R
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.app.MyApp
import com.example.message.recovery.model.OnboardingItem
import com.example.message.recovery.notification.NotificationListenerHelper
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.ui.compose.NativeOrBannerAdSlot
import com.example.message.recovery.ui.navigation.AppNavigator
import com.example.message.recovery.utils.AdUtils
import com.example.message.recovery.utils.PermissionNavigationHelper
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
    // Set when we send the user to system Settings, cleared only once they have actually come
    // back. hasLeftScreen guards against the resume that fires before we leave — see the
    // LifecycleResumeEffect below.
    var awaitingSettingsReturn by remember { mutableStateOf(false) }
    var hasLeftScreen by remember { mutableStateOf(false) }

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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { continueStartup() }

    fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            MyApp.ignoreNextResume = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            continueStartup()
        }
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
        // Only ask for what is actually missing. A returning user who already granted both (for
        // example after reinstalling onboarding state, or coming back through Settings) should not
        // be shown a permission wall they have nothing to do on.
        val needsListener = !NotificationListenerHelper.isEnabled(activity)
        val needsNotifications = !PermissionNavigationHelper.hasPostNotifications(activity)
        if (needsListener || needsNotifications) {
            built.add(
                OnboardingPage.Permissions(
                    needsListener = needsListener,
                    needsNotifications = needsNotifications,
                ),
            )
        }
        pages = built
    }

    // Keyed on Unit, not on the pending flag. Keying it on the flag meant that setting the flag
    // re-keyed the effect, which then fired straight away while the screen was still RESUMED —
    // i.e. before startActivity() had actually taken the user to Settings. The flag was consumed
    // on the way out instead of on the way back, so returning from Settings did nothing and the
    // Allow Access button appeared dead. Requiring an intervening pause makes the handoff explicit.
    LifecycleResumeEffect(Unit) {
        if (awaitingSettingsReturn && hasLeftScreen) {
            awaitingSettingsReturn = false
            hasLeftScreen = false
            requestPostNotificationsIfNeeded()
        }
        onPauseOrDispose { hasLeftScreen = true }
    }

    fun updateSmallNativeVisibility(position: Int) {
        val adRules = RemoteConfigManager.getAdRules()
        val page = pages.getOrNull(position)
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "ob${position + 1}_view")
        if (page is OnboardingPage.FullNativeAd || page is OnboardingPage.Permissions || AdMobManager.isPremium) {
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
                nativeAdUnitId = AdIds.getNativeOb1AdId(),
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
                is OnboardingPage.Permissions -> {
                    analyticsManager.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE,
                        AnalyticsManager.Events.OB4_GET_STARTED,
                    )
                    if (NotificationListenerHelper.isEnabled(activity)) {
                        // Listener already granted (or granted on a previous trip to Settings) —
                        // nothing to send them out for, so go straight to the runtime permission.
                        requestPostNotificationsIfNeeded()
                    } else {
                        awaitingSettingsReturn = true
                        hasLeftScreen = false
                        PermissionNavigationHelper.openNotificationListenerSettings(activity)
                    }
                }
                null -> continueStartup()
            }
        },
        onSkip = {
            val permissionIndex = pages.indexOfFirst { it is OnboardingPage.Permissions }
            if (permissionIndex >= 0 && currentPage < permissionIndex) {
                currentPage = permissionIndex
            } else {
                continueStartup()
            }
        },
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
