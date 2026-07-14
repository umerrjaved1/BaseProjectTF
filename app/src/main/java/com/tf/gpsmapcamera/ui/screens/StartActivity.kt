package com.tf.gpsmapcamera.ui.screens

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com
 */

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.ui.viewmodel.StartData
import com.tf.gpsmapcamera.ui.viewmodel.StartViewModel
import com.tf.gpsmapcamera.update.AppUpdateDialogFragment
import com.tf.gpsmapcamera.update.AppUpdateManager
import com.tf.gpsmapcamera.update.AppUpdateReadyDialogFragment
import com.tf.gpsmapcamera.utils.AdFrequencyControl
import com.tf.gpsmapcamera.utils.AdUnitFrequencyController
import com.tf.gpsmapcamera.utils.AdUtils
import com.tf.gpsmapcamera.utils.UIState
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.databinding.ActivityStartBinding
import com.tf.gpsmapcamera.utils.StartupNavigationManager
import com.umer_tf.ads.domain.consent.AdsConsentManager
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.view.animation.AnimationUtils
import com.applovin.sdk.AppLovinPrivacySettings
import com.tf.gpsmapcamera.constants.Constants
import com.tf.gpsmapcamera.utils.setClickWithTimeout
import com.tf.gpsmapcamera.utils.startShakeAnimation
import com.vungle.ads.VunglePrivacySettings
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

    @Inject
    lateinit var appUpdateManager: AppUpdateManager


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
    private var hasHandledState = false
    private var pendingStartData: StartData? = null
    private var currentUpdateType: Int? = null
    private var isImmediateUpdate = false

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        appUpdateManager.handleUpdateResult(
            resultCode = result.resultCode,
            isImmediate = isImmediateUpdate,
            onImmediateCanceled = { showAppUpdateDialog(isImmediate = true) },
            onFlexibleStarted = { proceedWithStartupIfPending() }
        )
    }

    private val splashDelayLength = 8000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, com.tf.gpsmapcamera.R.color.bg_color)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true // Dark icons for light background

        // Preload somewhere sensible (e.g., onResume)

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)
        analyticsManager.sendAnalytics(
            AnalyticsManager.Action.ACTION_TYPE,
            AnalyticsManager.Events.SPLASH_VIEW
        )

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
                            checkForAppUpdateAndProceed(data)
                        }
                    }
                }
            }
        }

        viewModel.initialize()

        startLoadingAnimation()

        appUpdateManager.setOnFlexibleDownloadCompleteListener {
            showFlexibleUpdateReadyDialog()
        }
    }

    private fun checkForAppUpdateAndProceed(data: StartData) {
        if (!data.hasInternet) {
            proceedWithStartup(data)
            return
        }

        lifecycleScope.launch {
            when (val result = appUpdateManager.checkForUpdate()) {
                is AppUpdateManager.UpdateCheckResult.Available -> {
                    pendingStartData = data
                    currentUpdateType = result.updateType
                    isImmediateUpdate = result.isImmediate
                    showAppUpdateDialog(result.isImmediate)
                }

                else -> proceedWithStartup(data)
            }
        }
    }

    private fun proceedWithStartupIfPending() {
        pendingStartData?.let { proceedWithStartup(it) }
    }

    private fun proceedWithStartup(data: StartData) {
        pendingStartData = null

        if (isPremium) {
            if (RemoteConfigManager.getStartScreenConfig().showGetStartedButton) {
                showGetStartedButton()
            } else {
                moveToNextScreen()
            }
        } else if (!data.hasInternet) {
            Log.w(TAG, "No internet at start. Proceeding without ads.")
            startOfflineFlow()
        } else {
            initConsent()
        }
    }

    private fun showAppUpdateDialog(isImmediate: Boolean) {
        if (supportFragmentManager.findFragmentByTag(AppUpdateDialogFragment.TAG) != null) return

        val dialog = AppUpdateDialogFragment.newInstance(isImmediate)
        dialog.setOnUpdateNowListener {
            val updateType = currentUpdateType
            if (updateType != null) {
                appUpdateManager.startUpdate(this, updateType, updateLauncher)
            } else {
                appUpdateManager.openPlayStore(this)
                if (!isImmediate) proceedWithStartupIfPending()
            }
        }
        dialog.setOnUpdateLaterListener {
            proceedWithStartupIfPending()
        }
        dialog.show(supportFragmentManager, AppUpdateDialogFragment.TAG)
    }

    private fun showFlexibleUpdateReadyDialog() {
        if (isFinishing || isDestroyed) return
        if (supportFragmentManager.findFragmentByTag(AppUpdateReadyDialogFragment.TAG) != null) return

        val dialog = AppUpdateReadyDialogFragment.newInstance()
        dialog.setOnInstallListener {
            appUpdateManager.completeFlexibleUpdate()
        }
        dialog.show(supportFragmentManager, AppUpdateReadyDialogFragment.TAG)
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.handleOnResume(this, updateLauncher)
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
            if (RemoteConfigManager.getStartScreenConfig().showGetStartedButton) {
                binding.shimmerBtn.visibility = View.VISIBLE
                binding.btnGetStarted.visibility = View.VISIBLE
                binding.tvAdsDisclaimer.visibility = View.VISIBLE
                binding.shimmerBtn.startShakeAnimation(lifecycleScope)
                binding.btnGetStarted.setClickWithTimeout {
                    val popAnim = AnimationUtils.loadAnimation(this, R.anim.pop_button)
                    binding.btnGetStarted.startAnimation(popAnim)
                    triggerNextNavigationStep()
                }
            } else {
                binding.shimmerBtn.visibility = View.GONE
                binding.btnGetStarted.visibility = View.GONE
                binding.tvAdsDisclaimer.visibility = View.GONE
            }
        }
    }

    private fun triggerNextNavigationStep() {
        val startConfig = RemoteConfigManager.getStartScreenConfig()
        
        if (isPremium) {
            moveToNextScreen()
            return
        }

        if (startConfig.showSplashInterstitialAd) {
            showInterstitialAndNavigate()
        } else if (startConfig.showAppOpenSplashAd) {
            showOpenAdAndNavigate()
        } else if (startConfig.showFullScreenNativeSplashAd) {
            showFullScreenNativeAdAndNavigate()
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
                // binding.tvPercent.text = "$progress%"
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
            if (RemoteConfigManager.getStartScreenConfig().showGetStartedButton) {
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
        AppLovinPrivacySettings.setHasUserConsent(isConsent)
        AppLovinPrivacySettings.setDoNotSell(isConsent)
        VunglePrivacySettings.setGDPRStatus(isConsent, "v1.0.0")
        VunglePrivacySettings.setCCPAStatus(isConsent)

    }

    private fun proceedWithAds() {
        val startConfig = RemoteConfigManager.getStartScreenConfig()
        // If all ads on start screen are disabled
        if (!startConfig.showAppOpenSplashAd && !startConfig.showSplashInterstitialAd && !startConfig.showSplashNativeAd && !startConfig.showFullScreenNativeSplashAd) {
            if (startConfig.showGetStartedButton) {
                showGetStartedButton()
            } else {
                moveToNextScreen()
            }
            return
        }

        Log.d(TAG, "Initializing Ads")

        val globalRules = RemoteConfigManager.getGlobalAdRulesConfig()
        adMobManager
            .setInterstitialAdMaxTime(globalRules.interstitialMaxTimer.toLong())
            .setInterstitialAdMinTime(globalRules.interstitialMinTimer.toLong())
            .setInterstitialCounter(globalRules.interstitialCounter)
            .setOpenAdResumeTime(globalRules.openAdResumeTimer.toLong())
            .setPremium(isPremium).setSplash(true)
            .setAppOpenAdStartId(AdIds.getAppOpenAdId())
            .setAppOpenAdResumeId(AdIds.getAppResumeAdId())

        StartupNavigationManager.preloadNextScreenAd(this, StartupNavigationManager.Step.START, appPreferences, adMobManager)

        if (startConfig.showGetStartedButton) {
            if (startConfig.showSplashNativeAd) {
                loadAndShowSplashNativeAd()
            } else {
                showGetStartedButton()
            }
        } else {
            if (startConfig.showSplashInterstitialAd) {
                showInterstitialAndNavigate()
            } else if (startConfig.showAppOpenSplashAd) {
                showOpenAdAndNavigate()
            } else if (startConfig.showFullScreenNativeSplashAd) {
                showFullScreenNativeAdAndNavigate()
            } else {
                moveToNextScreen()
            }
        }
    }


    private fun loadAndShowSplashNativeAd() {
        Log.e(TAG, "loadNativeAd: ")

        scheduleNativeAdTimeout()

        AdUtils.loadAndShowNativeAd(
            adMobManager = adMobManager,
            adUnitId = AdIds.getNativeAdId(),
            layoutResId = R.layout.native_ad_modern,
            frameLayout = binding.includeAd.adFrame,
            shimmerFrameLayout = binding.includeAd.shimmerFbAd,
            showMedia = true,
            analyticsManager = analyticsManager,
            eventNamePrefix = "splash_native"
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
        val hfAdId = AdIds.getInterstitialSplashHfAdId()
        val normalAdId = AdIds.getInterstitialSplashAdId()

        AdUtils.loadAndShowWaterfallInterAdWithDialog(
            this,
            adMobManager,
            hfAdId,
            normalAdId,
            lifecycleScope,
            analyticsManager,
            "splash_int"
        ) { adShown ->
            proceedAfterInterstitial(adShown)
        }
    }

    private fun proceedAfterInterstitial(adShown: Boolean) {
        navigateAfterAd()
    }

    private fun navigateAfterAd() {
        moveToNextScreen()
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
        analyticsManager.sendAnalytics(
            AnalyticsManager.Action.ACTION_TYPE,
            AnalyticsManager.Events.APPOPEN_REQUEST
        )
        adMobManager.appOpenAdLoader.loadAppOpenAd(this) { isLoaded ->
            if (isLoaded) {
                analyticsManager.sendAnalytics(
                    AnalyticsManager.Action.ACTION_TYPE,
                    AnalyticsManager.Events.APPOPEN_REQUEST_PASS
                )
                adMobManager.appOpenAdLoader.showAppOpenAdIfAvailable { _ ->
                    analyticsManager.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE,
                        AnalyticsManager.Events.APPOPEN_VIEW
                    )
                    AdFrequencyControl.recordAdShown(
                        this@StartActivity,
                        AdUnitFrequencyController.UNIT_OPEN_AD
                    )
                    navigateAfterAd()
                }
            } else {
                analyticsManager.sendAnalytics(
                    AnalyticsManager.Action.ACTION_TYPE,
                    AnalyticsManager.Events.APPOPEN_REQUEST_FAIL
                )
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
            adUnitId = AdIds.getNativeAdId(),
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

        val nextActivity = com.tf.gpsmapcamera.utils.StartupNavigationManager.getNextIntent(this, com.tf.gpsmapcamera.utils.StartupNavigationManager.Step.START, appPreferences)
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
        appUpdateManager.unregisterFlexibleUpdateListener()
        appUpdateManager.setOnFlexibleDownloadCompleteListener(null)
    }
}
