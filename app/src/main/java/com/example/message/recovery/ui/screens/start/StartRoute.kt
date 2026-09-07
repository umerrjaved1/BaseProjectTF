package com.example.message.recovery.ui.screens.start

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import com.applovin.sdk.AppLovinPrivacySettings
import com.example.message.recovery.R
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.app.MyApp
import com.example.message.recovery.fcm.FcmManager
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.remoteconfig.data.SplashAdLoadOrder
import com.example.message.recovery.ui.navigation.AppNavigator
import com.example.message.recovery.ui.theme.DevicePreviews
import com.example.message.recovery.ui.theme.SplashGradientEnd
import com.example.message.recovery.ui.theme.SplashGradientStart
import com.example.message.recovery.ui.theme.isShortScreen
import com.example.message.recovery.ui.theme.windowSize
import com.example.message.recovery.ui.viewmodel.StartViewModel
import com.example.message.recovery.utils.AdUtils
import com.example.message.recovery.utils.StartupNavigationManager
import com.example.message.recovery.utils.UIState
import com.umer_tf.ads.domain.consent.AdsConsentManager
import com.umer_tf.ads.domain.core.AdMobManager
import com.vungle.ads.VunglePrivacySettings
import dagger.Lazy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "SplashNav"
private const val SPLASH_MAX_MS = 8000L

