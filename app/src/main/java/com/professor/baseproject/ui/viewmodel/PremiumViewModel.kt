package com.professor.baseproject.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.iab.SubscriptionItem
import com.professor.baseproject.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    /**
     * One-shot "a purchase just completed" signal.
     *
     * The screen used to navigate off `uiState.isPremium`, which is seeded from prefs —
     * so merely opening this screen as an existing premium user relaunched the whole app
     * with CLEAR_TASK. A SharedFlow with no replay only fires on a real purchase.
     */
    private val _purchaseCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val purchaseCompleted: SharedFlow<Unit> = _purchaseCompleted.asSharedFlow()

    init {
        loadInitialState()
    }

    fun notifyPurchaseCompleted() {
        _purchaseCompleted.tryEmit(Unit)
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

    /** Sole writer of IS_PREMIUM from the premium screen. */
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
