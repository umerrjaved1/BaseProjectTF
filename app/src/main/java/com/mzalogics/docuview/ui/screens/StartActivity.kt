package com.mzalogics.docuview.ui.screens

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com
 */

import android.animation.ValueAnimator
import android.annotation.SuppressLint
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mzalogics.ads.domain.consent.AdsConsentManager
import com.mzalogics.ads.domain.core.AdMobManager
import com.mzalogics.docuview.app.AdIds
import com.mzalogics.docuview.app.AnalyticsManager
import com.mzalogics.docuview.utils.AdFrequencyControl
import com.mzalogics.docuview.utils.AdUnitFrequencyController
import com.mzalogics.docuview.app.AppPreferences
import com.mzalogics.docuview.constants.Constants
import com.mzalogics.docuview.remoteconfig.RemoteConfigManager
import com.mzalogics.docuview.ui.viewmodel.StartViewModel
import com.mzalogics.docuview.utils.UIState
import com.mzalogics.docuview.databinding.ActivityStartBinding

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class StartActivity : AppCompatActivity() {

    private val TAG = StartActivity::class.java.simpleName

    private lateinit var binding: ActivityStartBinding
    private var splashJob: Job? = null
    private var loadingAnimator: ValueAnimator? = null

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences


    private val adsConsentManager by lazy { AdsConsentManager(this) }

    private val viewModel: StartViewModel by viewModels()


    private var isPremium: Boolean = false


    private val isLanguageSelected: Boolean
        get() = appPreferences.getBoolean(AppPreferences.Companion.IS_LANGUAGE_SELECTED, false)

    private val isOnboarding: Boolean
        get() = appPreferences.getBoolean(AppPreferences.Companion.IS_ONBOARDING, false)


    private var hasMovedToNext = false
    private var isAdShow = false
    private var hasTriggeredInterstitialNavigation = false
    private var hasShownPremiumAfterInterstitial = false

    private val splashDelayLength = 8000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true // Dark icons for light background


        // Preload somewhere sensible (e.g., onResume)


        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UIState.Loading -> Unit
                        is UIState.Error -> {
                            Log.e(TAG, "Startup error: ${state.throwable.message}")
                            startOfflineFlow()
                        }

                        is UIState.Success -> {
                            val data = state.data
                            isPremium = data.isPremium
                            AdMobManager.isPremium = isPremium

                            if (isPremium) {
                                moveToNextScreen()
                            } else {
                                if (!data.hasInternet) {
                                    Log.w(TAG, "No internet at start. Proceeding without ads.")
                                    startOfflineFlow()
                                } else {
                                    initConsent()
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.initialize()

        startLoadingAnimation()
    }

    private fun startLoadingAnimation() {
        loadingAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = splashDelayLength
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Int
                binding.loadingBar.progress = progress
                @SuppressLint("SetTextI18n")
                binding.tvPercent.text = "$progress%"
            }
            start()
        }
    }

    override fun onStart() {
        super.onStart()
        if (isAdShow) {
            moveToNextScreen()
            isAdShow = false
        }

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
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    private fun startOfflineFlow() {
        if (hasMovedToNext) return
        splashJob?.cancel()
        splashJob = lifecycleScope.launch {
            delay(1200L)
            moveToNextScreen()
        }
    }

    private fun initConsent() {
        if (!adsConsentManager.canRequestAds) {
            adsConsentManager.showGDPRConsent(this, false) { error ->
                error?.let {
                    Log.w(TAG, "Consent error: ${it.errorCode} - ${it.message}")
                }
                setMediationConsent(adsConsentManager.canRequestAds)
                proceedWithAds()
            }
        } else {
            proceedWithAds()
        }
    }

    private fun setMediationConsent(isConsent: Boolean) {
        Log.e(TAG, "setMediationConsent: ")
//        AppLovinPrivacySettings.setHasUserConsent(isConsent)
//        AppLovinPrivacySettings.setDoNotSell(isConsent)
//        VunglePrivacySettings.setGDPRStatus(isConsent, "v1.0.0")
//        VunglePrivacySettings.setCCPAStatus(isConsent)

    }

    private fun proceedWithAds() {
        if (!RemoteConfigManager.shouldShowAds()) {
            moveToNextScreen()
            return
        }

        Log.d(TAG, "Initializing Ads")

        adMobManager
            .setInterstitialAdMaxTime(
                RemoteConfigManager.getAdsConfig().interstitialMaxTimer?.toLong() ?: 20
            ).setInterstitialAdMinTime(
                RemoteConfigManager.getAdsConfig().interstitialMinTimer?.toLong() ?: 10
            ).setInterstitialCounter(RemoteConfigManager.getAdsConfig().interstitialCounter ?: 2)
            .setOpenAdResumeTime(
                RemoteConfigManager.getAdsConfig().openAdResumeTimer?.toLong() ?: 10
            ).setPremium(isPremium).setSplash(true)
            .setAppOpenAdStartId(AdIds.getAppOpenAdId())

        if (!isLanguageSelected) {
            adMobManager.nativeAdLoader.loadAd(AdIds.getNativeLanguageAdId())
        }

        val adStrategy = RemoteConfigManager.getAdsConfig().firstOpenAdStrategy
        Log.d(TAG, "Ad strategy from remote config: $adStrategy (1=Inter, 0=OpenApp)")
        if (adStrategy == 1) {
            showInterstitialAndNavigate()
        } else {
            showOpenAdAndNavigate()
        }
    }

    private fun showInterstitialAndNavigate() {
        Log.e(TAG, "showInterstitialAndNavigate")
        if (hasTriggeredInterstitialNavigation) return
        hasTriggeredInterstitialNavigation = true

        if (isPremium) {
            moveToNextScreen()
            return
        }
        if (!AdFrequencyControl.canShowAd(this, AdUnitFrequencyController.UNIT_INTERSTITIAL)) {
            hasShownPremiumAfterInterstitial = true
            isAdShow = true
            startActivity(Intent(this@StartActivity, PremiumActivity::class.java))
            return
        }
        adMobManager.interstitialAdLoader.loadAndShowAd(
            this,
            AdIds.getInterstitialSplashAdId(),
            false
        ) {
            AdFrequencyControl.recordAdShown(this@StartActivity, AdUnitFrequencyController.UNIT_INTERSTITIAL)
            hasShownPremiumAfterInterstitial = true
            isAdShow = true
            startActivity(Intent(this@StartActivity, PremiumActivity::class.java))
        }
    }

    private fun showOpenAdAndNavigate() {
        Log.e(TAG, "showOpenAdAndNavigate")
        if (hasTriggeredInterstitialNavigation) return
        hasTriggeredInterstitialNavigation = true

        if (isPremium) {
            moveToNextScreen()
            return
        }
        if (!AdFrequencyControl.canShowAd(this, AdUnitFrequencyController.UNIT_OPEN_AD)) {
            hasShownPremiumAfterInterstitial = true
            isAdShow = true
            startActivity(Intent(this@StartActivity, PremiumActivity::class.java))
            return
        }
        adMobManager.appOpenAdLoader.loadAppOpenAd(this) { isLoaded ->
            if (isLoaded) {
                adMobManager.appOpenAdLoader.showAppOpenAdIfAvailable { _ ->
                    AdFrequencyControl.recordAdShown(this@StartActivity, AdUnitFrequencyController.UNIT_OPEN_AD)
                    hasShownPremiumAfterInterstitial = true
                    isAdShow = true
                    startActivity(Intent(this@StartActivity, PremiumActivity::class.java))
                }
            } else {
                hasShownPremiumAfterInterstitial = true
                isAdShow = true
                startActivity(Intent(this@StartActivity, PremiumActivity::class.java))
            }
        }
    }

    private fun moveToNextScreen() {
        Log.e(TAG, "moveToNextScreen: ")
        if (hasMovedToNext) return
        hasMovedToNext = true
        splashJob?.cancel()
        splashJob = null

        val nextActivity = when {
            !isLanguageSelected -> {
                Intent(this, LanguageActivity::class.java).apply {
                    putExtra(Constants.EXTRA_LANGUAGE_FROM_START, true)
                }
            }

//            !isOnboarding -> {
//                Intent(this, OnboardingActivity::class.java)
//            }

            isPremium -> {
                // User is already premium — go straight to main, never loop through PremiumActivity
                Intent(this, MainActivity::class.java)
            }

            else -> {
                if (RemoteConfigManager.getShowPremiumActivityAfterThreeClick() && !hasShownPremiumAfterInterstitial) {
                    Intent(
                        this,
                        PremiumActivity::class.java
                    ).putExtra(Constants.EXTRA_PREMIUM_FROM_SPLASH, true)
                }else{
                    Intent(this, MainActivity::class.java)
                }
            }

        }
        // Guard: don't start on a finishing/destroyed Activity
        if (!isFinishing && !isDestroyed) {
            startActivity(nextActivity)
            finish()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        loadingAnimator?.cancel()
        adMobManager.setSplash(false)
        splashJob?.cancel()
    }
}
