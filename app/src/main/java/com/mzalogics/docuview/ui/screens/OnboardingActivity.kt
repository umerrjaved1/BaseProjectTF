package com.mzalogics.docuview.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import android.widget.FrameLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.umer_tf.ads.domain.ads.native_ad.NativeAd
import com.umer_tf.ads.domain.ads.native_ad.NativeAdBuilder
import com.umer_tf.ads.domain.core.AdMobManager
import com.mzalogics.docuview.adapter.OnboardingAdapter
import com.mzalogics.docuview.app.AdIds
import com.mzalogics.docuview.app.AnalyticsManager
import com.mzalogics.docuview.app.AppPreferences
import com.mzalogics.docuview.constants.Constants
import com.mzalogics.docuview.model.OnboardingItem
import com.mzalogics.docuview.remoteconfig.RemoteConfigManager
import com.mzalogics.docuview.utils.AdFrequencyControl
import com.mzalogics.docuview.utils.AdUnitFrequencyController
import com.mzalogics.docuview.utils.setClickWithTimeout
import com.mzalogics.docuview.R
import com.mzalogics.docuview.databinding.ActivityOnboardingBinding
import com.mzalogics.docuview.utils.AdUtils
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

        val localeList =
            LocaleListCompat.forLanguageTags(appPreferences.getString(AppPreferences.Companion.LANGUAGE_CODE))
        AppCompatDelegate.setApplicationLocales(localeList)
        setContentView(binding.root)

        Log.e("TAG", "onCreate: onboarding", )

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars =
            true // Light icons = false, Dark icons = true
        window.statusBarColor = ContextCompat.getColor(this, R.color.bg_color)

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_onboarding")

        val onboardingItems = listOf(
            OnboardingItem(
                title = getString(R.string.onboarding_title_1),
                description = "",
                imageRes = R.drawable.ob_1
            ),
            OnboardingItem(
                title = getString(R.string.onboarding_title_2),
                description = "",
                imageRes = R.drawable.ob_2
            ),
            OnboardingItem(
                title = getString(R.string.onboarding_title_3),
                description = "",
                imageRes = R.drawable.ob_3
            ),
            OnboardingItem(
                title = getString(R.string.onboarding_title_4),
                description = "",
                imageRes = R.drawable.ob_4
            )
        )

        adapter = OnboardingAdapter(onboardingItems, adMobManager)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = onboardingItems.size

        val hasAdPage = adapter.itemCount > onboardingItems.size
        setupDotsIndicator(onboardingItems.size, hasAdPage, binding.viewPager)

        binding.btnContinue.setClickWithTimeout {
            if (binding.viewPager.currentItem < adapter.itemCount - 1) {
                binding.viewPager.currentItem += 1
            } else {
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
    }


    private fun updateButtonText(position: Int, hasAdPage: Boolean) {
        val isLastPage = position == (adapter.itemCount - 1)
        val isAdPage = hasAdPage && position == 1

        binding.btnContinue.text =
            if (isLastPage) getString(R.string.get_started) else getString(R.string.continuee)

        binding.llIndicators.isVisible = true
        binding.btnSkip.isVisible = false
        binding.btnContinue.isVisible = !isAdPage
        
        handleSmallNativeAd(position, hasAdPage)
    }

    private fun handleSmallNativeAd(position: Int, hasAdPage: Boolean) {
        val isFullAdPage = hasAdPage && position == 1
        val shouldShowAd = RemoteConfigManager.shouldShowAds() && !AdMobManager.isPremium
        val disableSlides = RemoteConfigManager.getAdsConfig().disableSmallAdSlides
        
        if (isFullAdPage || !shouldShowAd || disableSlides.contains(position) || 
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
            val nativeConfig = RemoteConfigManager.getAdsConfig().nativeConfig.getOrNull(0)
            nativeConfig?.let {
                builder.setAdTitleColor(it.heading)
                builder.setAdBodyColor(it.description)
                builder.setCtaTextColor(it.ctaText)
                builder.setCtaBgColor(it.callActionButtonColor)
            }
                
            loader.loadAndShow(AdIds.getNativeOnboardingAdId(), builder.build(), null)
        }
        
        // Attach the cached view for this slide
        binding.adContainerWrapper.addView(adView)
        
        // PRELOAD the next ad so it's ready instantly when user swipes!
        preloadSmallNativeAd(position + 1, hasAdPage)
    }

    private fun preloadSmallNativeAd(position: Int, hasAdPage: Boolean) {
        val maxItems = binding.viewPager.adapter?.itemCount ?: 0
        if (position >= maxItems) return

        val isFullAdPage = hasAdPage && position == 1
        val shouldShowAd = RemoteConfigManager.shouldShowAds() && !AdMobManager.isPremium
        val disableSlides = RemoteConfigManager.getAdsConfig().disableSmallAdSlides
        
        if (isFullAdPage || !shouldShowAd || disableSlides.contains(position) || 
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
            val nativeConfig = RemoteConfigManager.getAdsConfig().nativeConfig.getOrNull(0)
            nativeConfig?.let {
                builder.setAdTitleColor(it.heading)
                builder.setAdBodyColor(it.description)
                builder.setCtaTextColor(it.ctaText)
                builder.setCtaBgColor(it.callActionButtonColor)
            }
                
            loader.loadAndShow(AdIds.getNativeOnboardingAdId(), builder.build(), null)
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

        appPreferences.setBoolean(AppPreferences.Companion.IS_ONBOARDING, true)

        when (RemoteConfigManager.getAdsConfig().onBoardingMonetizationStrategy) {
            0 -> {
                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                finish()
            }

            1 -> {
                startActivity(
                    Intent(this@OnboardingActivity, PremiumActivity::class.java)
                        .putExtra(Constants.EXTRA_PREMIUM_FROM_ONBOARDING, true)
                )
                finish()
            }

            2 -> {
                if (AdMobManager.isPremium || !RemoteConfigManager.shouldShowAds()) {
                    startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                    finish()
                    return
                }
                // FIX: do NOT call finish() here; let the ad callback handle it.
                // Calling finish() before showAd() destroys the window → crash.
              /*  adMobManager.interstitialAdLoader.showAd(
                    this,
                    AdIds.getInterstitialAdID()
                ) {
                    if (!isFinishing && !isDestroyed) {
                        startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                        finish()
                    }
                }*/
            }

            else -> {
                startActivity(
                    Intent(this@OnboardingActivity, PremiumActivity::class.java)
                        .putExtra(Constants.EXTRA_PREMIUM_FROM_ONBOARDING, true)
                )
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
