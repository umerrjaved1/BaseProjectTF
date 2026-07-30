package com.professor.baseproject.ui.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.professor.baseproject.R
import com.professor.baseproject.adapter.SurveyAdapter
import com.professor.baseproject.ads.AdsController
import com.professor.baseproject.ads.NativeAdOverlayFragment
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.databinding.ActivitySurveyBinding
import com.professor.baseproject.model.SurveyItem
import com.professor.baseproject.utils.StartupNavigationManager
import com.professor.baseproject.utils.setClickWithTimeout
import com.professor.baseproject.utils.startShakeAnimation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SurveyActivity : AppCompatActivity(), NativeAdOverlayFragment.Host {

    private lateinit var binding: ActivitySurveyBinding
    private lateinit var adapter: SurveyAdapter

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var adsController: AdsController

    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurveyBinding.inflate(layoutInflater)

        setContentView(binding.root)


        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_survey")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.SURVEY_SCR_VIEW)

        setupRecyclerView()
        setupButtons()
        binding.shimmerBtn.startShakeAnimation(this)

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
        // Placeholder feature list. Names come from strings.xml so they are localisable
        // and findable; previously they were hardcoded English literals naming a
        // specific app's features ("Live Earth Map", "Traffic Map", ...).
        val surveyItems = listOf(
            SurveyItem(1, getString(R.string.survey_feature_1), R.drawable.ic_live_earth_iap, "#E5F7ED"),
            SurveyItem(2, getString(R.string.survey_feature_2), R.drawable.ic_live_earth_iap, "#F0FAF7"),
            SurveyItem(3, getString(R.string.survey_feature_3), R.drawable.ic_live_earth_iap, "#FFF7E6"),
            SurveyItem(4, getString(R.string.survey_feature_4), R.drawable.ic_live_earth_iap, "#FFEDEE"),
            SurveyItem(5, getString(R.string.survey_feature_5), R.drawable.ic_live_earth_iap, "#F0F0FF")
        )

        adapter = SurveyAdapter(surveyItems) {
            updateSelectionState(surveyItems)
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.FEATURE_SELECTED)
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
            // Medium native (300x250 rectangle banner on a no-fill) on the CTA, then navigate.
            // Navigation hangs off onNativeAdOverlayDismissed rather than happening here, so the
            // ad is not torn down by the next screen the instant it appears. The overlay
            // self-skips when ads are off, so there is no branch to duplicate.
            NativeAdOverlayFragment.show(this, adsController)
        }
    }

    /** The interest-screen CTA ad is done (shown, skipped, or unfilled) — carry on to home. */
    override fun onNativeAdOverlayDismissed() {
        moveToMain()
    }

    private fun moveToMain() {
        if (hasNavigated) return
        hasNavigated = true

        appPreferences.setBoolean(AppPreferences.IS_SURVEY_DONE, true)

        StartupNavigationManager.navigateNext(
            activity = this,
            currentStep = StartupNavigationManager.Step.SURVEY,
            appPreferences = appPreferences
        )
    }
}
