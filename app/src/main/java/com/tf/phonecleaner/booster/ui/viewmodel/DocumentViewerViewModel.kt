package com.tf.phonecleaner.booster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tf.phonecleaner.booster.adapter.PagePreviewItem
import com.tf.phonecleaner.booster.data.DocumentRepository
import com.tf.phonecleaner.booster.model.DocumentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DocumentViewerUiState(
    val document: DocumentItem? = null,
    val pages: List<PagePreviewItem> = emptyList(),
    val zoomPercent: Int = 100
)

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentViewerUiState())
    val uiState: StateFlow<DocumentViewerUiState> = _uiState.asStateFlow()

    fun load(documentId: String) {
        val document = repository.getDocumentById(documentId)
        val totalPages = 12
        val previews = (1..3).map { index ->
            PagePreviewItem(index, totalPages)
        }
        _uiState.value = DocumentViewerUiState(
            document = document,
            pages = previews,
            zoomPercent = 100
        )
    }

    fun toggleFavorite() {
        val currentDocument = _uiState.value.document ?: return
        viewModelScope.launch {
            repository.toggleFavorite(currentDocument.id)
            _uiState.value = _uiState.value.copy(
                document = _uiState.value.document?.copy(isFavorite = !currentDocument.isFavorite)
            )
        }
    }

    fun zoomIn() {
        val current = _uiState.value.zoomPercent
        if (current < 200) {
            _uiState.value = _uiState.value.copy(zoomPercent = current + 10)
        }
    }

    fun zoomOut() {
        val current = _uiState.value.zoomPercent
        if (current > 50) {
            _uiState.value = _uiState.value.copy(zoomPercent = current - 10)
        }
    }
}
