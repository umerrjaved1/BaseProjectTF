package com.mzalogics.docuview.enums

import android.net.Uri

sealed class DownloadState {
        data class Loading(val progress: Int) : DownloadState()
        data class Success(val filePath: String, val fileUri: Uri?) : DownloadState()
        data class Error(val message: String) : DownloadState()
        object Idle : DownloadState()
    }
