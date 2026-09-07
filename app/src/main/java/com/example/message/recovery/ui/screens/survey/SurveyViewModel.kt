package com.example.message.recovery.ui.screens.survey

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.notification.WatchedApps
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SurveyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private var fromHome = false

    private val _uiState = MutableStateFlow(SurveyContract.UiState())
    val uiState: StateFlow<SurveyContract.UiState> = _uiState.asStateFlow()

    private val _effects = Channel<SurveyContract.Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        val apps = WatchedApps.catalog.map { app ->
            WatchedAppUi(
                id = app.id,
                packageName = app.packageName,
                name = app.displayName,
                letter = app.letter,
                avatarColor = app.avatarColor,
                isInstalled = isInstalled(app.packageName),
            )
        }
        val saved = appPreferences.getStringSet(AppPreferences.WATCHED_APP_PACKAGES)
        val installed = apps.filter { it.isInstalled }.map { it.packageName }.toSet()
        val selected = (saved.ifEmpty { WatchedApps.defaultSelectedPackages }).intersect(installed)
        _uiState.update { it.copy(apps = apps, selectedPackages = selected) }
    }

    fun setFromHome(fromHome: Boolean) {
        this.fromHome = fromHome
        _uiState.update { it.copy(fromHome = fromHome) }
    }

    fun onEvent(event: SurveyContract.Event) {
        when (event) {
            is SurveyContract.Event.Toggle -> toggle(event.packageName)
            is SurveyContract.Event.QueryChanged -> _uiState.update { it.copy(query = event.query) }
            SurveyContract.Event.Next -> saveAndFinish()
            SurveyContract.Event.Back -> viewModelScope.launch {
                _effects.send(
                    if (fromHome) SurveyContract.Effect.Close
                    else SurveyContract.Effect.FinishAffinity,
                )
            }
        }
    }

    private fun toggle(packageName: String) {
        val app = _uiState.value.apps.find { it.packageName == packageName } ?: return
        if (!app.isInstalled) return
        _uiState.update { state ->
            val next = if (packageName in state.selectedPackages) {
                state.selectedPackages - packageName
            } else {
                state.selectedPackages + packageName
            }
            state.copy(selectedPackages = next)
        }
    }

    private fun saveAndFinish() {
        val state = _uiState.value
        if (!state.canContinue) return
        appPreferences.setStringSet(AppPreferences.WATCHED_APP_PACKAGES, state.selectedPackages)
        viewModelScope.launch {
            _effects.send(
                if (fromHome) SurveyContract.Effect.Close
                else SurveyContract.Effect.ContinueStartup,
            )
        }
    }

    private fun isInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
