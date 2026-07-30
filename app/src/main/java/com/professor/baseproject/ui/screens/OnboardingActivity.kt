package com.professor.baseproject.ui.screens

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.professor.baseproject.ui.base.FullscreenScreen
import com.professor.baseproject.R
import com.professor.baseproject.adapter.OnboardingAdapter
import com.professor.baseproject.ads.AdSlotStyle
import com.professor.baseproject.ads.AdsSlot
import com.professor.baseproject.ads.NativePlacement
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.databinding.ActivityOnboardingBinding
import com.professor.baseproject.model.OnboardingItem
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.utils.StartupNavigationManager
import com.professor.baseproject.utils.setClickWithTimeout
import com.professor.baseproject.utils.startShakeAnimation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity(), FullscreenScreen {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var adsSlot: AdsSlot

    /** Guards against double-navigation on rapid taps. */
    private var hasNavigated = false

    /**
     * Ad containers this screen has handed to [AdsSlot], so every banner-refresh timer can be
     * stopped in [onDestroy].
     *
     * Doubles as the load-once guard: `onBindViewHolder` can fire again for a slide that is
     * already showing an ad (a `notifyDataSetChanged`, a configuration change), and each of those
     * would otherwise be a fresh ad request.
     */
    private val adSlots = mutableSetOf<FrameLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)

        setContentView(binding.root)

        Log.e("TAG", "onCreate: onboarding", )


        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_onboarding")

        val config = RemoteConfigManager.getAdRules()
        val onboardingItems = mutableListOf<OnboardingItem>()

        // Slide presence and that slide's ad are separate remote flags: showObSlideN decides
        // whether the slide exists at all, showObNNative whether it carries an ad.
        if (config.showObSlide1) {
            onboardingItems.add(
                OnboardingItem(
                    title = getString(R.string.onboarding_title_1),
                    description = "",
                    imageRes = R.drawable.ob_1,
                    adEnabled = config.showOb1Native
                )
            )
        }
        if (config.showObSlide2) {
            onboardingItems.add(
                OnboardingItem(
                    title = getString(R.string.onboarding_title_2),
                    description = "",
                    imageRes = R.drawable.ob_2,
                    adEnabled = config.showOb2Native
                )
            )
        }
        if (config.showObSlide3) {
            onboardingItems.add(
                OnboardingItem(
                    title = getString(R.string.onboarding_title_3),
                    description = "",
                    imageRes = R.drawable.ob_3,
                    adEnabled = config.showOb3Native
                )
            )
        }

        if (onboardingItems.isEmpty()) {
            moveToMain()
            return
        }

        adapter = OnboardingAdapter(onboardingItems, onBindAdSlot = ::loadSlideAd)
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
        binding.shimmerBtn.startShakeAnimation(this)
    }

    /**
     * Small native shaped like a banner, per slide, with a banner fallback when native does not
     * fill. Uses the `ob_native` unit from `ad_ids`.
     *
     * `offscreenPageLimit` is the slide count, so all slides are alive at once and each requests
     * its ad once - three concurrent requests against one unit, which AdMob serves independently.
     */
    private fun loadSlideAd(container: FrameLayout) {
        if (!adSlots.add(container)) return
        adsSlot.show(
            activity = this,
            container = container,
            placement = NativePlacement.ONBOARDING,
            style = AdSlotStyle.SMALL_BANNER
        )
    }

    override fun onDestroy() {
        // Stops each slide's banner-refresh timer; without this they keep requesting after the
        // screen is gone.
        adSlots.forEach { adsSlot.release(it) }
        adSlots.clear()
        super.onDestroy()
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
            appPreferences = appPreferences,
            // Written here, after navigateNext has already resolved the next screen,
            // so marking onboarding done cannot swallow the survey step. Without this
            // write, nothing ever set the flag when the survey was config-skipped and
            // onboarding replayed on every launch.
            onBeforeNavigate = {
                appPreferences.setBoolean(AppPreferences.IS_ONBOARDING, true)
            }
        )
    }
}
