package com.tf.gpsmapcamera.ui.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.adapter.SurveyAdapter
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.databinding.ActivitySurveyBinding
import com.tf.gpsmapcamera.model.SurveyItem
import com.tf.gpsmapcamera.utils.StartupNavigationManager
import com.tf.gpsmapcamera.utils.setClickWithTimeout
import com.tf.gpsmapcamera.utils.startShakeAnimation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SurveyActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySurveyBinding
    private lateinit var adapter: SurveyAdapter

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    private var hasNavigated = false

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
            moveToMain()
        }
    }

    private fun moveToMain() {
        if (hasNavigated) return
        hasNavigated = true

        appPreferences.setBoolean(AppPreferences.IS_ONBOARDING, true)

        StartupNavigationManager.navigateNext(
            activity = this,
            currentStep = StartupNavigationManager.Step.SURVEY,
            appPreferences = appPreferences
        )
    }
}