@Composable
fun StartRoute(
    navigator: AppNavigator,
    adMobManagerLazy: Lazy<AdMobManager>,
    analyticsManager: Lazy<AnalyticsManager>,
    appPreferences: AppPreferences,
    viewModel: StartViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current as AppCompatActivity
    val adMobManager = remember { adMobManagerLazy.get() }
    val adsConsentManager = remember { AdsConsentManager(activity) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var loadingProgress by remember { mutableFloatStateOf(0.15f) }
    var showGetStarted by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(true) }
    var isPremium by remember { mutableStateOf(false) }
    var hasMovedToNext by remember { mutableStateOf(false) }
    var pendingNavigationAfterAd by remember { mutableStateOf(false) }
    var hasTriggeredInterstitialNavigation by remember { mutableStateOf(false) }
    var hasHandledState by remember { mutableStateOf(false) }
    var isAdLoadingFinished by remember { mutableStateOf(false) }
    var hasStartedSplashWork by remember { mutableStateOf(false) }
    var splashJob by remember { mutableStateOf<Job?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun setSplashProgress(value: Float) {
        if (value > loadingProgress) {
            loadingProgress = value
            showProgress = !isAdLoadingFinished
        }
    }

    fun moveToNextScreen(reason: String) {
        Log.i(TAG, "moveToNextScreen reason=$reason hasMoved=$hasMovedToNext")
        if (hasMovedToNext || activity.isFinishing || activity.isDestroyed) return
        mainHandler.post {
            if (hasMovedToNext || activity.isFinishing || activity.isDestroyed) return@post
            hasMovedToNext = true
            pendingNavigationAfterAd = false
            splashJob?.cancel()
            splashJob = null
            val next = StartupNavigationManager.getNextRoute(
                StartupNavigationManager.Step.START,
                appPreferences,
            )
            Log.i(TAG, "navigating next $next")
            navigator.replaceAll(next)
        }
    }

    fun navigateAfterAd(reason: String) {
        pendingNavigationAfterAd = true
        Log.i(TAG, "navigateAfterAd reason=$reason")
        mainHandler.post { moveToNextScreen(reason) }
    }

    fun triggerNextNavigationStep() {
        val adRules = RemoteConfigManager.getAdRules()
        if (isPremium) {
            moveToNextScreen("premium_next")
            return
        }
        if (hasTriggeredInterstitialNavigation) return
        hasTriggeredInterstitialNavigation = true
        pendingNavigationAfterAd = true

        val tryAppOpen = adRules.showAppOpenSplashAd
        val tryInter = adRules.showSplashInterstitialAd
        val interFirst = adRules.splashAdLoadOrder == SplashAdLoadOrder.INTER_THEN_APP_OPEN

        fun showFullScreenNativeOrLeave(reason: String) {
            if (adRules.showFullScreenNativeSplashAd && !tryAppOpen && !tryInter) {
                AdUtils.loadAndShowFullScreenNativeAdWithDialog(
                    activity = activity,
                    adUnitId = AdIds.getNativeAdId(),
                    nativeConfig = RemoteConfigManager.getAdRules().nativeConfig,
                ) { navigateAfterAd("fs_native_complete") }
            } else if (!tryAppOpen && !tryInter) {
                moveToNextScreen("no_splash_ad")
            } else {
                navigateAfterAd(reason)
            }
        }

        fun loadInter(onFail: () -> Unit) {
            if (!tryInter) {
                onFail()
                return
            }
            AdUtils.loadAndShowInterSplash(
                adMobManager = adMobManager,
                lifecycle = activity.lifecycle,
                activity = activity,
                adUnit = AdIds.getInterstitialSplashAdId(),
                lifecycleScope = activity.lifecycleScope,
                analyticsManager = analyticsManager.get(),
                eventNamePrefix = "splash_int",
            ) { shown ->
                if (shown) navigateAfterAd("interstitial_complete") else onFail()
            }
        }

        fun loadAppOpen(onFail: () -> Unit) {
            if (!tryAppOpen) {
                onFail()
                return
            }
            val analytics = analyticsManager.get()
            analytics.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APPOPEN_REQUEST)
            adMobManager.appOpenAdLoader.loadAppOpenAd(activity) { isLoaded ->
                if (isLoaded) {
                    analytics.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APPOPEN_REQUEST_PASS)
                    MyApp.ignoreNextResume = true
                    adMobManager.appOpenAdLoader.showAppOpenAdIfAvailable {
                        analytics.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APPOPEN_VIEW)
                        navigateAfterAd("app_open_complete")
                    }
                } else {
                    analytics.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APPOPEN_REQUEST_FAIL)
                    onFail()
                }
            }
        }

        when {
            !tryAppOpen && !tryInter -> showFullScreenNativeOrLeave("no_splash_ad")
            interFirst -> loadInter { loadAppOpen { showFullScreenNativeOrLeave("waterfall_fail") } }
            else -> loadAppOpen { loadInter { showFullScreenNativeOrLeave("waterfall_fail") } }
        }
    }

    fun showGetStartedButton() {
        isAdLoadingFinished = true
        setSplashProgress(1f)
        showProgress = false
        val showButton = RemoteConfigManager.getAdRules().showGetStartedButton
        if (showButton) {
            showGetStarted = true
        } else {
            showGetStarted = false
            triggerNextNavigationStep()
        }
    }

    fun proceedWithAds() {
        setSplashProgress(0.7f)
        val startConfig = RemoteConfigManager.getAdRules()
        val adRules = RemoteConfigManager.getAdRules()
        val hasSplashFullscreenAd =
            adRules.showAppOpenSplashAd || adRules.showSplashInterstitialAd || adRules.showFullScreenNativeSplashAd
        if (!hasSplashFullscreenAd) {
            if (startConfig.showGetStartedButton) showGetStartedButton() else moveToNextScreen("no_fullscreen_ad")
            return
        }
        AdUtils.applyRemoteAdControllerConfig(adMobManager)
        adMobManager.setPremium(isPremium).setSplash(true)
        StartupNavigationManager.preloadNextScreenAd(
            activity, StartupNavigationManager.Step.START, appPreferences, adMobManager
        )
        if (startConfig.showGetStartedButton) showGetStartedButton() else triggerNextNavigationStep()
    }

    fun initConsent() {
        setSplashProgress(0.55f)
        if (!adsConsentManager.canRequestAds) {
            adsConsentManager.showGDPRConsent(activity, false) { error ->
                error?.let { Log.w(TAG, "Consent error: ${it.errorCode} - ${it.message}") }
                AppLovinPrivacySettings.setHasUserConsent(adsConsentManager.canRequestAds)
                AppLovinPrivacySettings.setDoNotSell(adsConsentManager.canRequestAds)
                VunglePrivacySettings.setGDPRStatus(adsConsentManager.canRequestAds, "v1.0.0")
                VunglePrivacySettings.setCCPAStatus(adsConsentManager.canRequestAds)
                proceedWithAds()
            }
        } else {
            proceedWithAds()
        }
    }

    LaunchedEffect(Unit) {
        if (hasStartedSplashWork) return@LaunchedEffect
        hasStartedSplashWork = true

        // Let the splash reach the screen before any of the work below starts. A LaunchedEffect
        // body runs on the main dispatcher as part of the *first* composition, so initialising the
        // ads SDK from here put MobileAds.initialize() and ten mediation adapters — a large chunk
        // of which is main-thread work — inside the first frame. That is the stall users see as
        // the splash appearing already frozen. Suspending for one frame lets the first frame
        // present, then the heavy work runs against a screen that is already up.
        withFrameNanos { }

        Log.i("StartupTiming", "first frame work starting at +${SystemClock.elapsedRealtime() - MyApp.appStartTimeMs}ms")
        (activity.application as MyApp).initializeAdsIfNeeded()
        analyticsManager.get().sendAnalytics(AnalyticsManager.Action.OPENED, "StartActivity")
        analyticsManager.get().sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.SPLASH_VIEW)
        viewModel.initialize()
        splashJob = activity.lifecycleScope.launch {
            delay(SPLASH_MAX_MS)
            if (!hasMovedToNext && !isAdLoadingFinished && !hasTriggeredInterstitialNavigation) {
                showGetStartedButton()
            }
        }
        Looper.myQueue().addIdleHandler {
            if (!activity.isDestroyed) {
                FcmManager.initFcm(activity, appPreferences)
            }
            false
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UIState.Loading -> Unit
            is UIState.Error -> showGetStartedButton()
            is UIState.Success -> {
                if (hasHandledState) return@LaunchedEffect
                hasHandledState = true
                setSplashProgress(0.4f)
                isPremium = state.data.isPremium
                AdMobManager.isPremium = isPremium
                if (isPremium) {
                    if (RemoteConfigManager.getAdRules().showGetStartedButton) {
                        showGetStartedButton()
                    } else {
                        moveToNextScreen("premium")
                    }
                } else if (!state.data.hasInternet) {
                    showGetStartedButton()
                } else {
                    initConsent()
                }
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (pendingNavigationAfterAd) moveToNextScreen("onStart")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (pendingNavigationAfterAd) moveToNextScreen("onResume")
    }

    DisposableEffect(Unit) {
        onDispose {
            mainHandler.removeCallbacksAndMessages(null)
            if (hasStartedSplashWork) {
                adMobManager.setSplash(false)
            }
            splashJob?.cancel()
        }
    }

    BackHandler { activity.finishAffinity() }

    StartContent(
        progress = loadingProgress,
        showProgress = showProgress,
        showGetStarted = showGetStarted,
        onGetStarted = { triggerNextNavigationStep() },
    )
}

