package com.mzalogics.docuview.model

data class DocumentItem(
    val id: String,
    val uriString: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastOpenedAt: Long,
    val isFavorite: Boolean
)
