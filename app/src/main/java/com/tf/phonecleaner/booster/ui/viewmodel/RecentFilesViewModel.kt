package com.tf.phonecleaner.booster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tf.phonecleaner.booster.data.DocumentRepository
import com.tf.phonecleaner.booster.model.DocumentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class RecentFilesEvent {
    data class OpenDocument(val documentId: String) : RecentFilesEvent()
    data object OpenFilePicker : RecentFilesEvent()
}

@HiltViewModel
class RecentFilesViewModel @Inject constructor(
    private val repository: DocumentRepository
) : ViewModel() {

    val recentFiles: StateFlow<List<DocumentItem>> = repository.recentDocuments

    private val _events = MutableSharedFlow<RecentFilesEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RecentFilesEvent> = _events.asSharedFlow()

    fun onFileClicked(documentId: String) {
        _events.tryEmit(RecentFilesEvent.OpenDocument(documentId))
    }

    fun onOpenPickerClicked() {
        _events.tryEmit(RecentFilesEvent.OpenFilePicker)
    }

    fun toggleFavorite(documentId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(documentId)
        }
    }
}
