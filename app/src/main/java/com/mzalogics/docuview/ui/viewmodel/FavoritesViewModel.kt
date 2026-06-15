package com.mzalogics.docuview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzalogics.docuview.data.DocumentRepository
import com.mzalogics.docuview.model.DocumentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class FavoritesEvent {
    data class OpenDocument(val documentId: String) : FavoritesEvent()
    data object OpenFilePicker : FavoritesEvent()
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: DocumentRepository
) : ViewModel() {

    val favorites: StateFlow<List<DocumentItem>> = repository.favoriteDocuments

    private val _events = MutableSharedFlow<FavoritesEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FavoritesEvent> = _events.asSharedFlow()

    fun onFavoriteClicked(documentId: String) {
        _events.tryEmit(FavoritesEvent.OpenDocument(documentId))
    }

    fun onBrowseClicked() {
        _events.tryEmit(FavoritesEvent.OpenFilePicker)
    }

    fun toggleFavorite(documentId: String) {
        viewModelScope.launch {
            repository.setFavorite(documentId, false)
        }
    }
}
