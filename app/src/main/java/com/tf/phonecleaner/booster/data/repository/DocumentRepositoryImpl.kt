package com.tf.phonecleaner.booster.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tf.phonecleaner.booster.app.AppPreferences
import com.tf.phonecleaner.booster.model.DocumentItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val gson: Gson
) : DocumentRepository {

    private val keyRecent = "recent_documents_json"

    private val _recentDocuments = MutableStateFlow(loadDocuments())
    override val recentDocuments: StateFlow<List<DocumentItem>> = _recentDocuments.asStateFlow()

    private val _favoriteDocuments = MutableStateFlow(_recentDocuments.value.filter { it.isFavorite })
    override val favoriteDocuments: StateFlow<List<DocumentItem>> = _favoriteDocuments.asStateFlow()

    override suspend fun addOrUpdateFromUri(uri: Uri) {
        val metadata = readMetadata(uri) ?: return
        val now = System.currentTimeMillis()
        val id = uri.toString()

        val current = _recentDocuments.value.toMutableList()
        val existing = current.firstOrNull { it.id == id }

        val item = DocumentItem(
            id = id,
            uriString = id,
            name = metadata.first,
            mimeType = metadata.second,
            sizeBytes = metadata.third,
            lastOpenedAt = now,
            isFavorite = existing?.isFavorite == true
        )

        current.removeAll { it.id == id }
        current.add(0, item)

        _recentDocuments.value = current.sortedByDescending { it.lastOpenedAt }
        syncFavoritesAndPersist()
    }

    override suspend fun toggleFavorite(documentId: String) {
        val target = _recentDocuments.value.firstOrNull {
            it.id == documentId || it.uriString == documentId
        } ?: return
        setFavorite(documentId, !target.isFavorite)
    }

    override suspend fun setFavorite(documentId: String, isFavorite: Boolean) {
        val target = _recentDocuments.value.firstOrNull {
            it.id == documentId || it.uriString == documentId
        } ?: return

        val updated = _recentDocuments.value.map {
            val isSameDocument =
                it.id == documentId ||
                    it.uriString == documentId ||
                    it.id == target.id ||
                    it.uriString == target.uriString

            if (isSameDocument) it.copy(isFavorite = isFavorite) else it
        }
        _recentDocuments.value = updated
        syncFavoritesAndPersist()
    }

    override suspend fun clearRecentHistory() {
        _recentDocuments.value = emptyList()
        _favoriteDocuments.value = emptyList()
        appPreferences.remove(keyRecent)
    }

    override fun getDocumentById(documentId: String): DocumentItem? {
        return _recentDocuments.value.firstOrNull {
            it.id == documentId || it.uriString == documentId
        }
    }

    private fun syncFavoritesAndPersist() {
        _favoriteDocuments.value = _recentDocuments.value.filter { it.isFavorite }
        val json = gson.toJson(_recentDocuments.value)
        appPreferences.setString(keyRecent, json)
    }

    private fun loadDocuments(): List<DocumentItem> {
        val json = appPreferences.getString(keyRecent)
        if (json.isBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<DocumentItem>>() {}.type
            gson.fromJson<List<DocumentItem>>(json, type)?.sortedByDescending { it.lastOpenedAt }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun readMetadata(uri: Uri): Triple<String, String, Long>? {
        return try {
            val contentResolver: ContentResolver = context.contentResolver
            var fileName = "Document"
            var size = 0L
            var mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

            val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                }
            }

            if (mimeType == "application/octet-stream") {
                mimeType = when {
                    fileName.endsWith(".pdf", true) -> "application/pdf"
                    fileName.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    fileName.endsWith(".doc", true) -> "application/msword"
                    fileName.endsWith(".xlsx", true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    fileName.endsWith(".xls", true) -> "application/vnd.ms-excel"
                    fileName.endsWith(".pptx", true) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    fileName.endsWith(".ppt", true) -> "application/vnd.ms-powerpoint"
                    fileName.endsWith(".txt", true) -> "text/plain"
                    fileName.endsWith(".zip", true) -> "application/zip"
                    fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) || fileName.endsWith(".png", true) -> "image/*"
                    else -> mimeType
                }
            }

            Triple(fileName, mimeType, size)
        } catch (_: Exception) {
            null
        }
    }
}
