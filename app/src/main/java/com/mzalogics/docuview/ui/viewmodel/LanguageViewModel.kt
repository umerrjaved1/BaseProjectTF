package com.mzalogics.docuview.ui.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzalogics.docuview.app.AppPreferences
import com.mzalogics.docuview.model.LanguageModel
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
    private val appPreferences: AppPreferences
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
        val savedLanguageId = appPreferences.getInt(AppPreferences.Companion.LANGUAGE_ID)

        if (selected.id != savedLanguageId) {
            appPreferences.setInt(AppPreferences.Companion.LANGUAGE_ID, selected.id)
            appPreferences.setString(AppPreferences.Companion.LANGUAGE_CODE, selected.code)
            
            // Apply language change immediately
            val localeList = LocaleListCompat.forLanguageTags(selected.code)
            AppCompatDelegate.setApplicationLocales(localeList)
        }

        val isFirstTime = !appPreferences.getBoolean(AppPreferences.Companion.IS_LANGUAGE_SELECTED)
        if (isFirstTime) {
            appPreferences.setBoolean(AppPreferences.Companion.IS_LANGUAGE_SELECTED, true)
        }

        hasNavigated = true
        viewModelScope.launch {
            _navigate.emit(if (isFirstTime) LanguageNav.ONBOARDING else LanguageNav.MAIN)
        }
    }
}

enum class LanguageNav {
    ONBOARDING,
    MAIN
}
