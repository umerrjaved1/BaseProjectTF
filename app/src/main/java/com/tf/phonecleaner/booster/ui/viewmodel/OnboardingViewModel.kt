package com.tf.phonecleaner.booster.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.tf.phonecleaner.booster.app.AnalyticsManager
import com.tf.phonecleaner.booster.app.AppPreferences
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    val adMobManager: AdMobManager,
    val analyticsManager: AnalyticsManager
) : ViewModel() {

    fun markOnboardingComplete() {
        appPreferences.setBoolean(AppPreferences.Companion.IS_ONBOARDING, true)
    }
}