@Composable
private fun StartContent(
    progress: Float,
    showProgress: Boolean,
    showGetStarted: Boolean,
    onGetStarted: () -> Unit,
) {
    val isShort = windowSize().isShortScreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Drawn as a brush rather than loaded from R.drawable.bg_splash_gradient:
            // painterResource only handles vectors and bitmaps, and that resource is a <shape>,
            // so loading it threw at composition. The colours are the same two.
            .background(
                Brush.verticalGradient(listOf(SplashGradientStart, SplashGradientEnd)),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Loaded through Coil rather than painterResource: ic_launcher is an <adaptive-icon>
            // on API 26+, which painterResource cannot decode (it threw here). Coil goes through
            // the platform drawable loader, which handles it.
            AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = stringResource(R.string.app_name),
                // Smaller on a landscape phone, where the logo, the title, the progress bar and the
                // Get Started button all have to share 390dp of height.
                modifier = Modifier.size(if (isShort) 64.dp else 100.dp),
            )
            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (showProgress) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .width(180.dp)
                        .height(6.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
        if (showGetStarted) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 24.dp, vertical = 0.dp)
                    .padding(bottom = if (isShort) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        // A minimum rather than a fixed width, so a long translation or a large
                        // font scale grows the button instead of clipping its label.
                        .widthIn(min = 220.dp)
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(stringResource(R.string.get_started), fontWeight = FontWeight.Bold)
                }
                Text(
                    text = stringResource(R.string.this_action_may_contain_ads),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun StartContentPreview() {
    StartContent(
        progress = 0.6f,
        showProgress = true,
        showGetStarted = true,
        onGetStarted = {},
    )
}
