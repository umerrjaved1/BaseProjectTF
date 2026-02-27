package com.professor.baseproject.utils

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val message: String, val code: Int = -1) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()
    object Empty : ApiResponse<Nothing>()
}