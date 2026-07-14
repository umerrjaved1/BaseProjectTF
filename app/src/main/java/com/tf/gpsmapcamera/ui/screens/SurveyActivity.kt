package com.tf.gpsmapcamera.ui.screens

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.facebook.shimmer.ShimmerFrameLayout
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.adapter.SurveyAdapter
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.databinding.ActivitySurveyBinding
import com.tf.gpsmapcamera.model.SurveyItem
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.utils.AdUtils
import com.tf.gpsmapcamera.utils.setClickWithTimeout
import com.tf.gpsmapcamera.utils.startShakeAnimation
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SurveyActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySurveyBinding
    private lateinit var adapter: SurveyAdapter

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    private var hasNavigated = false
    private var hasRefreshedAd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurveyBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        window.statusBarColor = ContextCompat.getColor(this, R.color.bg_color)

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_survey")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.SURVEY_SCR_VIEW)

        setupRecyclerView()
        setupButtons()
        binding.shimmerBtn.startShakeAnimation(lifecycleScope)
        
        loadNativeAd()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun setupRecyclerView() {
        val surveyItems = listOf(
            SurveyItem(1, "Live Earth Map", R.drawable.ic_live_earth_iap, "#E5F7ED"),
            SurveyItem(2, "Famous Places", R.drawable.ic_fammous_places, "#F0FAF7"),
            SurveyItem(3, "Traffic Map", R.drawable.ic_live_earth_iap, "#FFF7E6"),
            SurveyItem(4, "Time Stamp", R.drawable.ic_fammous_places, "#FFEDEE"),
            SurveyItem(5, "Compass", R.drawable.ic_compass, "#F0F0FF")
        )

        adapter = SurveyAdapter(surveyItems) { clickedItem ->
            updateSelectionState(surveyItems)
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.FEATURE_SELECTED)
            
            // Force refresh ad once upon first selection
            if (!hasRefreshedAd) {
                hasRefreshedAd = true
                loadNativeAd(true)
            }
        }

        binding.rvSurveyTools.adapter = adapter
        updateSelectionState(surveyItems)
    }

    private fun updateSelectionState(items: List<SurveyItem>) {
        val selectedCount = items.count { it.isSelected }
        binding.tvSelectionCount.text = if (selectedCount <= 1) "$selectedCount item selected" else "$selectedCount items selected"
        
        if (selectedCount > 0) {
            binding.btnNext.isEnabled = true
            binding.btnNext.alpha = 1.0f
            binding.shimmerBtn.startShimmer()
        } else {
            binding.btnNext.isEnabled = false
            binding.btnNext.alpha = 0.5f
            binding.shimmerBtn.stopShimmer()
        }
    }

    private fun setupButtons() {
        binding.btnNext.setClickWithTimeout {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.SURVEY_SCR_DONE)
            moveToMain()
        }
    }



    private fun loadNativeAd(forceLoadNew: Boolean = true) {
        if (AdMobManager.isPremium) {
            binding.adContainerWrapper.visibility = android.view.View.GONE
            return
        }
        
        val config = RemoteConfigManager.getSurveyScreenConfig()
        val showNative1 = !hasRefreshedAd && config.showSurveyNative1
        val showNative2 = hasRefreshedAd && config.showSurveyNative2

        if (!showNative1 && !showNative2) {
            binding.adContainerWrapper.visibility = android.view.View.GONE
            return
        }

        binding.adContainerWrapper.visibility = android.view.View.VISIBLE
        binding.adContainerWrapper.removeAllViews()

        val adView = layoutInflater.inflate(R.layout.shimmer_layout_large_native, binding.adContainerWrapper, false)
        val adFrame = adView.findViewById<FrameLayout>(R.id.adFrame)
        val shimmerFbAd = adView.findViewById<ShimmerFrameLayout>(R.id.shimmerFbAd)

        val nativeConfig = config.nativeConfig
        val eventPrefix = if (hasRefreshedAd) "survey_scr_native2" else "survey_scr_native1"
        val adId = if (hasRefreshedAd) AdIds.getSurveyNative2AdId() else AdIds.getSurveyNative1AdId()
        
        AdUtils.loadAndShowNativeAd(
            adMobManager = adMobManager,
            adUnitId = adId,
            layoutResId = R.layout.native_ad_large,
            frameLayout = adFrame,
            shimmerFrameLayout = shimmerFbAd,
            showMedia = true,
            forceLoadNew = forceLoadNew,
            analyticsManager = analyticsManager,
            eventNamePrefix = eventPrefix
        ) {
            // Preload the second ad after the first ad finishes loading
            if (!hasRefreshedAd && config.showSurveyNative2) {
                adMobManager.nativeAdLoader.loadAd(AdIds.getSurveyNative2AdId())
            }
        }
        
        binding.adContainerWrapper.addView(adView)
    }

    private fun moveToMain() {
        if (hasNavigated) return
        hasNavigated = true

        appPreferences.setBoolean(AppPreferences.IS_ONBOARDING, true)

        com.tf.gpsmapcamera.utils.StartupNavigationManager.navigateNextWithAd(
            activity = this,
            currentStep = com.tf.gpsmapcamera.utils.StartupNavigationManager.Step.SURVEY,
            appPreferences = appPreferences,
            adMobManager = adMobManager,
            analyticsManager = analyticsManager
        )
    }
}
