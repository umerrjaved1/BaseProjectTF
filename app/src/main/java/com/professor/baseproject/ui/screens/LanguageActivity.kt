package com.professor.baseproject.ui.screens

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mzalogics.ads.domain.ads.native_ad.NativeAdBuilder
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.R
import com.professor.baseproject.adapter.LanguageAdapter
import com.professor.baseproject.app.AdIds
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.databinding.ActivityLanguageBinding
import com.professor.baseproject.databinding.DialogExitBinding
import com.professor.baseproject.enums.AdState
import com.professor.baseproject.model.LanguageModel
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.ui.viewmodel.LanguageViewModel
import com.professor.baseproject.utils.NavigationEvent
import com.professor.baseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageAdapter
    private val viewModel: LanguageViewModel by viewModels()

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var exitDialog: Dialog
    private var isFromStart = false
    private var nativeAdLoadAttempted = false

    private val TAG = "LanguageActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: LanguageActivity started")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)

        setupWindowInsets()
        initializeActivity()
        setupFlowCollectors()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Native ad state = ${viewModel.adState.value}")

        // Try to show ad if it's loaded but not showing
        if (viewModel.adState.value == AdState.LOADED) {
            showNativeAd()
        } else if (viewModel.adState.value == AdState.FAILED && !nativeAdLoadAttempted) {
            loadNativeAdWithRetry()
        }
    }

    private fun setupWindowInsets() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
    }

    private fun initializeActivity() {
        isFromStart = intent.getBooleanExtra(Constants.IS_FROM_START, false)
        Log.d(TAG, "isFromStart: $isFromStart")

        initAdapter()
        initClickListeners()
        loadExitDialog()

        // Preload onboarding ads immediately
        preloadOnboardingAds()

        // Handle native ad
        handleNativeAd()
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launchWhenStarted {
            // Collect UI State
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is com.professor.baseproject.utils.UIState.Loading -> {
                        Log.d(TAG, "Loading language data...")
                        // Show loading state if needed
                    }

                    is com.professor.baseproject.utils.UIState.Success -> {
                        Log.d(TAG, "Language data loaded successfully")
                        val uiState = state.data
                        adapter.submitList(uiState.displayList)
                        updateDoneButton(uiState.selectedLanguage)
                    }

                    is com.professor.baseproject.utils.UIState.Error -> {
                        Log.e(TAG, "Error loading language data: ${state.throwable.message}")
                        Toast.makeText(
                            this@LanguageActivity,
                            "Error loading languages",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            // Collect Selected Language
            viewModel.selectedLanguage.collectLatest { language ->
                language?.let {
                    updateDoneButton(it)
//                    binding.btnDone.isEnabled = true
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            // Collect Ad State
            viewModel.adState.collectLatest { state ->
                Log.d(TAG, "Ad state changed: $state")
                when (state) {
                    AdState.LOADING -> {
                        // Show loading state if needed
                    }

                    AdState.LOADED -> {
                        // Ad is loaded, will be shown in onResume or immediately
                        showNativeAd()
                    }

                    AdState.SHOWING -> {
                        analyticsManager.sendAnalytics("ad_show_success", "language_native")
                    }

                    AdState.FAILED -> {
                        showAdLoadingFailedUI()
                        analyticsManager.sendAnalytics("ad_show_failed", "language_native")
                    }

                    AdState.RETRYING -> {
                        Log.d(TAG, "Retrying ad load...")
                    }

                    else -> {
                        // Handle other states
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            // Collect Navigation Events
            viewModel.navigationEvent.collectLatest { event ->
                event?.let {
                    when (it) {
                        is NavigationEvent.Onboarding -> {
                            navigateToOnboarding()
                        }

                        is NavigationEvent.Main -> {
                            navigateToMain()
                        }

                        else -> {}
                    }
                    viewModel.clearNavigationEvent()
                }
            }
        }
    }

    private fun preloadOnboardingAds() {
        Log.d(TAG, "Preloading onboarding ads...")

        // Preload native ads for onboarding screen using loadAd (not loadAndShow)
        adMobManager.nativeAdLoader.loadAd(AdIds.getNativeOnboardingAdId()) { isLoaded, nativeAd ->
            if (isLoaded) {
                Log.d(TAG, "Onboarding native ad preloaded successfully")
                analyticsManager.sendAnalytics("ad_preload_success", "onboarding_native")
            } else {
                Log.w(TAG, "Failed to preload onboarding native ad")
                analyticsManager.sendAnalytics("ad_preload_failed", "onboarding_native")
            }
        }

        adMobManager.nativeAdLoader.loadAd(AdIds.getFullNativeOnboardingAdId()) { isLoaded, nativeAd ->
            if (isLoaded) {
                Log.d(TAG, "Full onboarding native ad preloaded successfully")
                analyticsManager.sendAnalytics("ad_preload_success", "full_onboarding_native")
            } else {
                Log.w(TAG, "Failed to preload full onboarding native ad")
                analyticsManager.sendAnalytics("ad_preload_failed", "full_onboarding_native")
            }
        }
    }

    private fun handleNativeAd() {
        Log.d(TAG, "Handling native ad, preloaded: ${adMobManager.nativeAdLoader.isAdLoaded()}")

        // Check if ad was preloaded from StartActivity
        if (adMobManager.nativeAdLoader.isAdLoaded() && isFromStart) {
            Log.d(TAG, "Showing preloaded native ad from StartActivity")
            analyticsManager.sendAnalytics("ad_show_preloaded", "language_native")
            viewModel.updateAdState(AdState.LOADED)
        } else {
            Log.d(TAG, "Loading native ad for LanguageActivity")
            loadNativeAdWithRetry()
        }
    }

    private fun loadNativeAdWithRetry(maxRetries: Int = 2) {
        if (nativeAdLoadAttempted) {
            Log.d(TAG, "Native ad load already attempted, skipping retry")
            return
        }

        nativeAdLoadAttempted = true
        viewModel.updateAdState(AdState.LOADING)

        Log.d(TAG, "Loading native ad for Language screen (attempt 1)")

        loadNativeAd { success ->
            if (success) {
                Log.d(TAG, "Native ad loaded successfully")
                viewModel.updateAdState(AdState.LOADED)
                analyticsManager.sendAnalytics("ad_load_success", "language_native_loaded")
            } else {
                Log.w(TAG, "Native ad failed to load on first attempt")
                viewModel.updateAdState(AdState.FAILED)
                analyticsManager.sendAnalytics("ad_load_failed", "language_native_first_attempt")

                // Retry after delay
                if (maxRetries > 0) {
                    binding.root.postDelayed({
                        retryNativeAdLoad(maxRetries - 1)
                    }, 1000L)
                }
            }
        }
    }

    private fun retryNativeAdLoad(retryCount: Int) {
        Log.d(TAG, "Retrying native ad load, attempts left: $retryCount")
        viewModel.updateAdState(AdState.RETRYING)

        loadNativeAd { success ->
            if (success) {
                Log.d(TAG, "Native ad loaded successfully on retry $retryCount")
                viewModel.updateAdState(AdState.LOADED)
                analyticsManager.sendAnalytics(
                    "ad_load_success",
                    "language_native_retry_$retryCount"
                )
            } else if (retryCount > 0) {
                Log.w(TAG, "Native ad failed on retry $retryCount")
                analyticsManager.sendAnalytics(
                    "ad_load_failed",
                    "language_native_retry_$retryCount"
                )

                binding.root.postDelayed({
                    if (retryCount == 1) {
                        loadFallbackNativeAd()
                    }
                }, 1500L)
            }
        }
    }

    private fun loadFallbackNativeAd() {
        Log.d(TAG, "Loading fallback native ad")

        loadNativeAd { success ->
            if (success) {
                Log.d(TAG, "Fallback native ad loaded successfully")
                viewModel.updateAdState(AdState.LOADED)
                analyticsManager.sendAnalytics("ad_load_success", "language_native_fallback")
            } else {
                Log.e(TAG, "All native ad loading attempts failed")
                viewModel.updateAdState(AdState.FAILED)
                analyticsManager.sendAnalytics("ad_load_failed", "language_native_all_attempts")
                showAdLoadingFailedUI()
            }
        }
    }

    private fun loadNativeAd(onResult: (Boolean) -> Unit) {
        val nativeAdBuilder = createNativeAdBuilder()

        // Use loadAndShow for immediate display
        adMobManager.nativeAdLoader.loadAndShow(
            AdIds.getNativeLanguageAdId(),
            nativeAdBuilder
        ) { success ->
            lifecycleScope.launchWhenStarted {
                onResult(success)
            }
        }
    }

    private fun showNativeAd() {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity finishing, skipping ad show")
            return
        }

        // If we have a preloaded ad, show it using showLoadedAd
        if (adMobManager.nativeAdLoader.isAdLoaded() && viewModel.adState.value != AdState.SHOWING) {
            Log.d(TAG, "Showing preloaded native ad")
            viewModel.updateAdState(AdState.SHOWING)

            // FIXED: Use the builder directly, don't call .build() again
            adMobManager.nativeAdLoader.showLoadedAd(
                createNativeAdBuilder(), // This is already a NativeAdBuilder object
                AdIds.getNativeLanguageAdId()
            )
        }
    }

    private fun createNativeAdBuilder(): NativeAdBuilder {
        return NativeAdBuilder.Builder(
            R.layout.native_ad_lang,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd
        ).setShowMedia(RemoteConfigManager.getLangNativeMedia())
            .setShowBody(true)
            .setIconEnabled(true)
            .setShowRating(false)
            .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
            .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
            .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
            .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
            .build()
        // Don't call .build() here - return the Builder directly
    }

    private fun showAdLoadingFailedUI() {
        binding.includeAd.adFrame.visibility = android.view.View.GONE
        binding.includeAd.shimmerFbAd.visibility = android.view.View.GONE
    }

    private fun loadExitDialog() {
        exitDialog = Dialog(this)
        val dialogBinding = DialogExitBinding.inflate(layoutInflater)
        exitDialog.setContentView(dialogBinding.root)
        exitDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layoutParams = dialogBinding.root.layoutParams
        val width = resources.displayMetrics.widthPixels
        layoutParams.width = (width * 0.9).toInt()
        dialogBinding.root.layoutParams = layoutParams

        exitDialog.setCancelable(true)
        dialogBinding.btnNo.setClickWithTimeout { exitDialog.dismiss() }
        dialogBinding.btnYes.setClickWithTimeout {
            exitDialog.dismiss()
            finishAffinity()
        }

        loadExitBannerAd(dialogBinding)
    }

    private fun loadExitBannerAd(dialogBinding: DialogExitBinding) {
        adMobManager.bannerAdLoader.showMemRecBanner(
            this,
            dialogBinding.includeAd.adFrame,
            dialogBinding.includeAd.shimmerFbAd,
            AdIds.getBannerAdIdExit()
        )
    }

    private fun initClickListeners() {
        binding.btnDone.setClickWithTimeout {
//            binding.btnDone.isEnabled = false

            analyticsManager.sendAnalytics(
                AnalyticsManager.Action.CLICKED, "btn_select_language_done"
            )

            if (!viewModel.isLanguageSelected()) {
                Toast.makeText(
                    this, getString(R.string.please_select_a_language), Toast.LENGTH_SHORT
                ).show()
//                binding.btnDone.isEnabled = true
                return@setClickWithTimeout
            }

            viewModel.selectedLanguage.value?.let { selectedLanguage ->
                analyticsManager.sendAnalytics(
                    "language_selected",
                    "${selectedLanguage.name} (${selectedLanguage.code})"
                )
            }

            viewModel.saveSelectedLanguage()
            viewModel.navigateToNextScreen()
        }
    }

    private fun initAdapter() {
        adapter = LanguageAdapter(null) { languageItem ->
            viewModel.selectLanguage(languageItem.model)
        }

        binding.rvLanguage.layoutManager = LinearLayoutManager(this)
        binding.rvLanguage.adapter = adapter
    }

    private fun updateDoneButton(selectedLang: LanguageModel) {
//        binding.btnDone.isEnabled = true
//        binding.btnDone.text = getString(
//            when (selectedLang.id) {
//                1 -> R.string.done_in_arabic
//                2 -> R.string.done_in_english
//                3 -> R.string.done_in_spanish
//                4 -> R.string.done_in_indonesian
//                6 -> R.string.done_in_persian
//                7 -> R.string.done_in_hindi
//                8 -> R.string.done_in_russian
//                9 -> R.string.done_in_portuguese
//                10 -> R.string.done_in_bengali
//                11 -> R.string.done_in_turkish
//                else -> R.string.done_in_english
//            }
//        )
    }

    private fun navigateToOnboarding() {
        analyticsManager.sendAnalytics("navigation", "language_to_onboarding")
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    private fun navigateToMain() {
        analyticsManager.sendAnalytics("navigation", "language_to_main")
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    override fun onBackPressed() {
        if (isFromStart) {
            exitDialog.show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Final ad state = ${viewModel.adState.value}")
        analyticsManager.sendAnalytics(
            "activity_destroyed",
            "language_ad_state_${viewModel.adState.value}"
        )
    }
}