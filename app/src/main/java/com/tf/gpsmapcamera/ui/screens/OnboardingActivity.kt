package com.tf.gpsmapcamera.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.facebook.shimmer.ShimmerFrameLayout
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.adapter.OnboardingAdapter
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.constants.Constants
import com.tf.gpsmapcamera.databinding.ActivityOnboardingBinding
import com.tf.gpsmapcamera.model.OnboardingItem
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.utils.AdFrequencyControl
import com.tf.gpsmapcamera.utils.AdUnitFrequencyController
import com.tf.gpsmapcamera.utils.AdUtils
import com.tf.gpsmapcamera.utils.setClickWithTimeout
import com.tf.gpsmapcamera.utils.startShakeAnimation
import com.umer_tf.ads.domain.ads.native_ad.NativeAd
import com.umer_tf.ads.domain.ads.native_ad.NativeAdBuilder
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    private val cachedNativeAds = arrayOfNulls<NativeAd>(10)
    private val cachedAdViews = arrayOfNulls<android.view.View>(10)

    @Inject
    lateinit var appPreferences: AppPreferences

    /** Guards against double-navigation on rapid taps. */
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)

        setContentView(binding.root)

        Log.e("TAG", "onCreate: onboarding", )

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars =
            true // Light icons = false, Dark icons = true
        window.statusBarColor = ContextCompat.getColor(this, R.color.bg_color)

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_onboarding")

        val config = RemoteConfigManager.getOnboardingScreenConfig()
        val onboardingItems = mutableListOf<OnboardingItem>()
        
        if (config.showObSlide1) {
            onboardingItems.add(
                OnboardingItem(
                    title = getString(R.string.onboarding_title_1),
                    description = "",
                    imageRes = R.drawable.ob_1
                )
            )
        }
        if (config.showObSlide2) {
            onboardingItems.add(
                OnboardingItem(
                    title = getString(R.string.onboarding_title_2),
                    description = "",
                    imageRes = R.drawable.ob_2
                )
            )
        }
        if (config.showObSlide3) {
            onboardingItems.add(
                OnboardingItem(
                    title = getString(R.string.onboarding_title_3),
                    description = "",
                    imageRes = R.drawable.ob_3
                )
            )
        }

        if (onboardingItems.isEmpty()) {
            moveToMain()
            return
        }

        adapter = OnboardingAdapter(onboardingItems, adMobManager)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = onboardingItems.size

        val hasAdPage = adapter.itemCount > onboardingItems.size
        
        binding.viewPager.layoutDirection = resources.configuration.layoutDirection
        binding.dotContainer.layoutDirection = resources.configuration.layoutDirection
        binding.llIndicators.layoutDirection = resources.configuration.layoutDirection
        
        setupDotsIndicator(onboardingItems.size, hasAdPage, binding.viewPager)

        binding.btnContinue.setClickWithTimeout {
            if (binding.viewPager.currentItem < adapter.itemCount - 1) {
                binding.viewPager.currentItem += 1
            } else {
                analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.OB4_GET_STARTED)
                moveToMain()
            }
        }

        binding.btnSkip.setClickWithTimeout {
            moveToMain()
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtonText(position, hasAdPage)
            }
        })

        updateButtonText(binding.viewPager.currentItem, hasAdPage)
        binding.shimmerBtn.startShakeAnimation(lifecycleScope)
    }

    private fun updateButtonText(position: Int, hasAdPage: Boolean) {
        val isLastPage = position == (adapter.itemCount - 1)
        val config = RemoteConfigManager.getOnboardingScreenConfig()
        val isAdPage = hasAdPage && position == config.fullNativeAdPosition

        val viewEventName = "ob${position + 1}_view"
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, viewEventName)

        binding.btnContinue.text =
            if (isLastPage) getString(R.string.get_started) else getString(R.string.continuee)

        binding.llIndicators.isVisible = !isAdPage
        binding.btnSkip.isVisible = false
        binding.shimmerBtn.isVisible = !isAdPage
        
        handleSmallNativeAd(position, hasAdPage)
    }

    private fun handleSmallNativeAd(position: Int, hasAdPage: Boolean) {
        val config = RemoteConfigManager.getOnboardingScreenConfig()
        val fullAdPos = config.fullNativeAdPosition
        val isFullAdPage = hasAdPage && position == fullAdPos
        
        val itemIndex = if (hasAdPage) {
            if (position < fullAdPos) position else if (position > fullAdPos) position - 1 else -1
        } else {
            position
        }

        val shouldShowAdThisSlide = if (isFullAdPage) false else when (itemIndex) {
            0 -> config.showOb1Native
            1 -> config.showOb2Native
            2 -> config.showOb3Native
            else -> false
        }
        
        if (isFullAdPage || !shouldShowAdThisSlide || AdMobManager.isPremium || 
            !AdFrequencyControl.canShowAd(this, AdUnitFrequencyController.UNIT_NATIVE)) {
            binding.adContainerWrapper.visibility = android.view.View.GONE
            return
        }

        binding.adContainerWrapper.visibility = android.view.View.VISIBLE
        binding.adContainerWrapper.removeAllViews() // Detach previous slide's ad view
        
        var adView = cachedAdViews[position]
        if (adView == null) {
            // Inflate new container for this slide
            adView = layoutInflater.inflate(R.layout.shimmer_layout_large_native, binding.adContainerWrapper, false)
            cachedAdViews[position] = adView
            
            val adFrame = adView.findViewById<FrameLayout>(R.id.adFrame)
            val shimmerFbAd = adView.findViewById<ShimmerFrameLayout>(R.id.shimmerFbAd)
            
            val loader = NativeAd(this)
            cachedNativeAds[position] = loader
            
            val builder = NativeAdBuilder.Builder(
                R.layout.native_ad_large,
                adFrame,
                shimmerFbAd
            ).setShowMedia(true).setShowBody(true).setShowRating(false).setIconEnabled(true)
            
            // Apply colors
            val nativeConfig = RemoteConfigManager.getOnboardingScreenConfig().nativeConfig
            nativeConfig.let {
                builder.setAdTitleColor(it.heading)
                builder.setAdBodyColor(it.description)
                builder.setCtaTextColor(it.ctaText)
                builder.setCtaBgColor(it.callActionButtonColor)
            }
                
            val eventPrefix = "ob${position + 1}_native"
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${eventPrefix}_request")
            loader.loadAndShow(AdIds.getNativeOnboardingAdId(), builder.build()) { success ->
                if (success) {
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${eventPrefix}_pass")
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${eventPrefix}_view")
                } else {
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${eventPrefix}_fail")
                }
            }
        }
        
        // Attach the cached view for this slide
        binding.adContainerWrapper.addView(adView)
        
        // PRELOAD the next ad so it's ready instantly when user swipes!
        preloadSmallNativeAd(position + 1, hasAdPage)
    }

    private fun preloadSmallNativeAd(position: Int, hasAdPage: Boolean) {
        val maxItems = binding.viewPager.adapter?.itemCount ?: 0
        if (position >= maxItems) return

        val config = RemoteConfigManager.getOnboardingScreenConfig()
        val fullAdPos = config.fullNativeAdPosition
        val isFullAdPage = hasAdPage && position == fullAdPos
        
        val itemIndex = if (hasAdPage) {
            if (position < fullAdPos) position else if (position > fullAdPos) position - 1 else -1
        } else {
            position
        }

        val shouldShowAdThisSlide = if (isFullAdPage) false else when (itemIndex) {
            0 -> config.showOb1Native
            1 -> config.showOb2Native
            2 -> config.showOb3Native
            else -> false
        }
        
        if (isFullAdPage || !shouldShowAdThisSlide || AdMobManager.isPremium || 
            !AdFrequencyControl.canShowAd(this, AdUnitFrequencyController.UNIT_NATIVE)) {
            return
        }

        var adView = cachedAdViews[position]
        if (adView == null) {
            // Inflate new container for this slide in the background
            adView = layoutInflater.inflate(R.layout.shimmer_layout_large_native, binding.adContainerWrapper, false)
            cachedAdViews[position] = adView
            
            val adFrame = adView.findViewById<FrameLayout>(R.id.adFrame)
            val shimmerFbAd = adView.findViewById<ShimmerFrameLayout>(R.id.shimmerFbAd)
            
            val loader = NativeAd(this)
            cachedNativeAds[position] = loader
            
            val builder = NativeAdBuilder.Builder(
                R.layout.native_ad_large,
                adFrame,
                shimmerFbAd
            ).setShowMedia(true).setShowBody(true).setShowRating(false).setIconEnabled(true)
            
            // Apply colors
            val nativeConfig = RemoteConfigManager.getOnboardingScreenConfig().nativeConfig
            nativeConfig.let {
                builder.setAdTitleColor(it.heading)
                builder.setAdBodyColor(it.description)
                builder.setCtaTextColor(it.ctaText)
                builder.setCtaBgColor(it.callActionButtonColor)
            }
                
            val eventPrefix = "ob${position + 1}_native"
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${eventPrefix}_request")
            loader.loadAndShow(AdIds.getNativeOnboardingAdId(), builder.build()) { success ->
                if (success) {
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${eventPrefix}_pass")
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${eventPrefix}_view")
                } else {
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, "${eventPrefix}_fail")
                }
            }
        }
    }

    private fun setupDotsIndicator(contentPageCount: Int, hasAdPage: Boolean, viewPager: ViewPager2) {
        val dotContainer = binding.dotContainer
        dotContainer.removeAllViews()
        val dots = mutableListOf<android.view.View>()

        val density = resources.displayMetrics.density
        fun dpToPx(dp: Int): Int = (dp * density).toInt()

        val totalPageCount = if (hasAdPage) contentPageCount + 1 else contentPageCount

        for (i in 0 until totalPageCount) {
            val dot = android.view.View(this).apply {
                val params = android.widget.LinearLayout.LayoutParams(
                    if (i == 0) dpToPx(16) else dpToPx(6),
                    dpToPx(6)
                ).apply {
                    setMargins(dpToPx(4), 0, dpToPx(4), 0)
                }
                layoutParams = params
                setBackgroundResource(
                    if (i == 0) R.drawable.dot_active
                    else R.drawable.dot_inactive
                )
            }
            dots.add(dot)
            dotContainer.addView(dot)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val dotIndex = position.coerceIn(0, dots.lastIndex)

                for (i in dots.indices) {
                    val params = dots[i].layoutParams as android.widget.LinearLayout.LayoutParams
                    if (i == dotIndex) {
                        params.width = dpToPx(16)
                        dots[i].setBackgroundResource(R.drawable.dot_active)
                    } else {
                        params.width = dpToPx(6)
                        dots[i].setBackgroundResource(R.drawable.dot_inactive)
                    }
                    dots[i].layoutParams = params
                }
            }
        })
    }





    private fun moveToMain() {
        // Guard against double-navigation (rapid taps on Skip + Continue)
        if (hasNavigated) return
        hasNavigated = true

        com.tf.gpsmapcamera.utils.StartupNavigationManager.navigateNextWithAd(
            activity = this,
            currentStep = com.tf.gpsmapcamera.utils.StartupNavigationManager.Step.ONBOARDING,
            appPreferences = appPreferences,
            adMobManager = adMobManager,
            analyticsManager = analyticsManager
        )
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
