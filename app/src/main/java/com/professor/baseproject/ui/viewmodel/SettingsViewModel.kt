package com.professor.baseproject.ui.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.professor.baseproject.app.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    companion object {
        const val THEME_MODE = "theme_mode"

        /**
         * Default is FOLLOW_SYSTEM, not MODE_NIGHT_NO — a template should respect the
         * device setting until the user overrides it. MyApp reads the same key with the
         * same default, so the two cannot disagree.
         */
        const val DEFAULT_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    private val _isDarkMode = MutableStateFlow(
        appPreferences.getInt(THEME_MODE, DEFAULT_MODE) == AppCompatDelegate.MODE_NIGHT_YES
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    /**
     * Persists the choice and applies it immediately.
     *
     * `setDefaultNightMode` recreates running Activities, which is what makes the
     * values-night resources take effect without a manual restart. Bar icon colours
     * follow automatically because StatusBarUtils uses SystemBarStyle.auto.
     */
    fun toggleTheme(enabled: Boolean) {
        _isDarkMode.value = enabled
        val mode = if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        appPreferences.setInt(THEME_MODE, mode)
        AppCompatDelegate.setDefaultNightMode(mode)
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
