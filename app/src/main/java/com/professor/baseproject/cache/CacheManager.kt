package com.professor.baseproject.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

enum class CacheType(val folderName: String) {
    AUDIO("audio_cache"),
    IMAGE("image_cache"),
    JSON("json_cache")
}

object CacheManager {

    private const val APP_CACHE_DIR = "app_cache"
    private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours (optional)
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 60_000

    /**
     * Keys on a hash of the **whole** URL, not just the last path segment.
     * Previously `.../v1/data.json` and `.../v2/data.json` collided on `data.json`,
     * and query strings leaked into the filename.
     */
    fun getSafeFileName(url: String): String {
        val normalized = url.trimEnd('/')
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
        val extension = normalized
            .substringAfterLast('/', "")
            .substringBefore('?')
            .substringAfterLast('.', "")
            .takeIf { it.isNotEmpty() && it.length <= 5 && it.all(Char::isLetterOrDigit) }
        return if (extension != null) "$hash.$extension" else "$hash.cache"
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

        // Download to a temp file and rename on success. Streaming straight into the
        // destination left a truncated file behind on a mid-transfer failure, and
        // file.exists() then treated that corrupt partial as a permanent cache hit.
        val tempFile = File(file.parentFile, "${file.name}.${UUID.randomUUID()}.tmp")
        var connection: HttpURLConnection? = null
        try {
            Log.d("CacheManager", "Downloading: $url")
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                connect()
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("CacheManager", "HTTP error: ${connection.responseCode} for $url")
                return@withContext null
            }

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }

            if (!tempFile.renameTo(file)) {
                // renameTo can fail if the destination exists on some filesystems.
                file.delete()
                if (!tempFile.renameTo(file)) {
                    Log.e("CacheManager", "Could not move temp file into place for $url")
                    return@withContext null
                }
            }

            enforceCacheLimit(context, type)
            Log.d("CacheManager", "Cached file: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("CacheManager", "Failed to cache $url", e)
            null
        } finally {
            connection?.disconnect()
            if (tempFile.exists()) tempFile.delete()
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
