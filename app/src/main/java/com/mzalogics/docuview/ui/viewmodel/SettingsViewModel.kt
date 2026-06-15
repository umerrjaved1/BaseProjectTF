package com.mzalogics.docuview.ui.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzalogics.docuview.app.AppPreferences
import com.mzalogics.docuview.data.DocumentRepository
import com.mzalogics.docuview.model.DocumentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val repository: DocumentRepository
) : ViewModel() {

    companion object {
        const val THEME_MODE = "theme_mode"
    }

    private val _isDarkMode = MutableStateFlow(
        appPreferences.getInt(THEME_MODE, AppCompatDelegate.MODE_NIGHT_NO) == AppCompatDelegate.MODE_NIGHT_YES
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    val recentDocuments: StateFlow<List<DocumentItem>> = repository.recentDocuments

    val usedStorageText: StateFlow<String> = repository.recentDocuments
        .combine(_isDarkMode) { recent, _ ->
            val totalBytes = recent.sumOf { it.sizeBytes }
            formatSize(totalBytes)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "0 B"
        )

    fun toggleTheme(enabled: Boolean) {
        _isDarkMode.value = enabled
        val mode = if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        appPreferences.setInt(THEME_MODE, mode)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearRecentHistory()
        }
    }

    private fun formatSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            sizeBytes >= gb -> String.format("%.1f GB", sizeBytes / gb)
            sizeBytes >= mb -> String.format("%.1f MB", sizeBytes / mb)
            sizeBytes >= kb -> String.format("%.1f KB", sizeBytes / kb)
            else -> "$sizeBytes B"
        }
    }
}
