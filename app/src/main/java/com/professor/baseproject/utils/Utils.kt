package com.professor.baseproject.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.professor.baseproject.R
import java.io.File


/**

Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 12:54 pm
Email: umerr8019@gmail.com

 */

/**
 * Small stateless helpers.
 *
 * Removed from this object, all of it inherited from earlier forks and unreferenced:
 *  - `isNetworkAvailable()` — a fifth connectivity check. Use the injected
 *    [ConnectivityObserver]; the copies disagreed on captive portals.
 *  - `downloadFile()` / `downloadToLocalFile()` — two more download implementations
 *    (the latter never checked the HTTP response code). Use [com.professor.baseproject.cache.CacheManager]
 *    or the Retrofit `@Streaming` endpoint.
 *  - `resetDefaultRingtones()` — from a ringtone app; nothing here sets ringtones.
 */
object Utils {

    @SuppressLint("Range")
    fun getFileNameFromUri(uri: Uri, context: Context): String {
        return when (uri.scheme) {
            "content" -> {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        // Was "Document.pdf" — a default from a document-viewer fork that
                        // mislabelled every file of unknown name in every other app.
                        it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                            ?: FALLBACK_FILE_NAME
                    } else {
                        FALLBACK_FILE_NAME
                    }
                } ?: FALLBACK_FILE_NAME
            }

            "file" -> uri.path?.let { File(it).name } ?: FALLBACK_FILE_NAME
            else -> FALLBACK_FILE_NAME
        }
    }

    fun getBitmapFromDrawable(context: Context, drawableResId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, drawableResId)
    }

    fun flipBitmap(source: Bitmap, horizontal: Boolean = true): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) {
                preScale(-1f, 1f) // flip horizontally
            } else {
                preScale(1f, -1f) // flip vertically
            }
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun getMIMEType(url: String?): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun getMIMETypeFromFile(filePath: String): String? {
        val extension = File(filePath).extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun hideKeyboard(view: View) {
        val imm =
            view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Shares a single file. All user-facing copy comes from strings.xml — the share text
     * used to be a hardcoded "Cleaned using Speaker Cleaner - Your ultimate speaker
     * maintenance tool!", which every fork of this template shipped to its users.
     */
    fun shareFile(fileName: String, filePath: String, context: Context) {
        try {
            val fileToShare = File(filePath)
            if (!fileToShare.exists()) {
                Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
                return
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                fileToShare
            )

            val storeUrl =
                "https://play.google.com/store/apps/details?id=${context.packageName}"
            val shareText = context.getString(
                R.string.share_file_message,
                fileName,
                context.getString(R.string.app_name),
                storeUrl
            )

            // ACTION_SEND (not SEND_MULTIPLE) — this shares exactly one file, and
            // SEND_MULTIPLE with a single-item list confuses some receivers.
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = getMIMETypeFromFile(filePath) ?: "*/*"
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.share))
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.error_sharing_file, e.message ?: ""),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private const val FALLBACK_FILE_NAME = "file"
}
