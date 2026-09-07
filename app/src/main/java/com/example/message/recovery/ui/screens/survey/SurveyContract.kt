package com.example.message.recovery.ui.screens.survey

import androidx.compose.runtime.Immutable

@Immutable
data class WatchedAppUi(
    val id: Int,
    val packageName: String,
    val name: String,
    val letter: String,
    val avatarColor: Long,
    val isInstalled: Boolean,
)

interface SurveyContract {
    @Immutable
    data class UiState(
        val apps: List<WatchedAppUi> = emptyList(),
        val selectedPackages: Set<String> = emptySet(),
        val query: String = "",
        val fromHome: Boolean = false,
    ) {
        val visibleApps: List<WatchedAppUi>
            get() {
                val q = query.trim()
                if (q.isEmpty()) return apps
                return apps.filter { it.name.contains(q, ignoreCase = true) }
            }

        val selectedCount: Int
            get() = selectedPackages.count { pkg ->
                apps.any { it.packageName == pkg && it.isInstalled }
            }
        val canContinue: Boolean get() = selectedCount > 0
    }

    sealed class Event {
        data class Toggle(val packageName: String) : Event()
        data class QueryChanged(val query: String) : Event()
        data object Next : Event()
        data object Back : Event()
    }

    sealed class Effect {
        data object ContinueStartup : Effect()
        data object Close : Effect()
        data object FinishAffinity : Effect()
    }
}
