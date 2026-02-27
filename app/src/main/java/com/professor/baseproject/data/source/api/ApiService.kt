package com.professor.baseproject.data.source.api

import com.google.gson.annotations.SerializedName
import com.professor.baseproject.constants.ApiConstants
import com.professor.baseproject.constants.Constants
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming
import retrofit2.http.Url


interface ApiService {

    // Example: Upload PDF for conversion
    @Multipart
    @POST(ApiConstants.PDF_TO_DOC)
    suspend fun convertPdfToDoc(
        @Part file: MultipartBody.Part,
        @Header("X-API-Key") apiKey: String? = null
    ): Response<ConversionResponse>

    // Example: Upload document for conversion
    @Multipart
    @POST(ApiConstants.DOC_TO_PDF)
    suspend fun convertDocToPdf(
        @Part file: MultipartBody.Part,
        @Header("X-API-Key") apiKey: String? = null
    ): Response<ConversionResponse>



    // Example: Download converted file
    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>


}

// Example Response Models
data class ConversionResponse(
    val code: String,
    val `data`: Data,
    val msg: String
)

data class Data(
    val assetTypeId: Int,
    val callbackUrl: Any,
    val fileInfoDTOList: List<FileInfoDTO>,
    val sourceType: String,
    val targetType: String,
    val taskCost: Int,
    val taskFailNum: Int,
    val taskFileNum: Int,
    val taskId: String,
    val taskLanguage: Any,
    val taskStatus: String,
    val taskSuccessNum: Int,
    val taskTime: Int
)

data class FileInfoDTO(
    val convertSize: Int,
    val convertTime: Int,
    val downFileName: String,
    val downloadUrl: String,
    val failureCode: String,
    val failureReason: String,
    val fileKey: String,
    val fileName: String,
    val fileParameter: Any,
    val fileSize: Int,
    val fileUrl: String,
    val sourceType: String,
    val status: String,
    val targetType: String,
    val taskId: String
)



data class ApiStatus(
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("version") val version: String
)