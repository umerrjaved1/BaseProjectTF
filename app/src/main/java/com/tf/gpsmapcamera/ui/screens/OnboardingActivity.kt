package com.tf.gpsmapcamera.ui.screens

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.adapter.OnboardingAdapter
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.databinding.ActivityOnboardingBinding
import com.tf.gpsmapcamera.model.OnboardingItem
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.utils.StartupNavigationManager
import com.tf.gpsmapcamera.utils.setClickWithTimeout
import com.tf.gpsmapcamera.utils.startShakeAnimation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    @Inject
    lateinit var analyticsManager: AnalyticsManager

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

        adapter = OnboardingAdapter(onboardingItems)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = onboardingItems.size

        binding.viewPager.layoutDirection = resources.configuration.layoutDirection
        binding.dotContainer.layoutDirection = resources.configuration.layoutDirection
        binding.llIndicators.layoutDirection = resources.configuration.layoutDirection

        setupDotsIndicator(onboardingItems.size, binding.viewPager)

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
                updateButtonText(position)
            }
        })

        updateButtonText(binding.viewPager.currentItem)
        binding.shimmerBtn.startShakeAnimation(lifecycleScope)
    }

    private fun updateButtonText(position: Int) {
        val isLastPage = position == (adapter.itemCount - 1)

        val viewEventName = "ob${position + 1}_view"
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, viewEventName)

        binding.btnContinue.text =
            if (isLastPage) getString(R.string.get_started) else getString(R.string.continuee)

        binding.llIndicators.isVisible = true
        binding.btnSkip.isVisible = false
        binding.shimmerBtn.isVisible = true
    }

    private fun setupDotsIndicator(pageCount: Int, viewPager: ViewPager2) {
        val dotContainer = binding.dotContainer
        dotContainer.removeAllViews()
        val dots = mutableListOf<android.view.View>()

        val density = resources.displayMetrics.density
        fun dpToPx(dp: Int): Int = (dp * density).toInt()

        for (i in 0 until pageCount) {
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

        StartupNavigationManager.navigateNext(
            activity = this,
            currentStep = StartupNavigationManager.Step.ONBOARDING,
            appPreferences = appPreferences
        )
    }
}
