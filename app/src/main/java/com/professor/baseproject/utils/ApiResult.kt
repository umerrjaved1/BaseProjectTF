package com.professor.baseproject.utils

data class ApiResult<T>(
    val status: Status,
    val data: T? = null,
    val message: String? = null,
    val code: Int? = null
) {
    companion object {
        fun <T> success(data: T): ApiResult<T> = 
            ApiResult(Status.SUCCESS, data)
        
        fun <T> error(message: String, code: Int? = null): ApiResult<T> = 
            ApiResult(Status.ERROR, message = message, code = code)
        
        fun <T> loading(): ApiResult<T> = 
            ApiResult(Status.LOADING)
    }
    
    enum class Status {
        SUCCESS, ERROR, LOADING
    }
}