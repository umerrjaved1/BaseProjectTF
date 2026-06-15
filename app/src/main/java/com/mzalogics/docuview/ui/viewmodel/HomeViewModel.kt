package com.mzalogics.docuview.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzalogics.docuview.data.DocumentRepository
import com.mzalogics.docuview.model.DocumentItem
import com.mzalogics.docuview.model.QuickAccessItem
import com.mzalogics.docuview.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class HomeEvent {
    data class OpenFilePicker(val mimeTypes: List<String>) : HomeEvent()
    data class OpenDocument(val documentId: String) : HomeEvent()
    data object OpenRecentAll : HomeEvent()
    data object NavigateToFavorites : HomeEvent()
}

data class HomeUiState(
    val quickAccess: List<QuickAccessItem> = emptyList(),
    val recentDocuments: List<DocumentItem> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val quickAccess = listOf(
        QuickAccessItem("application/pdf", "PDF", R.drawable.ic_pdf),
        QuickAccessItem("word", "Word", R.drawable.ic_doc),
        QuickAccessItem("excel", "Excel", R.drawable.ic_excel),
        QuickAccessItem("ppt", "PPT", R.drawable.ic_ppt),
        QuickAccessItem("text", "TXT", R.drawable.ic_text),
        QuickAccessItem("starred", "Starred", R.drawable.ic_stared)
    )

    private val _uiState = MutableStateFlow(HomeUiState(quickAccess = quickAccess))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.recentDocuments, repository.favoriteDocuments) { recent, _ ->
                HomeUiState(
                    quickAccess = quickAccess,
                    recentDocuments = recent.take(5)
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onBrowseClicked() {
        _events.tryEmit(HomeEvent.OpenFilePicker(listOf("*/*")))
    }

    fun onRecentAllClicked() {
        _events.tryEmit(HomeEvent.OpenRecentAll)
    }

    fun onRecentClicked(documentId: String) {
        _events.tryEmit(HomeEvent.OpenDocument(documentId))
    }

    fun onQuickAccessClicked(type: String) {
        if (type == "starred") {
            _events.tryEmit(HomeEvent.NavigateToFavorites)
        } else {
            _events.tryEmit(HomeEvent.OpenFilePicker(mimeTypesForType(type)))
        }
    }

    private fun mimeTypesForType(type: String): List<String> {
        return when (type) {
            "application/pdf" -> listOf("application/pdf")
            "word" -> listOf(
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
            "excel" -> listOf(
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
            "ppt" -> listOf(
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            )
            "text" -> listOf("text/plain")
            else -> listOf("*/*")
        }
    }

    fun onFilePicked(documentItemId: String) {
        _events.tryEmit(HomeEvent.OpenDocument(documentItemId))
    }

    fun onUriPicked(uri: Uri) {
        viewModelScope.launch {
            repository.addOrUpdateFromUri(uri)
            _events.tryEmit(HomeEvent.OpenDocument(uri.toString()))
        }
    }

    fun toggleFavorite(documentId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(documentId)
        }
    }
}
