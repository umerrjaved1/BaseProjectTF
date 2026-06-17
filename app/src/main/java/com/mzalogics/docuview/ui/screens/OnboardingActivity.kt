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
                description = getString(R.string.onboarding_desc_1),
                imageRes = R.drawable.ob_1
            ),
            OnboardingItem(
                title = getString(R.string.onboarding_title_2),
                description = getString(R.string.onboarding_desc_2),
                imageRes = R.drawable.ob_2
            ),

            OnboardingItem(
                title = getString(R.string.onboarding_title_3),
                description = getString(R.string.onboarding_desc_3),
                imageRes = R.drawable.ob_3
            )
        )

        adapter = OnboardingAdapter(onboardingItems, adMobManager)
        binding.viewPager.adapter = adapter

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
        loadNativeAd()
    }


    private fun updateButtonText(position: Int, hasAdPage: Boolean) {
        val isLastPage = position == (adapter.itemCount - 1)
        val isAdPage = hasAdPage && position == 1

        binding.btnContinue.text =
            if (isLastPage) getString(R.string.get_started) else getString(R.string.continuee)

        binding.btnContinue.icon = if (isLastPage) null else ContextCompat.getDrawable(this, R.drawable.ic_small_arrow)

        binding.llIndicators.isVisible = true
        binding.btnSkip.isVisible = !isAdPage && !isLastPage
        binding.btnContinue.isVisible = !isAdPage
        binding.includeAd.adRoot.isVisible = isLastPage
    }

    private fun setupDotsIndicator(contentPageCount: Int, hasAdPage: Boolean, viewPager: ViewPager2) {
        val dotContainer = binding.dotContainer
        val dots = mutableListOf<TextView>()
        val totalPageCount = if (hasAdPage) contentPageCount + 1 else contentPageCount

        for (i in 0 until totalPageCount) {
            val dot = TextView(this).apply {
                text = "•"
                textSize = 32f
                setTextColor(
                    if (i == 0) getColor(R.color.onboarding_indicator_active)
                    else getColor(R.color.onboarding_indicator_inactive)
                )
                setPadding(2, 0, 2, 0)
            }
            dots.add(dot)
            dotContainer.addView(dot)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val dotIndex = position.coerceIn(0, dots.lastIndex)

                for (i in dots.indices) {
                    dots[i].setTextColor(
                        if (i == dotIndex) getColor(R.color.onboarding_indicator_active)
                        else getColor(R.color.onboarding_indicator_inactive)
                    )
                }
            }
        })
    }


    private fun loadNativeAd() {
        if (AdMobManager.isPremium || !RemoteConfigManager.shouldShowAds()) {
            binding.includeAd.adRoot.isVisible = false
            return
        }
        if (!AdFrequencyControl.canShowAd(this, AdUnitFrequencyController.UNIT_NATIVE)) {
            binding.includeAd.adRoot.isVisible = false
            return
        }
        // Safe null check — prevents IndexOutOfBoundsException if remote config returns empty list
        val nativeConfig = RemoteConfigManager.getAdsConfig().nativeConfig.getOrNull(0) ?: return

        val builder = NativeAdBuilder.Builder(
            R.layout.native_ad_onboarding,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd
        ).setShowBody(true)
            .setShowMedia(RemoteConfigManager.getOnBoardingNativeMedia())
            .setAdTitleColor(nativeConfig.heading)
            .setAdBodyColor(nativeConfig.description)
            .setCtaTextColor(nativeConfig.ctaText)
            .setCtaBgColor(nativeConfig.callActionButtonColor)
            .build()

        if (adMobManager.nativeAdLoader.isAdLoaded()) {
            adMobManager.nativeAdLoader.showLoadedAd(builder, AdIds.getNativeOnboardingAdId())
            AdFrequencyControl.recordAdShown(this, AdUnitFrequencyController.UNIT_NATIVE)
        } else {
            adMobManager.nativeAdLoader.loadAndShow(AdIds.getNativeOnboardingAdId(), builder) {
                AdFrequencyControl.recordAdShown(this@OnboardingActivity, AdUnitFrequencyController.UNIT_NATIVE)
            }
        }
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
