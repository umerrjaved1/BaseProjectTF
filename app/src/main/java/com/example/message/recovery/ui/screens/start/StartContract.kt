package com.example.message.recovery.ui.screens.start

import androidx.compose.runtime.Immutable

interface StartContract {
    @Immutable
    data class UiState(
        val progress: Float = 0.15f,
        val showGetStarted: Boolean = false,
        val showProgress: Boolean = true,
        val isPremium: Boolean = false,
    )

    sealed class Event {
        data object GetStartedClick : Event()
        data object Initialize : Event()
    }

    sealed class Effect {
        data object NavigateNext : Effect()
    }

    data class EventHandler(
        val onGetStarted: () -> Unit,
    )
}
