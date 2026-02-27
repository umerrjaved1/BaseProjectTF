package com.professor.baseproject.constants

/**
 * Created by Rana Umer on 2/27/2026.
 *
 * Description: ApiConstants
 *
 * @version 1.0
 */

object ApiConstants {


    const val BASE_URL = "https://api-server.compdf.com/server/v2/process/"
    const val PDF_TO_DOC = "pdf/docx"
    const val DOC_TO_PDF = "docx/pdf"
    const val CONTENT_TYPE_JSON = "application/json"
    const val API_KEY_HEADER = "X-API-Key"
    const val MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB


}
