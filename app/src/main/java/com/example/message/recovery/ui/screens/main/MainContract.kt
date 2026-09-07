package com.example.message.recovery.ui.screens.main

import androidx.compose.runtime.Immutable
import com.example.message.recovery.ui.navigation.AppRoute
import com.example.message.recovery.ui.navigation.Home

interface MainContract {
    @Immutable
    data class UiState(
        val selectedTab: AppRoute = Home,
        val listenerEnabled: Boolean = false,
        val showExitDialog: Boolean = false,
        val showNotificationDialog: Boolean = false,
        val showListenerPermissionDialog: Boolean = false,
        val showRateUsDialog: Boolean = false,
    )

    sealed class Event {
        data class TabSelected(val tab: AppRoute) : Event()
        data object Premium : Event()
        data object Language : Event()
        data object ExitConfirm : Event()
        data object ExitDismiss : Event()
        data object NotificationAllow : Event()
        data object NotificationDismiss : Event()
        data object ListenerAllow : Event()
        data object ListenerDismiss : Event()
        data object Rate : Event()
        data object RateDismiss : Event()
        data object RequestListenerAccess : Event()
        data object Back : Event()
    }

    sealed class Effect {
        data class NavigateTab(val tab: AppRoute) : Effect()
        data object OpenPremium : Effect()
        data object OpenLanguage : Effect()
        data object FinishAffinity : Effect()
        data object RequestPostNotifications : Effect()
        data object OpenListenerSettings : Effect()
        data object OpenPlayStore : Effect()
    }

    data class EventHandler(
        val onTabSelected: (AppRoute) -> Unit,
        val onPremium: () -> Unit,
        val onLanguage: () -> Unit,
        val onRequestListenerAccess: () -> Unit,
        val onHomeBack: () -> Unit,
        val onSettingsBack: () -> Unit,
        val onAddApps: () -> Unit,
        val onExitConfirm: () -> Unit,
        val onExitDismiss: () -> Unit,
        val onNotificationAllow: () -> Unit,
        val onNotificationDismiss: () -> Unit,
        val onListenerAllow: () -> Unit,
        val onListenerDismiss: () -> Unit,
        val onRate: () -> Unit,
        val onRateDismiss: () -> Unit,
    )
}
