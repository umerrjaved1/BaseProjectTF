package com.mzalogics.docuview.ui.screens

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com
 */

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mzalogics.docuview.R
import com.mzalogics.docuview.app.AdIds
import com.mzalogics.docuview.app.AnalyticsManager
import com.mzalogics.docuview.app.AppPreferences
import com.mzalogics.docuview.constants.Constants
import com.mzalogics.docuview.databinding.ActivityStartBinding
import com.mzalogics.docuview.remoteconfig.RemoteConfigManager
import com.mzalogics.docuview.ui.viewmodel.StartViewModel
import com.mzalogics.docuview.utils.AdFrequencyControl
import com.mzalogics.docuview.utils.AdUnitFrequencyController
import com.mzalogics.docuview.utils.AdUtils
import com.mzalogics.docuview.utils.UIState
import com.umer_tf.ads.domain.consent.AdsConsentManager
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private var nativeAdTimeoutPosted = false

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
    private var hasHandledState = false

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
                            if (hasHandledState) return@collect
                            hasHandledState = true
                            val data = state.data
                            isPremium = data.isPremium
                            AdMobManager.isPremium = isPremium

                            if (isPremium) {
                                if (RemoteConfigManager.shouldShowGetStartedButton()) {
                                    showGetStartedButton()
                                } else {
                                    moveToNextScreen()
                                }
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

    private var isAdLoadingFinished = false
    private var isAnimationFinished = false

    private fun showGetStartedButton() {
        isAdLoadingFinished = true
        checkAndShowUI()
    }

    private fun checkAndShowUI() {
        if (isAdLoadingFinished && isAnimationFinished) {
            binding.llLoading.visibility = View.GONE
            if (RemoteConfigManager.shouldShowGetStartedButton()) {
                binding.btnGetStarted.visibility = View.VISIBLE
                binding.btnGetStarted.setOnClickListener {
                    triggerNextNavigationStep()
                }
            } else {
                binding.btnGetStarted.visibility = View.GONE
            }
        }
    }

    private fun triggerNextNavigationStep() {
        val shouldShowAds = !isPremium && RemoteConfigManager.shouldShowAds()
        if (shouldShowAds) {
            val adStrategy = RemoteConfigManager.getAdsConfig().firstOpenAdStrategy
            when (adStrategy) {
                1 -> showInterstitialAndNavigate()
                2 -> showFullScreenNativeAdAndNavigate()
                3 -> moveToNextScreen() // No Ad
                else -> showOpenAdAndNavigate()
            }
        } else {
            moveToNextScreen()
        }
    }

    private fun startLoadingAnimation() {
        loadingAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = splashDelayLength
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Int
                binding.loadingBar.progress = progress
                @SuppressLint("SetTextI18n")
                binding.tvPercent.text = "$progress%"
                if (progress == 100) {
                    isAnimationFinished = true
                    checkAndShowUI()
                }
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


    private fun startOfflineFlow() {
        if (hasMovedToNext) return
        
        loadingAnimator?.duration = 2000L
        
        splashJob?.cancel()
        splashJob = lifecycleScope.launch {
            if (RemoteConfigManager.shouldShowGetStartedButton()) {
                showGetStartedButton()
            } else {
                delay(2000L.milliseconds)
                moveToNextScreen()
            }
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
            if (RemoteConfigManager.shouldShowGetStartedButton()) {
                showGetStartedButton()
            } else {
                moveToNextScreen()
            }
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

        if (RemoteConfigManager.shouldShowGetStartedButton()) {
            loadAndShowSplashNativeAd()
        } else {
            val adStrategy = RemoteConfigManager.getAdsConfig().firstOpenAdStrategy
            Log.d(TAG, "Ad strategy from remote config: $adStrategy (1=Inter, 0=OpenApp)")
            if (adStrategy == 1) {
                showInterstitialAndNavigate()
            } else {
                showOpenAdAndNavigate()
            }
        }
    }


    private fun loadAndShowSplashNativeAd() {
        Log.e(TAG, "loadNativeAd: ")
        
        scheduleNativeAdTimeout()

        AdUtils.loadAndShowNativeAd(
            adMobManager = adMobManager,
            adUnitId = AdIds.getNativeAdId(),
            layoutResId = R.layout.native_ad_onboarding,
            frameLayout = binding.includeAd.adFrame,
            shimmerFrameLayout = binding.includeAd.shimmerFbAd,
            showMedia = true,
            nativeAdConfigIndex = 2
        ) {
            lifecycleScope.launch {
                nativeAdTimeoutPosted = false
                showGetStartedButton()
            }
        }
    }

    private fun scheduleNativeAdTimeout() {
        if (nativeAdTimeoutPosted) return
        nativeAdTimeoutPosted = true
        binding.root.postDelayed({
            if (nativeAdTimeoutPosted) {
                nativeAdTimeoutPosted = false
                binding.includeAd.shimmerFbAd.stopShimmer()
                binding.includeAd.shimmerFbAd.visibility = View.GONE
                showGetStartedButton()
            }
        }, 10000)
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
            proceedAfterInterstitial(false)
            return
        }
        val hfAdId = "ca-app-pub-3940256099942544/1033173515"//AdIds.getInterstitialSplashHfAdId()
        val normalAdId = AdIds.getInterstitialSplashAdId()

        AdUtils.loadAndShowWaterfallInterAdWithDialog(
            this,
            adMobManager,
            hfAdId,
            normalAdId,
            lifecycleScope
        ) { adShown ->
            proceedAfterInterstitial(adShown)
        }
    }

    private fun proceedAfterInterstitial(adShown: Boolean) {
        navigateAfterAd()
    }

    private fun navigateAfterAd() {
        val strategy = RemoteConfigManager.getAdsConfig().splashAdPostNavigationStrategy
        if (strategy == 1) {
            hasShownPremiumAfterInterstitial = true
            isAdShow = true
            startActivity(Intent(this@StartActivity, PremiumActivity::class.java))
        } else {
            moveToNextScreen()
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
            navigateAfterAd()
            return
        }
        adMobManager.appOpenAdLoader.loadAppOpenAd(this) { isLoaded ->
            if (isLoaded) {
                adMobManager.appOpenAdLoader.showAppOpenAdIfAvailable { _ ->
                    AdFrequencyControl.recordAdShown(
                        this@StartActivity,
                        AdUnitFrequencyController.UNIT_OPEN_AD
                    )
                    navigateAfterAd()
                }
            } else {
                navigateAfterAd()
            }
        }
    }

    private fun showFullScreenNativeAdAndNavigate() {
        Log.e(TAG, "showFullScreenNativeAdAndNavigate")
        if (hasTriggeredInterstitialNavigation) return
        hasTriggeredInterstitialNavigation = true

        AdUtils.loadAndShowFullScreenNativeAdWithDialog(
            activity = this,
            adUnitId = AdIds.getNativeAdId()
        ) { adShown ->
            navigateAfterAd()
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

            !isOnboarding -> {
                Intent(this, OnboardingActivity::class.java)
            }

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
                } else {
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
