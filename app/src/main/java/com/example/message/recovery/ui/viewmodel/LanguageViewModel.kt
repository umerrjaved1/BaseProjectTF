package com.example.message.recovery.ui.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.model.LanguageModel
import com.example.message.recovery.ui.navigation.AppNavigator
import com.example.message.recovery.ui.navigation.Home
import com.example.message.recovery.utils.StartupNavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val appNavigator: AppNavigator,
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow<LanguageModel?>(null)
    val selectedLanguage: StateFlow<LanguageModel?> = _selectedLanguage.asStateFlow()

    private val _navigate = MutableSharedFlow<LanguageNav>(extraBufferCapacity = 1)
    val navigate: SharedFlow<LanguageNav> = _navigate.asSharedFlow()

    private var hasNavigated = false

    fun setSelectedLanguage(model: LanguageModel) {
        _selectedLanguage.value = model
    }

    fun onDoneClicked() {
        if (hasNavigated) return

        val selected = _selectedLanguage.value ?: return
        appPreferences.setInt(AppPreferences.LANGUAGE_ID, selected.id)
        appPreferences.setString(AppPreferences.LANGUAGE_CODE, selected.code)

        val isFirstTime = !appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED)
        if (isFirstTime) {
            appPreferences.setBoolean(AppPreferences.IS_LANGUAGE_SELECTED, true)
        }

        hasNavigated = true

        val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val needsLocaleChange = selected.code.isNotEmpty() && !localeMatches(currentLang, selected.code)
        if (needsLocaleChange) {
            val next = if (isFirstTime) {
                StartupNavigationManager.getNextRoute(StartupNavigationManager.Step.LANGUAGE, appPreferences)
            } else {
                Home
            }
            // Recreate would restore Language from the saved NavHost unless we drop that stack.
            appNavigator.prepareLocaleRecreate(next)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selected.code))
            return
        }

        viewModelScope.launch {
            _navigate.emit(if (isFirstTime) LanguageNav.ONBOARDING else LanguageNav.MAIN)
        }
    }

    private fun localeMatches(currentTags: String, code: String): Boolean {
        if (currentTags.isEmpty()) return false
        val primary = currentTags.substringBefore(',').substringBefore('-')
        return primary.equals(code, ignoreCase = true)
    }
}

enum class LanguageNav {
    ONBOARDING,
    MAIN
}
