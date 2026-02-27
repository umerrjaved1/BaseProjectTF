package com.professor.baseproject.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.mzalogics.ads.domain.ads.native_ad.NativeAdBuilder
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.R
import com.professor.baseproject.adapter.OnboardingAdapter
import com.professor.baseproject.app.AdIds
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.databinding.ActivityOnboardingBinding
import com.professor.baseproject.enums.AdState
import com.professor.baseproject.model.OnboardingItem
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.ui.viewmodel.OnboardingViewModel
import com.professor.baseproject.utils.NavigationEvent
import com.professor.baseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var adapter: OnboardingAdapter? = null
    private val viewModel: OnboardingViewModel by viewModels()

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    private var nativeAdLoadAttempted = false
    private var isWaitingForInterstitial = false
    private val TAG = "OnboardingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)

        // Set language
        val localeList =
            LocaleListCompat.forLanguageTags(appPreferences.getString(AppPreferences.LANGUAGE_CODE))
        AppCompatDelegate.setApplicationLocales(localeList)

        setContentView(binding.root)

        setupWindowInsets()
        initializeActivity()
        setupFlowCollectors()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Native ad state = ${viewModel.adState.value}")

        // Handle case where we were waiting for interstitial
        if (isWaitingForInterstitial) {
            isWaitingForInterstitial = false
            navigateBasedOnStrategy()
        }

        // Try to show small native ad if it's loaded but not showing
        if (viewModel.adState.value == AdState.LOADED) {
            showSmallNativeAd()
        } else if (viewModel.adState.value == AdState.FAILED && !nativeAdLoadAttempted) {
            loadSmallNativeAdWithRetry()
        }
    }

    private fun setupWindowInsets() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
    }

    private fun initializeActivity() {
        val shouldShowAd = !RemoteConfigManager.getDisableAds()
        Log.d(TAG, "Initializing OnboardingActivity - Ads Enabled: $shouldShowAd")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_onboarding")
        setupViewPager()
        initClickListeners()
        loadSmallNativeAdWithRetry()
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launchWhenStarted {
            // Collect UI State
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is com.professor.baseproject.utils.UIState.Loading -> {
                        Log.d(TAG, "Loading onboarding data...")
                    }

                    is com.professor.baseproject.utils.UIState.Success -> {
                        Log.d(TAG, "Onboarding data loaded successfully")
                        val uiState = state.data
                        adapter?.submitList(uiState.onboardingItems)
                        setupDotsIndicator(uiState.totalPages)
                    }

                    is com.professor.baseproject.utils.UIState.Error -> {
                        Log.e(TAG, "Error loading onboarding data: ${state.throwable.message}")
                        setupDefaultOnboardingItems()
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            // Collect Current Page
            viewModel.currentPage.collectLatest { position ->
                updateButtonText(position)
                updateDotsIndicator(position)
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
                        showSmallNativeAd()
                    }

                    AdState.SHOWING -> {
                        analyticsManager.sendAnalytics("ad_show_success", "onboarding_small_native")
                    }

                    AdState.FAILED -> {
                        showSmallAdLoadingFailedUI()
                        analyticsManager.sendAnalytics("ad_show_failed", "onboarding_small_native")
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
                        is NavigationEvent.Main -> {
                            navigateToMain()
                        }

                        is NavigationEvent.Premium -> {
                            navigateToPremium(it.fromOnboarding)
                        }

                        is NavigationEvent.MainWithInterstitial -> {
                            showInterstitialAndNavigate()
                        }

                        else -> {}
                    }
                    viewModel.clearNavigationEvent()
                }
            }
        }
    }

    private fun setupViewPager() {
        adapter = OnboardingAdapter(emptyList(), adMobManager) { adLoaded ->
            Log.d(TAG, "Full native ad loaded in adapter: $adLoaded")
            if (adLoaded) {
                analyticsManager.sendAnalytics("ad_show_success", "full_native_onboarding")
            } else {
                analyticsManager.sendAnalytics("ad_show_failed", "full_native_onboarding")
            }
        }
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.updateCurrentPage(position)

                val totalItems = adapter?.itemCount ?: 0
                val isLastPage = position == totalItems - 1
                // Full-screen ad is fixed at position 1, but only shown when ads are enabled
                val shouldShowAd = !RemoteConfigManager.getDisableAds()
                val isAdPage = shouldShowAd && position == 1

                Log.d(TAG, "Page selected: $position, isLastPage: $isLastPage, isAdPage: $isAdPage")

                // Update UI based on current page
                updateUIForPage(position, isAdPage)
            }
        })
    }

    private fun setupDefaultOnboardingItems() {
        val defaultItems = listOf(
            OnboardingItem(
                title = getString(R.string.onboarding_title_1),
                description = getString(R.string.onboarding_desc_1),
                imageRes = R.drawable.onboarding_1
            ),
            OnboardingItem(
                title = getString(R.string.onboarding_title_2),
                description = getString(R.string.onboarding_desc_2),
                imageRes = R.drawable.onboarding_2
            ),

            OnboardingItem(
                title = getString(R.string.onboarding_title_3),
                description = getString(R.string.onboarding_desc_3),
                imageRes = R.drawable.onboarding_3
            )
        )
        adapter?.submitList(defaultItems)
        setupDotsIndicator(defaultItems.size)
    }

    private fun initClickListeners() {
        binding.btnContinue.setClickWithTimeout {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, "btn_onboarding_next")

            val currentPosition = binding.viewPager.currentItem
            val totalItems = adapter?.itemCount ?: 0
            val isLastPage = currentPosition == totalItems - 1

            if (isLastPage) {
                // If we're on the last page (ad page), complete onboarding
                completeOnboarding()
            } else {
                // Move to next page
                binding.viewPager.currentItem = currentPosition + 1
            }
        }

        // Optional: Add skip button for better UX
        binding.btnSkip.setClickWithTimeout {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, "btn_onboarding_skip")
            completeOnboarding()
        }
    }

    private fun updateButtonText(position: Int) {
        val totalItems = adapter?.itemCount ?: 0
        val isLastPage = position == totalItems - 1

        binding.btnContinue.text = if (isLastPage) {
            getString(R.string.get_started)
        } else {
            getString(R.string.continuee)
        }

        // Show skip button on all pages except the last one
        binding.btnSkip.isVisible = !isLastPage
    }

    private fun updateUIForPage(position: Int, isAdPage: Boolean) {
        // Show/hide appropriate UI elements
        binding.llIndicators.isVisible = !isAdPage

        // Show bottom small native ad only on last page
        binding.includeAd.root.isVisible =
            isAdPage.not() && (position == (adapter?.itemCount ?: 1) - 1)

        // On ad page, you might want different behavior
        if (isAdPage) {
            setupAdPageUI()
        }
    }

    private fun setupAdPageUI() {
        // Optional: Add specific UI for ad page
        Log.d(TAG, "User reached ad page")
    }

    private fun setupDotsIndicator(pageCount: Int) {
        val dotContainer = binding.dotContainer
        dotContainer.removeAllViews()

        // Dots represent content pages only (ad page at position 1 doesn't get a dot)
        // pageCount already reflects the number of onboarding items (not including ad)
        val contentPages = pageCount
        for (i in 0 until contentPages) {
            val dot = TextView(this).apply {
                text = "•"
                textSize = 32f
                setTextColor(
                    if (i == 0) getColor(R.color.primary_color) else getColor(R.color.dot_color)
                )
                setPadding(8, 0, 8, 0)
            }
            dotContainer.addView(dot)
        }
    }

    private fun updateDotsIndicator(position: Int) {
        val dotContainer = binding.dotContainer
        val shouldShowAd = !RemoteConfigManager.getDisableAds()
        // Map actual position to content dot index (skip ad page at position 1 if ads are shown)
        val dotIndex = when {
            position <= 0 -> 0
            shouldShowAd && position == 1 -> 1 // keep highlight stable when on ad page, highlight second dot
            shouldShowAd && position > 1 -> position - 1
            else -> position
        }
        for (i in 0 until dotContainer.childCount) {
            val dot = dotContainer.getChildAt(i) as TextView
            dot.setTextColor(
                if (i == dotIndex) getColor(R.color.primary_color) else getColor(R.color.dot_color)
            )
        }
    }

    // Small Native Ad (shown at bottom of last onboarding page)
    private fun loadSmallNativeAdWithRetry(maxRetries: Int = 2) {
        if (nativeAdLoadAttempted) {
            Log.d(TAG, "Small native ad load already attempted, skipping retry")
            return
        }

        nativeAdLoadAttempted = true
        viewModel.updateAdState(AdState.LOADING)

        Log.d(TAG, "Loading small native ad for Onboarding screen (attempt 1)")

        loadSmallNativeAd { success ->
            if (success) {
                Log.d(TAG, "Small native ad loaded successfully")
                viewModel.updateAdState(AdState.LOADED)
                analyticsManager.sendAnalytics("ad_load_success", "onboarding_small_native_loaded")
            } else {
                Log.w(TAG, "Small native ad failed to load on first attempt")
                viewModel.updateAdState(AdState.FAILED)
                analyticsManager.sendAnalytics(
                    "ad_load_failed",
                    "onboarding_small_native_first_attempt"
                )

                // Retry after delay
                if (maxRetries > 0) {
                    binding.root.postDelayed({
                        retrySmallNativeAdLoad(maxRetries - 1)
                    }, 1000L)
                }
            }
        }
    }

    private fun retrySmallNativeAdLoad(retryCount: Int) {
        Log.d(TAG, "Retrying small native ad load, attempts left: $retryCount")
        viewModel.updateAdState(AdState.RETRYING)

        loadSmallNativeAd { success ->
            if (success) {
                Log.d(TAG, "Small native ad loaded successfully on retry $retryCount")
                viewModel.updateAdState(AdState.LOADED)
                analyticsManager.sendAnalytics(
                    "ad_load_success",
                    "onboarding_small_native_retry_$retryCount"
                )
            } else if (retryCount > 0) {
                Log.w(TAG, "Small native ad failed on retry $retryCount")
                analyticsManager.sendAnalytics(
                    "ad_load_failed",
                    "onboarding_small_native_retry_$retryCount"
                )
            } else {
                Log.e(TAG, "All small native ad loading attempts failed")
                viewModel.updateAdState(AdState.FAILED)
                analyticsManager.sendAnalytics(
                    "ad_load_failed",
                    "onboarding_small_native_all_attempts"
                )
                showSmallAdLoadingFailedUI()
            }
        }
    }

    private fun loadSmallNativeAd(onResult: (Boolean) -> Unit) {
        val nativeAdBuilder = createSmallNativeAdBuilder()

        // Check if ad was preloaded from LanguageActivity
        if (adMobManager.nativeAdLoader.isAdLoaded()) {
            Log.d(TAG, "Using preloaded small native ad from LanguageActivity")
            onResult(true)
        } else {
            // Load fresh ad
            adMobManager.nativeAdLoader.loadAndShow(
                AdIds.getNativeOnboardingAdId(),
                nativeAdBuilder
            ) { success ->
                lifecycleScope.launchWhenStarted {
                    onResult(success)
                }
            }
        }
    }

    private fun showSmallNativeAd() {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity finishing, skipping small native ad show")
            return
        }

        if (viewModel.adState.value != AdState.SHOWING) {
            Log.d(TAG, "Showing small native ad")
            viewModel.updateAdState(AdState.SHOWING)

            try {
                adMobManager.nativeAdLoader.showLoadedAd(
                    createSmallNativeAdBuilder(),
                    AdIds.getNativeOnboardingAdId()
                )
                Log.d(TAG, "Small native ad shown successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show small native ad: ${e.message}")
                viewModel.updateAdState(AdState.FAILED)
            }
        }
    }

    private fun createSmallNativeAdBuilder(): NativeAdBuilder {
        return NativeAdBuilder.Builder(
            R.layout.native_ad_onboarding,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd
        ).setShowBody(true)
            .setShowMedia(RemoteConfigManager.getOnBoardingNativeMedia())
            .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
            .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
            .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
            .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
            .build()
    }

    private fun showSmallAdLoadingFailedUI() {
        binding.includeAd.adFrame.visibility = android.view.View.GONE
        binding.includeAd.shimmerFbAd.visibility = android.view.View.GONE
    }

    private fun completeOnboarding() {
        Log.d(TAG, "Completing onboarding")
        appPreferences.setBoolean(AppPreferences.IS_ONBOARDING, true)
        handleNavigationStrategy()
    }

    private fun handleNavigationStrategy() {
        val strategy = RemoteConfigManager.getAdsConfig().onBoardingMonetizationStrategy
        Log.d(TAG, "Navigation strategy: $strategy")

        when (strategy) {
            0 -> navigateToMain()
            1 -> navigateToPremium(true)
            2 -> showInterstitialAndNavigate()
            else -> navigateToPremium(true)
        }
    }

    private fun navigateToMain() {
        analyticsManager.sendAnalytics("navigation", "onboarding_to_main")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToPremium(fromOnboarding: Boolean) {
        analyticsManager.sendAnalytics("navigation", "onboarding_to_premium")
        val intent = Intent(this, PremiumActivity::class.java).apply {
            if (fromOnboarding) {
                putExtra(Constants.FROM_ONBOARDING, true)
            } else {
                putExtra(Constants.IS_FROM_START_TO_PREMIUM, true)
            }
        }
        startActivity(intent)
        finish()
    }

    private fun showInterstitialAndNavigate() {
        analyticsManager.sendAnalytics("navigation", "onboarding_with_interstitial")

        adMobManager.interstitialAdLoader.loadAd(AdIds.getInterstitialAdID()) { isLoaded ->
            if (isLoaded) {
                adMobManager.interstitialAdLoader.showAd(
                    this,
                    AdIds.getInterstitialAdID()
                ) {
                    Log.d(TAG, "Interstitial ad dismissed, navigating to main")
                    navigateToMain()
                }
            } else {
                Log.w(TAG, "Interstitial ad not loaded, navigating directly to main")
                navigateToMain()
            }
        }
    }

    private fun navigateBasedOnStrategy() {
        // Fallback navigation method
        val strategy = RemoteConfigManager.getAdsConfig().onBoardingMonetizationStrategy
        when (strategy) {
            0, 2 -> navigateToMain()
            1 -> navigateToPremium(true)
            else -> navigateToMain()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Final ad state = ${viewModel.adState.value}")
        analyticsManager.sendAnalytics(
            "activity_destroyed",
            "onboarding_ad_state_${viewModel.adState.value}"
        )
        adapter?.destroy()
        adapter = null
    }
}