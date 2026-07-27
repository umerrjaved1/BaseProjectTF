package com.professor.baseproject.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.professor.baseproject.data.db.DataModelDao
import com.professor.baseproject.model.DataModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Minimal working example of the Room chain: Hilt -> AppDatabase -> DAO -> Flow -> UI.
 *
 * This exists because the persistence layer previously had no caller anywhere in the app,
 * so the database was never opened at runtime. That is why a missing `room-compiler` — and
 * the fact that DataModel was not annotated `@Entity` — could sit undetected: nothing ever
 * touched the code path. Keeping one real consumer means a broken Room setup fails on the
 * Home screen during development instead of inside someone's fork.
 *
 * Replace this with the fork's real home-screen state, but keep *some* consumer.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dataModelDao: DataModelDao
) : ViewModel() {

    /**
     * Cold Room Flow lifted to a hot StateFlow. `catch` stops a persistence failure from
     * taking the screen down — it surfaces as an empty list instead.
     */
    val items: StateFlow<List<DataModel>> = dataModelDao.observeAll()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    fun addSample(label: String) {
        viewModelScope.launch {
            dataModelDao.insert(DataModel(label = label, tags = listOf("sample")))
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            dataModelDao.deleteAll()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
