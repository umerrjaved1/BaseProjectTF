package com.example.message.recovery.ui.screens.premium

import androidx.compose.runtime.Immutable

interface PremiumContract {
    @Immutable
    data class UiState(
        val weeklyPrice: String = "",
        val yearlyPrice: String = "",
        val yearlyWeeklyHint: String = "",
        val trialText: String = "",
        val ctaEnabled: Boolean = false,
        val ctaLoading: Boolean = false,
        val selectedPlan: PremiumPlan = PremiumPlan.WEEKLY,
        val showClose: Boolean = false,
        val fromIcon: Boolean = false,
        val fromSplash: Boolean = false,
        val fromSurvey: Boolean = false,
        val fromOnboarding: Boolean = false,
        val fromResume: Boolean = false,
    )

    sealed class Event {
        data class SelectPlan(val plan: PremiumPlan) : Event()
        data object Upgrade : Event()
        data object Close : Event()
        data object Terms : Event()
        data object Privacy : Event()
        data object Restore : Event()
        data object Back : Event()
    }

    sealed class Effect {
        data object Pop : Effect()
        data object ContinueStartupAfterSplash : Effect()
        data object ContinueStartupAfterSurvey : Effect()
        data object GoHome : Effect()
        data object RestartFromStart : Effect()
        data class OpenUrl(val url: String) : Effect()
        data class ShowToast(val message: String) : Effect()
    }

    data class EventHandler(
        val onSelectPlan: (PremiumPlan) -> Unit,
        val onUpgrade: () -> Unit,
        val onClose: () -> Unit,
        val onTerms: () -> Unit,
        val onPrivacy: () -> Unit,
        val onRestore: () -> Unit,
    )
}
