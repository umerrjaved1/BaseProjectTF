package com.professor.baseproject.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.professor.baseproject.R
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.enums.AdState
import com.professor.baseproject.model.LanguageListItem
import com.professor.baseproject.model.LanguageModel
import com.professor.baseproject.utils.NavigationEvent
import com.professor.baseproject.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<LanguageUiState>>(UIState.Loading)
    val uiState: StateFlow<UIState<LanguageUiState>> = _uiState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<LanguageModel?>(null)
    val selectedLanguage: StateFlow<LanguageModel?> = _selectedLanguage.asStateFlow()

    private val _adState = MutableStateFlow(AdState.NOT_LOADED)
    val adState: StateFlow<AdState> = _adState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadLanguageData()
    }

    fun loadLanguageData() {
        _uiState.value = UIState.Loading
        viewModelScope.launch {
            try {
                val languages = getAvailableLanguages()
                val displayList = createDisplayList(languages)
                val defaultLanguage = getDefaultLanguage(languages)
                val uiState = LanguageUiState(
                    languages = languages,
                    displayList = displayList,
                    selectedLanguage = defaultLanguage
                )
                _uiState.value = UIState.Success(uiState)
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e)
            }
        }
    }

    fun selectLanguage(language: LanguageModel) {
        _selectedLanguage.value = language
    }

    fun saveSelectedLanguage() {
        val language = _selectedLanguage.value
        if (language != null) {
            val savedLanguageId = appPreferences.getInt(AppPreferences.LANGUAGE_ID)
            if (language.id != savedLanguageId) {
                appPreferences.setInt(AppPreferences.LANGUAGE_ID, language.id)
                appPreferences.setString(AppPreferences.LANGUAGE_CODE, language.code)
                appPreferences.setBoolean(AppPreferences.IS_LANGUAGE_SELECTED, true)
            }
        }
    }

    fun updateAdState(state: AdState) {
        _adState.value = state
    }

    fun isLanguageSelected(): Boolean {
        return _selectedLanguage.value != null
    }

    fun shouldShowOnboarding(): Boolean {
        return !appPreferences.getBoolean(AppPreferences.IS_ONBOARDING)
    }

    fun navigateToNextScreen() {
        _navigationEvent.value = if (shouldShowOnboarding()) {
            NavigationEvent.Onboarding
        } else {
            NavigationEvent.Main
        }
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    private fun getAvailableLanguages(): List<LanguageModel> {
        return listOf(
            LanguageModel(1, R.drawable.flag_arabic, application.getString(R.string.arabic), "ar"),
            LanguageModel(
                2,
                R.drawable.flag_english,
                application.getString(R.string.english),
                "en"
            ),
            LanguageModel(
                3,
                R.drawable.flag_spanish,
                application.getString(R.string.spanish),
                "es"
            ),
            LanguageModel(
                4,
                R.drawable.flag_indonesia,
                application.getString(R.string.indonesian),
                "in"
            ),
            LanguageModel(
                6,
                R.drawable.flag_persian,
                application.getString(R.string.persian),
                "fa"
            ),
            LanguageModel(7, R.drawable.flag_hindi, application.getString(R.string.hindi), "hi"),
            LanguageModel(8, R.drawable.flag_russia, application.getString(R.string.russian), "ru"),
            LanguageModel(
                9,
                R.drawable.flag_portuguese,
                application.getString(R.string.portuguese),
                "pt"
            ),
            LanguageModel(
                10,
                R.drawable.flag_bangla,
                application.getString(R.string.bengali),
                "bn"
            ),
            LanguageModel(11, R.drawable.flag_turkey, application.getString(R.string.turkish), "tr")
        )
    }

    private fun getDefaultLanguage(languages: List<LanguageModel>): LanguageModel {
        val savedLanguageId = appPreferences.getInt(AppPreferences.LANGUAGE_ID)
        val savedLanguage = languages.find { it.id == savedLanguageId }

        val deviceLangCode = Locale.getDefault().language
        return savedLanguage ?: languages.find { it.code == deviceLangCode }
        ?: languages.find { it.code == "en" } ?: languages.first()
    }

    private fun createDisplayList(languages: List<LanguageModel>): List<LanguageListItem> {
        val defaultLanguage = getDefaultLanguage(languages)
        val displayList = mutableListOf<LanguageListItem>()

        defaultLanguage.let {
            displayList.add(LanguageListItem.Header("Default"))
            displayList.add(LanguageListItem.Language(it))
        }

        displayList.add(LanguageListItem.Header("All Languages"))
        languages.filterNot { it == defaultLanguage }
            .forEach { displayList.add(LanguageListItem.Language(it)) }

        return displayList
    }
}

data class LanguageUiState(
    val languages: List<LanguageModel>,
    val displayList: List<LanguageListItem>,
    val selectedLanguage: LanguageModel
)




