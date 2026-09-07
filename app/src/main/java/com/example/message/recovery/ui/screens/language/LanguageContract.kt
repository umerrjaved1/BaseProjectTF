package com.example.message.recovery.ui.screens.language

import androidx.compose.runtime.Immutable
import com.example.message.recovery.model.LanguageModel

interface LanguageContract {
    @Immutable
    data class UiState(
        val languages: List<LanguageModel> = emptyList(),
        val selected: LanguageModel? = null,
        val doneLabel: String = "Done",
        val showNativeAd: Boolean = false,
        val nativeAdKey: Int = 0,
        val showExitDialog: Boolean = false,
        val fromStart: Boolean = false,
    )

    sealed class Event {
        data class LanguageClick(val language: LanguageModel) : Event()
        data object Done : Event()
        data object ExitConfirm : Event()
        data object ExitDismiss : Event()
        data object Back : Event()
    }

    sealed class Effect {
        data object ContinueStartup : Effect()
        data object PopToMain : Effect()
        data object FinishAffinity : Effect()
        data class ShowToast(val message: String) : Effect()
    }

    data class EventHandler(
        val onLanguageClick: (LanguageModel) -> Unit,
        val onDone: () -> Unit,
        val onExitConfirm: () -> Unit,
        val onExitDismiss: () -> Unit,
    )
}
