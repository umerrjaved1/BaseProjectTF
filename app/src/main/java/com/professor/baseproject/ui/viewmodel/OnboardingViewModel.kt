package com.professor.baseproject.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.professor.baseproject.R
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.enums.AdState
import com.professor.baseproject.model.OnboardingItem
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.utils.NavigationEvent
import com.professor.baseproject.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<OnboardingUiState>>(UIState.Loading)
    val uiState: StateFlow<UIState<OnboardingUiState>> = _uiState.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _adState = MutableStateFlow(AdState.NOT_LOADED)
    val adState: StateFlow<AdState> = _adState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadOnboardingData()
    }

    private fun loadOnboardingData() {
        _uiState.value = UIState.Loading
        viewModelScope.launch {
            try {
                val onboardingItems = getOnboardingItems()
                val uiState = OnboardingUiState(
                    onboardingItems = onboardingItems,
                    totalPages = onboardingItems.size
                )
                _uiState.value = UIState.Success(uiState)
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e)
            }
        }
    }

    fun updateCurrentPage(position: Int) {
        _currentPage.value = position
    }

    fun navigateToNextPage(totalPages: Int) {
        if (_currentPage.value < totalPages - 1) {
            _currentPage.value = _currentPage.value + 1
        } else {
            completeOnboarding()
        }
    }

    fun updateAdState(state: AdState) {
        _adState.value = state
    }

    private fun completeOnboarding() {
        appPreferences.setBoolean(AppPreferences.IS_ONBOARDING, true)
        handleNavigationStrategy()
    }

    private fun handleNavigationStrategy() {
        val strategy = RemoteConfigManager.getAdsConfig().onBoardingMonetizationStrategy
        _navigationEvent.value = when (strategy) {
            0 -> NavigationEvent.Main
            1 -> NavigationEvent.Premium(fromOnboarding = true)
            2 -> NavigationEvent.MainWithInterstitial
            else -> NavigationEvent.Premium(fromOnboarding = true)
        }
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    private fun getOnboardingItems(): List<OnboardingItem> {
        return listOf(
            OnboardingItem(
                title = application.getString(R.string.onboarding_title_1),
                description = application.getString(R.string.onboarding_desc_1),
                imageRes = R.drawable.onboarding_1
            ),
            OnboardingItem(
                title = application.getString(R.string.onboarding_title_2),
                description = application.getString(R.string.onboarding_desc_2),
                imageRes = R.drawable.onboarding_2
            ),
            OnboardingItem(
                title = application.getString(R.string.onboarding_title_3),
                description = application.getString(R.string.onboarding_desc_3),
                imageRes = R.drawable.onboarding_3
            )
        )
    }
}

data class OnboardingUiState(
    val onboardingItems: List<OnboardingItem>,
    val totalPages: Int
)


