package com.example.message.recovery.ui.screens.main

import androidx.compose.runtime.Immutable
import com.example.message.recovery.notification.WatchedApp
import com.example.message.recovery.ui.screens.survey.WatchedAppUi

@Immutable
data class ChatPreviewUi(
    val id: String,
    val name: String,
    val letter: String,
    val avatarColor: Long,
    val snippet: String,
    val deletedLabel: String,
    val appLabel: String,
    val packageName: String,
    val unread: Boolean,
)

interface ChatHomeContract {
    @Immutable
    data class UiState(
        val watchedApps: List<WatchedAppUi> = emptyList(),
        val selectedFilter: String? = null,
        val chats: List<ChatPreviewUi> = emptyList(),
        val showHomeNative: Boolean = false,
    ) {
        val visibleChats: List<ChatPreviewUi>
            get() {
                val filter = selectedFilter ?: return chats
                return chats.filter { it.packageName == filter }
            }
    }

    sealed class Event {
        data class FilterSelected(val packageName: String?) : Event()
        data object AddApps : Event()
        data object Premium : Event()
        data object Notifications : Event()
    }
}

fun WatchedApp.toUi(installed: Boolean = true) = WatchedAppUi(
    id = id,
    packageName = packageName,
    name = displayName,
    letter = letter,
    avatarColor = avatarColor,
    isInstalled = installed,
)
