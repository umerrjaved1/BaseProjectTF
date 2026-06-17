package com.mzalogics.docuview.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class CacheType(val folderName: String) {
    AUDIO("audio_cache"),
    IMAGE("image_cache"),
    JSON("json_cache")
}

object CacheManager {

    private const val APP_CACHE_DIR = "app_cache"
    private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours (optional)

     fun getSafeFileName(url: String): String {
        val sanitizedUrl = url.trimEnd('/')
        val name = sanitizedUrl.substringAfterLast('/')
        return name.ifEmpty {
            UUID.randomUUID().toString() + ".cache"
        }
    }

    private fun getCacheDir(context: Context, type: CacheType): File {
        val dir = File(context.cacheDir, "$APP_CACHE_DIR/${type.folderName}")
        if (!dir.exists()) {
            dir.mkdirs()
            Log.d("CacheManager", "Created cache directory: $dir")
        }
        return dir
    }

    private fun getCachedFile(context: Context, url: String, type: CacheType): File {
        val fileName = getSafeFileName(url)
        return File(getCacheDir(context, type), fileName)
    }

    fun isCacheExpired(file: File): Boolean {
        return System.currentTimeMillis() - file.lastModified() > CACHE_EXPIRY_MS
    }

    fun enforceCacheLimit(context: Context, type: CacheType, maxSizeMB: Int = 50) {
        val dir = getCacheDir(context, type)
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        val maxBytes = maxSizeMB * 1024 * 1024
        var currentSize = files.sumOf { it.length() }

        for (file in files) {
            if (currentSize <= maxBytes) break
            currentSize -= file.length()
            file.delete()
            Log.d("CacheManager", "Deleted cached file to enforce limit: ${file.name}")
        }
    }

    suspend fun downloadAndCacheIfNeeded(
        context: Context,
        url: String,
        type: CacheType,
        checkExpiry: Boolean = false
    ): File? = withContext(Dispatchers.IO) {
        val file = getCachedFile(context, url, type)

        if (file.exists()) {
            if (!checkExpiry || !isCacheExpired(file)) {
                Log.d("CacheManager", "Cache hit: ${file.name}")
                return@withContext file
            } else {
                Log.d("CacheManager", "Cache expired: ${file.name}")
                file.delete()
            }
        }

        try {
            Log.d("CacheManager", "Downloading: $url")
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("CacheManager", "HTTP error: ${connection.responseCode} for $url")
                return@withContext null
            }

            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("CacheManager", "Cached file: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("CacheManager", "Failed to cache $url", e)
            null
        }
    }

    fun clearCache(context: Context, type: CacheType) {
        val dir = getCacheDir(context, type)
        dir.listFiles()?.forEach { it.delete() }
        Log.d("CacheManager", "Cleared cache for ${type.name}")
    }

    fun clearAllCache(context: Context) {
        val baseDir = File(context.cacheDir, APP_CACHE_DIR)
        baseDir.deleteRecursively()
        Log.d("CacheManager", "Cleared all app cache")
    }
}
