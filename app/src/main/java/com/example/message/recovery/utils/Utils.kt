package com.example.message.recovery.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import android.app.Application
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.umer_tf.ads.domain.core.AdMobManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


/**

Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 12:54 pm
Email: umerr8019@gmail.com

 */
object Utils {


    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    @SuppressLint("Range")
    fun getFileNameFromUri(uri: Uri, context: Context): String {
        return when (uri.scheme) {
            "content" -> {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayName =
                            it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        displayName ?: "Document.pdf"
                    } else {
                        "Document.pdf"
                    }
                } ?: "Document.pdf"
            }

            "file" -> uri.path?.let { File(it).name } ?: "file"
            else -> "Document.pdf"
        }
    }

    suspend fun downloadFile(context: Context, url: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

                val fileName = url.toUri().lastPathSegment ?: return@withContext null
                val file = File(context.cacheDir, fileName)

                connection.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            } catch (e: Exception) {
                Log.e("PAG", "Download failed: ${e.message}")
                null
            }
        }


    fun getBitmapFromDrawable(context: Context, drawableResId: Int): Bitmap {
        val bmp = BitmapFactory.decodeResource(
            context.resources,
            drawableResId
        )

        return bmp

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


    suspend fun downloadToLocalFile(context: Context, fileUrl: String, fileName: String): File {
        val file = File(context.getExternalFilesDir(null), fileName)

        withContext(Dispatchers.IO) {
            URL(fileUrl).openStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file
    }

    fun getMIMEType(url: String?): String? {
        var mType: String? = null
        val mExtension: String = MimeTypeMap.getFileExtensionFromUrl(url)
        mType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mExtension)
        return mType
    }

    fun getMIMETypeFromFile(filePath: String): String? {
        val file = File(filePath)
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun hideKeyboard(view: View) {
        val imm =
            view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    fun resetDefaultRingtones(context: Context) {
        try {
            // Reset Phone Ringtone to system default
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                Settings.System.DEFAULT_RINGTONE_URI
            )

            // Reset Notification sound to system default
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_NOTIFICATION,
                Settings.System.DEFAULT_NOTIFICATION_URI
            )

            // Reset Alarm sound to system default
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_ALARM,
                Settings.System.DEFAULT_ALARM_ALERT_URI
            )

//

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


     fun shareFile(fileName: String, filePath: String, context: Context) {
        try {


            // Create File object
            val fileToShare = File(filePath)
            if (!fileToShare.exists()) {
                Toast.makeText(
                    context,
                    "File not found",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Get URI using FileProvider
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                fileToShare
            )

            // Determine MIME type
            val mimeType = getMIMETypeFromFile(filePath) ?: "*/*"

            // Create share intent with multiple content
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, "Sharing file: $fileName\n\nCleaned using Speaker Cleaner - Your ultimate speaker maintenance tool!\n\nDownload now: https://play.google.com/store/apps/details?id=${context.packageName}")
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(fileUri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Launch share dialog
            AdMobManager.getInstance(context.applicationContext as Application)
                .setShouldShowResumeAd(false)
            com.example.message.recovery.app.MyApp.ignoreNextResume = true
            context.startActivity(Intent.createChooser(shareIntent, "Share File"))

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error sharing file: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
