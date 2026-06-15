package com.mzalogics.docuview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzalogics.docuview.app.AppPreferences
import com.mzalogics.docuview.remoteconfig.RemoteConfigManager
import com.mzalogics.docuview.utils.NetworkUtils
import com.mzalogics.docuview.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<StartData>>(UIState.Loading)
    val uiState: StateFlow<UIState<StartData>> = _uiState.asStateFlow()

    fun initialize() {
        _uiState.value = UIState.Loading

        viewModelScope.launch {
            val hasInternet = withContext(Dispatchers.Default) {
                NetworkUtils.isConnected(context)
            }

            val remoteConfigSuccess = if (hasInternet) {
                withTimeoutOrNull(12_000L) {
                    fetchRemoteConfigSuspend()
                } ?: false
            } else {
                false
            }

            // Note: We do NOT touch IS_PREMIUM here.
            // IS_PREMIUM is managed exclusively by AppBillingClient based on
            // real Google Play purchases. RemoteConfig's disableAds flag is a
            // separate developer switch and must never overwrite purchase state.


            val isLanguageSelected = appPreferences.getBoolean(AppPreferences.Companion.IS_LANGUAGE_SELECTED, false)
            val isOnboarding = appPreferences.getBoolean(AppPreferences.Companion.IS_ONBOARDING, false)
            val isPremium = appPreferences.getBoolean(AppPreferences.Companion.IS_PREMIUM)

            _uiState.value = UIState.Success(
                StartData(
                    hasInternet = hasInternet,
                    remoteConfigLoaded = remoteConfigSuccess,
                    isPremium = isPremium,
                    isLanguageSelected = isLanguageSelected,
                    isOnboardingDone = isOnboarding
                )
            )
        }
    }

    private suspend fun fetchRemoteConfigSuspend(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        RemoteConfigManager.fetchRemoteConfig { success ->
            deferred.complete(success)
        }
        return deferred.await()
    }
}

data class StartData(
    val hasInternet: Boolean,
    val remoteConfigLoaded: Boolean,
    val isPremium: Boolean,
    val isLanguageSelected: Boolean,
    val isOnboardingDone: Boolean
)
