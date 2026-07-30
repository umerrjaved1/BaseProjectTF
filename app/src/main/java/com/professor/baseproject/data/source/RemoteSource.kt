package com.professor.baseproject.data.source

import android.content.Context
import com.professor.baseproject.app.CrashReporter
import com.google.gson.Gson
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

/**

Created by Umer Javed
Senior Android Developer
Created on 17/06/2025 3:08 pm
Email: umerr8019@gmail.com

 */

@Singleton
class RemoteSource @Inject constructor(
    private val crashReporter: CrashReporter
) : DataSource {

    override suspend fun <T> loadData(context: Context, keyOrFile: String, type: Type): List<T> {
        return withContext(Dispatchers.IO) {
            try {
                val json = RemoteConfigManager.getAssetsConfig().getString(keyOrFile)
                if (json.isBlank()) {
                    emptyList()
                } else {
                    // Gson yields null for a "null"/empty document; the declared return
                    // type is non-null, so this fallback prevents an NPE downstream.
                    Gson().fromJson<List<T>>(json, type) ?: emptyList()
                }
            } catch (e: Exception) {
                crashReporter.nonFatal(TAG, "Failed to load remote key '$keyOrFile'", e)
                emptyList()
            }
        }
    }

    private companion object {
        const val TAG = "RemoteSource"
    }
}
