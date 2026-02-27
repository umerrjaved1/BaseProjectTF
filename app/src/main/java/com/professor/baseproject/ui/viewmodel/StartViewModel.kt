package com.professor.baseproject.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<Boolean>>(UIState.Loading)
    val uiState: StateFlow<UIState<Boolean>> = _uiState.asStateFlow()

    val isPremium: Boolean
        get() = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)

    val isLanguageSelected: Boolean
        get() = appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED, false)

    val isOnboarding: Boolean
        get() = appPreferences.getBoolean(AppPreferences.IS_ONBOARDING, false)

    fun initializeApp() {
        _uiState.value = UIState.Loading
        viewModelScope.launch {
            try {
                RemoteConfigManager.fetchRemoteConfig { success ->
                    if (success) {
                        handleRemoteConfigUpdate()
                        _uiState.value = UIState.Success(true)
                    } else {
                        _uiState.value = UIState.Error(Exception("Failed to load remote config"))
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e)
            }
        }
    }

    private fun handleRemoteConfigUpdate() {
        val disableAds = RemoteConfigManager.getDisableAds()
        appPreferences.setBoolean(AppPreferences.IS_PREMIUM, !disableAds)
    }

    fun getNextScreenIntent(): NextScreen {
        return when {
            !isLanguageSelected -> NextScreen.LANGUAGE
            !isOnboarding -> NextScreen.ONBOARDING
            else -> NextScreen.MAIN
        }
    }
}

enum class NextScreen {
    LANGUAGE, ONBOARDING, MAIN
}