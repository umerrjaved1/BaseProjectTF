package com.professor.baseproject.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.mzalogics.ads.domain.consent.AdsConsentManager
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.baseproject.databinding.ActivityStartBinding
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.app.AdIds
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.ui.viewmodel.NextScreen
import com.professor.baseproject.ui.viewmodel.StartViewModel
import com.professor.baseproject.utils.UIState
import com.professor.baseproject.utils.mainCoroutine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class StartActivity : AppCompatActivity() {

    private val TAG = StartActivity::class.java.simpleName

    private lateinit var binding: ActivityStartBinding
    private val viewModel: StartViewModel by viewModels()

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    private val adsConsentManager by lazy { AdsConsentManager(this) }

    private var hasMovedToNext = false
    private var isAdShow = false
    private var job: Job? = null
    private var adLoadJob: Job? = null
    private var consentJob: Job? = null

    // Configurable constants with timeouts for limited internet
    private companion object {
        const val SPLASH_DELAY = 8000L
        const val MIN_SPLASH_TIME = 2000L
        const val REMOTE_CONFIG_TIMEOUT = 5000L
        const val AD_LOAD_TIMEOUT = 5000L
        const val CONSENT_TIMEOUT = 3000L
    }

    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startTime = System.currentTimeMillis()

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        initializeApp()
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        if (isAdShow) {
            moveToNextScreen()
            isAdShow = false
        }
    }

    private fun setupWindowInsets() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
    }

    private fun initializeApp() {
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)

        // Start the main splash timer regardless of internet conditions
        setupSplashTimer()

        // Start minimum splash time enforcement
        ensureMinimumSplashTime {
            if (!hasMovedToNext && viewModel.uiState.value is UIState.Error) {
                Log.w(TAG, "Minimum time reached with error state, moving to next screen")
                moveToNextScreen()
            }
        }

        // Initialize app components with timeout
        initializeWithTimeout()
    }

    private fun initializeWithTimeout() {
        lifecycleScope.launch {
            try {
                withTimeout(REMOTE_CONFIG_TIMEOUT) {
                    viewModel.initializeApp()
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Remote config initialization timed out, using cached values")
                // Even if timeout, try to proceed with whatever state we have
                handleInitializationWithTimeout()
            }
        }
    }

    private fun handleInitializationWithTimeout() {
        // Force update UI state if we timed out
        if (viewModel.uiState.value is UIState.Loading) {
            Log.d(TAG, "Proceeding with cached remote config due to timeout")
            // We'll rely on the existing timer to move forward
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UIState.Loading -> {
                        Log.d(TAG, "App initialization in progress...")
                        // Show loading state if needed in UI
                        showLoadingState()
                    }
                    is UIState.Success -> {
                        Log.d(TAG, "App initialization successful")
                        handleInitializationSuccess()
                    }
                    is UIState.Error -> {
                        Log.e(TAG, "App initialization failed: ${state.throwable.message}")
                        showErrorState(state.throwable)
                        // Don't move immediately, wait for minimum time or timer
                        // The existing timers will handle navigation
                    }
                }
            }
        }
    }

    private fun showLoadingState() {
        // Update UI to show loading state if needed
        // binding.loadingBar.isVisible = true
    }

    private fun showErrorState(throwable: Throwable) {
        // Update UI to show error state if needed
        // binding.loadingBar.isVisible = false
        Log.w(TAG, "Continuing with app despite initialization error: ${throwable.message}")
    }

    private fun handleInitializationSuccess() {
        Log.d(TAG, "Handling initialization success, premium: ${viewModel.isPremium}")

        if (viewModel.isPremium) {
            Log.d(TAG, "User is premium, skipping ads and consent")
            // Premium users don't need ads, but still wait for minimum time
        } else {
            Log.d(TAG, "User is not premium, initializing consent and ads")
            initConsentWithTimeout()
        }
    }

    private fun initConsentWithTimeout() {
        consentJob = lifecycleScope.launch {
            try {
                withTimeout(CONSENT_TIMEOUT) {
                    if (!adsConsentManager.canRequestAds) {
                        Log.d(TAG, "Showing GDPR consent form")
                        showConsentForm()
                    } else {
                        Log.d(TAG, "Consent already granted, proceeding with ads")
                        proceedWithAds()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Consent initialization timed out, proceeding with default consent")
                setMediationConsent(false)
                proceedWithAds()
            }
        }
    }

    private suspend fun showConsentForm() {
        try {
            withTimeout(CONSENT_TIMEOUT) {
                // Convert callback to suspend function
                suspendCoroutine { continuation ->
                    adsConsentManager.showGDPRConsent(this@StartActivity, false) { error ->
                        error?.let {
                            Log.w(TAG, "Consent error: ${it.errorCode} - ${it.message}")
                        }
                        setMediationConsent(adsConsentManager.canRequestAds)
                        continuation.resumeWith(Result.success(Unit))
                        proceedWithAds()
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Consent form display timed out, proceeding with default consent")
            setMediationConsent(false)
            proceedWithAds()
        }
    }

    private fun setMediationConsent(isConsent: Boolean) {
        Log.d(TAG, "setMediationConsent: $isConsent")
        // Your mediation consent code here
        // AppLovinPrivacySettings.setHasUserConsent(isConsent)
        // AppLovinPrivacySettings.setDoNotSell(isConsent)
        // VunglePrivacySettings.setGDPRStatus(isConsent, "v1.0.0")
        // VunglePrivacySettings.setCCPAStatus(isConsent)
    }

    private fun proceedWithAds() {
        Log.d(TAG, "Proceeding with ads initialization")

        setupAdMobManager()
        preloadAdsWithTimeout()
    }

    private fun setupAdMobManager() {
        val shouldShowAds = !RemoteConfigManager.getDisableAds()
        Log.d(TAG, "setupAdMobManager - Ads Enabled: $shouldShowAds")
        
        adMobManager.setAppOpenAdResumeId(AdIds.getAppOpenAdIdResume())
            .setAppOpenAdStartId(AdIds.getAppOpenAdId())
            .setShouldShowResumeAd(shouldShowAds)
            .setInterstitialAdMaxTime(
                RemoteConfigManager.getAdsConfig().interstitialMaxTimer?.toLong() ?: 20
            ).setInterstitialAdMinTime(
                RemoteConfigManager.getAdsConfig().interstitialMinTimer?.toLong() ?: 10
            ).setInterstitialCounter(RemoteConfigManager.getAdsConfig().interstitialCounter ?: 2)
            .setOpenAdResumeTime(
                RemoteConfigManager.getAdsConfig().openAdResumeTimer?.toLong() ?: 10
            ).setPremium(true)
            .setSplash(true)
    }

    private fun preloadAdsWithTimeout() {
        // Preload native ad for language screen with timeout
        if (!viewModel.isLanguageSelected) {
            lifecycleScope.launch {
                try {
                    withTimeout(AD_LOAD_TIMEOUT) {
                        Log.d(TAG, "Preloading native ad for language screen")
                        adMobManager.nativeAdLoader.loadAd(AdIds.getNativeLanguageAdId())
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Native ad loading timed out")
                }
            }
        }

        // Load splash interstitial ad with timeout handling
        loadSplashInterstitialAdWithTimeout()
    }

    private fun loadSplashInterstitialAdWithTimeout() {
        var adLoadCompleted = false

        // Set timeout for ad loading
        adLoadJob = lifecycleScope.launch {
            delay(AD_LOAD_TIMEOUT)
            if (!adLoadCompleted && !hasMovedToNext) {
                Log.w(TAG, "Interstitial ad loading timed out, will proceed without ad")
                // Don't move here, let the main timer handle navigation
            }
        }

        Log.d(TAG, "Loading interstitial ad for splash")
        adMobManager.interstitialAdLoader.loadAd(AdIds.getInterstitialSplashAdId()) { isAdLoaded ->
            adLoadCompleted = true
            adLoadJob?.cancel()

            lifecycleScope.launch {
                if (isAdLoaded && !hasMovedToNext) {
                    Log.d(TAG, "Interstitial ad loaded successfully, will show after minimum time")
                    job?.cancel() // Cancel the main timer since we'll show ad
                    ensureMinimumSplashTime {
                        showInterstitialAd()
                    }
                } else {
                    Log.d(TAG, "Interstitial ad not loaded, relying on main timer")
                    // Continue with normal flow, main timer will handle navigation
                }
            }
        }
    }

    private fun showInterstitialAd() {
        if (hasMovedToNext) {
            Log.d(TAG, "Already moved to next screen, skipping ad show")
            return
        }

        Log.d(TAG, "Showing interstitial ad")
        isAdShow = true
        adMobManager.interstitialAdLoader.showAd(
            this, AdIds.getInterstitialSplashAdId()
        ) {
            Log.d(TAG, "Interstitial ad dismissed, moving to next screen")
            moveToNextScreen()
        }
    }

    private fun setupSplashTimer() {
        job = mainCoroutine {
            delay(SPLASH_DELAY)
            if (!hasMovedToNext) {
                Log.d(TAG, "Splash timer completed after $SPLASH_DELAY ms, moving to next screen")
                moveToNextScreen()
            }
        }
    }

    private fun ensureMinimumSplashTime(onComplete: () -> Unit) {
        val elapsedTime = System.currentTimeMillis() - startTime
        val remainingTime = MIN_SPLASH_TIME - elapsedTime

        if (remainingTime > 0) {
            Log.d(TAG, "Ensuring minimum splash time: ${remainingTime}ms remaining")
            lifecycleScope.launch {
                delay(remainingTime)
                onComplete()
            }
        } else {
            Log.d(TAG, "Minimum splash time already reached")
            onComplete()
        }
    }

    private fun moveToNextScreen() {
        if (hasMovedToNext) {
            Log.d(TAG, "Already moved to next screen, skipping")
            return
        }

        hasMovedToNext = true
        Log.d(TAG, "Moving to next screen")

        // Cancel all jobs
        job?.cancel()
        job = null
        adLoadJob?.cancel()
        consentJob?.cancel()

        val nextIntent = when (viewModel.getNextScreenIntent()) {
            NextScreen.LANGUAGE -> {
                Log.d(TAG, "Navigating to LanguageActivity")
                Intent(this, LanguageActivity::class.java).apply {
                    putExtra(Constants.IS_FROM_START, true)
                }
            }
            NextScreen.ONBOARDING -> {
                Log.d(TAG, "Navigating to OnboardingActivity")
                Intent(this, OnboardingActivity::class.java)
            }
            NextScreen.MAIN -> {

               /* Intent(this, PremiumActivity::class.java).apply {
                    putExtra(Constants.IS_FROM_START_TO_PREMIUM, true)
                }*/
                 Intent(this, MainActivity::class.java)

            }
        }

        startActivity(nextIntent)
        finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Cleaning up resources")
        adMobManager.setSplash(false)
        job?.cancel()
        adLoadJob?.cancel()
        consentJob?.cancel()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }
}
