package com.tf.phonecleaner.booster.data

import android.net.Uri
import com.tf.phonecleaner.booster.model.DocumentItem
import kotlinx.coroutines.flow.StateFlow

interface DocumentRepository {
    val recentDocuments: StateFlow<List<DocumentItem>>
    val favoriteDocuments: StateFlow<List<DocumentItem>>

    suspend fun addOrUpdateFromUri(uri: Uri)
    suspend fun toggleFavorite(documentId: String)
    suspend fun setFavorite(documentId: String, isFavorite: Boolean)
    suspend fun clearRecentHistory()
    fun getDocumentById(documentId: String): DocumentItem?
}
