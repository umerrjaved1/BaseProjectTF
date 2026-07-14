package com.tf.gpsmapcamera.ui.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.model.LanguageModel
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
        val savedLanguageId = appPreferences.getInt(AppPreferences.LANGUAGE_ID)

        if (selected.id != savedLanguageId) {
            appPreferences.setInt(AppPreferences.LANGUAGE_ID, selected.id)
            appPreferences.setString(AppPreferences.LANGUAGE_CODE, selected.code)
        }

        val isFirstTime = !appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED)
        if (isFirstTime) {
            appPreferences.setBoolean(AppPreferences.IS_LANGUAGE_SELECTED, true)
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
