package com.mzalogics.docuview.utils

sealed class UIState<out T> {
    data object Loading : UIState<Nothing>()
    data class Success<T>(val data: T) : UIState<T>()
    data class Error(val throwable: Throwable) : UIState<Nothing>()
}
