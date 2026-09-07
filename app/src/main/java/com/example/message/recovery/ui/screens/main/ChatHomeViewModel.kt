package com.example.message.recovery.ui.screens.main

import androidx.lifecycle.ViewModel
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.notification.WatchedApps
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChatHomeViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatHomeContract.UiState())
    val uiState: StateFlow<ChatHomeContract.UiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val saved = appPreferences.getStringSet(AppPreferences.WATCHED_APP_PACKAGES)
            .ifEmpty { WatchedApps.defaultSelectedPackages }
        val apps = WatchedApps.catalog
            .filter { it.packageName in saved }
            .map { it.toUi() }
        val showNative = RemoteConfigManager.getAdRules().showHomeNative && !AdMobManager.isPremium
        _uiState.update { current ->
            val next = current.copy(
                watchedApps = apps,
                // Same instance every time. refresh() runs on every resume — most often returning
                // from a full-screen ad — and handing the list a freshly allocated set of identical
                // items invalidated every downstream `remember` and made the LazyColumn re-diff and
                // re-lay-out the whole visible list for no change at all.
                chats = SampleChats,
                showHomeNative = showNative,
            )
            // StateFlow already dedupes by equality, but building `next` is the cheap half; this
            // keeps the intent explicit.
            if (next == current) current else next
        }
    }

    fun onEvent(event: ChatHomeContract.Event) {
        when (event) {
            is ChatHomeContract.Event.FilterSelected ->
                _uiState.update { it.copy(selectedFilter = event.packageName) }
            ChatHomeContract.Event.AddApps,
            ChatHomeContract.Event.Premium,
            ChatHomeContract.Event.Notifications,
            -> Unit
        }
    }

}

/**
 * Placeholder content, built once at class load.
 *
 * Deliberately a file-level property rather than a member of the ViewModel: `init` calls [refresh],
 * which reads it, and instance property initialisers — including the hidden field behind a
 * `by lazy` — run in declaration order, so any member declared below `init` is still null at that
 * point. A top-level `val` is initialised before any instance is constructed, and its identity is
 * stable for the whole process, which is what keeps `remember` and LazyColumn item reuse from being
 * invalidated on every resume.
 */
private val SampleChats: List<ChatPreviewUi> = listOf(
        ChatPreviewUi(
            id = "1",
            name = "Sarah",
            letter = "S",
            avatarColor = 0xFFE1BEE7,
            snippet = "Can you send the photos from last night? I think I deleted them by accident.",
            deletedLabel = "Deleted 14:22",
            appLabel = "WhatsApp",
            packageName = WatchedApps.WHATSAPP,
            unread = true,
        ),
        ChatPreviewUi(
            id = "2",
            name = "Omar",
            letter = "O",
            avatarColor = 0xFFFFE0B2,
            snippet = "The meeting moved to Thursday. Don’t share this with the group yet.",
            deletedLabel = "Deleted 13:05",
            appLabel = "Messenger",
            packageName = WatchedApps.MESSENGER,
            unread = false,
        ),
        ChatPreviewUi(
            id = "3",
            name = "Maya",
            letter = "M",
            avatarColor = 0xFFF8BBD0,
            snippet = "I unsent the voice note — it was the wrong one. Check this instead.",
            deletedLabel = "Deleted 11:48",
            appLabel = "Instagram",
            packageName = WatchedApps.INSTAGRAM,
            unread = false,
        ),
        ChatPreviewUi(
            id = "4",
            name = "Alex",
            letter = "A",
            avatarColor = 0xFFC5CAE9,
            snippet = "Here’s the address. Delete after you save it.",
            deletedLabel = "Deleted 09:12",
            appLabel = "WhatsApp",
            packageName = WatchedApps.WHATSAPP,
            unread = true,
        ),
        ChatPreviewUi(
            id = "5",
            name = "Nora",
            letter = "N",
            avatarColor = 0xFFB2DFDB,
            snippet = "Did you see the story before it disappeared?",
            deletedLabel = "Deleted 08:40",
            appLabel = "Instagram",
            packageName = WatchedApps.INSTAGRAM,
            unread = false,
        ),
)
