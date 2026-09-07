package com.example.message.recovery.ui.screens.onboarding

import androidx.compose.runtime.Immutable

interface OnboardingContract {
    @Immutable
    data class UiState(
        val pages: List<OnboardingPage> = emptyList(),
        val currentPage: Int = 0,
        val showSmallNative: Boolean = false,
    )

    sealed class Event {
        data class PageChanged(val page: Int) : Event()
        data object Continue : Event()
        data object Skip : Event()
        data object Back : Event()
    }

    sealed class Effect {
        data object ContinueStartup : Effect()
        data object RequestNotificationPermission : Effect()
        data object OpenListenerSettings : Effect()
    }

    data class EventHandler(
        val onPageChanged: (Int) -> Unit,
        val onContinue: () -> Unit,
        val onSkip: () -> Unit,
    )
}
