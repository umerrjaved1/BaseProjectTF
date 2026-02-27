package com.professor.baseproject.enums

sealed class DownloadState {
        data class Loading(val progress: Int) : DownloadState()
        data class Success(val filePath: String, val fileUri: android.net.Uri?) : DownloadState()
        data class Error(val message: String) : DownloadState()
        object Idle : DownloadState()
    }