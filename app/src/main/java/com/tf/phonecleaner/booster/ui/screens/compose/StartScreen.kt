package com.tf.phonecleaner.booster.ui.screens.compose

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tf.phonecleaner.booster.R
import com.tf.phonecleaner.booster.app.AnalyticsManager
import com.tf.phonecleaner.booster.app.AdIds
import androidx.lifecycle.lifecycleScope
import com.tf.phonecleaner.booster.remoteconfig.RemoteConfigManager
import com.tf.phonecleaner.booster.ui.components.NativeAdView
import com.tf.phonecleaner.booster.ui.viewmodel.StartData
import com.tf.phonecleaner.booster.ui.viewmodel.StartViewModel
import com.tf.phonecleaner.booster.utils.AdFrequencyControl
import com.tf.phonecleaner.booster.utils.AdUnitFrequencyController
import com.tf.phonecleaner.booster.utils.AdUtils
import com.tf.phonecleaner.booster.utils.UIState
import com.umer_tf.ads.domain.core.AdMobManager
import kotlinx.coroutines.delay

@Composable
fun StartScreen(
    onNavigateToLanguage: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: StartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? AppCompatActivity

    // We only want to trigger initialization once
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    var showGetStarted by remember { mutableStateOf(false) }
    var isTimerFinished by remember { mutableStateOf(false) }
    
    // Fallback timer just in case config fails or ad fails
    LaunchedEffect(uiState) {
        if (uiState is UIState.Success) {
            delay(10000L) // 10 seconds max wait
            isTimerFinished = true
            showGetStarted = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon), // Use a PNG or VectorDrawable to avoid the crash
                contentDescription = "App Icon",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )

            if (!showGetStarted && !isTimerFinished) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator()
            }
        }

        // Native Ad at bottom if enabled
        if (uiState is UIState.Success) {
            val startData = (uiState as UIState.Success<StartData>).data
            val startConfig = RemoteConfigManager.getStartScreenConfig()

            if (startConfig.showSplashNativeAd && !startData.isPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    NativeAdView(
                        adMobManager = viewModel.adMobManager, // We need to expose adMobManager from viewModel or inject it
                        adUnitId = AdIds.getNativeAdId(),
                        layoutResId = R.layout.native_ad_onboarding,
                        shimmerLayoutResId = R.layout.shimmer_template_onboarding,
                        nativeConfig = startConfig.nativeConfig,
                        onAdLoaded = {
                            showGetStarted = true
                        }
                    )
                }
            }

            if (showGetStarted && startConfig.showGetStartedButton) {
                Button(
                    onClick = {
                        handleNavigation(activity, startData, viewModel.adMobManager, viewModel.analyticsManager, onNavigateToLanguage, onNavigateToOnboarding, onNavigateToPremium, onNavigateToMain)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                ) {
                    Text("Get Started")
                }
            } else if (showGetStarted || isTimerFinished) {
                // If button is NOT supposed to be shown, we just navigate automatically
                LaunchedEffect(Unit) {
                    handleNavigation(activity, startData, viewModel.adMobManager, viewModel.analyticsManager, onNavigateToLanguage, onNavigateToOnboarding, onNavigateToPremium, onNavigateToMain)
                }
            }
        }
    }
}

private fun handleNavigation(
    activity: AppCompatActivity?,
    startData: StartData,
    adMobManager: AdMobManager,
    analyticsManager: AnalyticsManager,
    onNavigateToLanguage: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    if (activity == null) return

    val startConfig = RemoteConfigManager.getStartScreenConfig()
    
    val navigateNext = {
        when {
            !startData.isLanguageSelected -> onNavigateToLanguage()
            !startData.isOnboardingDone -> onNavigateToOnboarding()
            startData.isPremium -> onNavigateToMain()
            else -> {
                if (RemoteConfigManager.getPremiumScreenConfig().showPremiumActivityAfterThreeClick) {
                    onNavigateToPremium()
                } else {
                    onNavigateToMain()
                }
            }
        }
    }

    if (startData.isPremium) {
        navigateNext()
        return
    }

    val adStrategy = startConfig.firstOpenAdStrategy
    if (adStrategy == 1 && startConfig.showWelcomeInterstitialAd) {
        AdUtils.loadAndShowWaterfallInterAdWithDialog(
            activity = activity,
            adMobManager = adMobManager,
            hfAdUnit = "ca-app-pub-3940256099942544/1033173515",
            normalAdUnit = AdIds.getInterstitialSplashAdId(),
            lifecycleScope = activity.lifecycleScope, // Use the activity's lifecycleScope
            analyticsManager = analyticsManager,
            eventNamePrefix = "splash_int"
        ) {
            navigateNext()
        }
    } else if (startConfig.showAppOpenSplashAd) {
        if (!AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_OPEN_AD)) {
            navigateNext()
            return
        }
        adMobManager.appOpenAdLoader.loadAppOpenAd(activity) { isLoaded ->
            if (isLoaded) {
                adMobManager.appOpenAdLoader.showAppOpenAdIfAvailable {
                    AdFrequencyControl.recordAdShown(activity, AdUnitFrequencyController.UNIT_OPEN_AD)
                    navigateNext()
                }
            } else {
                navigateNext()
            }
        }
    } else {
        navigateNext()
    }
}
