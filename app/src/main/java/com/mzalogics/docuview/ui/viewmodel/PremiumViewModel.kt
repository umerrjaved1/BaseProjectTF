package com.mzalogics.docuview.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzalogics.docuview.app.AppPreferences
import com.mzalogics.docuview.iab.SubscriptionItem
import com.mzalogics.docuview.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<PremiumUiState>>(UIState.Loading)
    val uiState: StateFlow<UIState<PremiumUiState>> = _uiState.asStateFlow()

    private val _subscriptions = MutableLiveData<List<SubscriptionItem>>()
    val subscriptions: LiveData<List<SubscriptionItem>> = _subscriptions

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        val isPremium = appPreferences.getBoolean(AppPreferences.Companion.IS_PREMIUM)
        val initialState = PremiumUiState(
            subscriptions = emptyList(),
            isPremium = isPremium
        )
        _uiState.value = UIState.Success(initialState)
    }

    fun updateSubscriptions(subscriptions: List<SubscriptionItem>) {
        viewModelScope.launch {
            _subscriptions.value = subscriptions

            val currentState = when (val state = _uiState.value) {
                is UIState.Success -> state.data
                else -> PremiumUiState(emptyList(), false)
            }

            val updatedState = currentState.copy(subscriptions = subscriptions)
            _uiState.value = UIState.Success(updatedState)
        }
    }

    fun setPremiumStatus(isPremium: Boolean) {
        viewModelScope.launch {
            appPreferences.setBoolean(AppPreferences.Companion.IS_PREMIUM, isPremium)

            val currentState = when (val state = _uiState.value) {
                is UIState.Success -> state.data
                else -> PremiumUiState(emptyList(), false)
            }

            val updatedState = currentState.copy(isPremium = isPremium)
            _uiState.value = UIState.Success(updatedState)
        }
    }
}

data class PremiumUiState(
    val subscriptions: List<SubscriptionItem>,
    val isPremium: Boolean
)