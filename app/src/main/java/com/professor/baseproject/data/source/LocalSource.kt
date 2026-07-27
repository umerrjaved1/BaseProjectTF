package com.professor.baseproject.data.source

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
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
class LocalSource @Inject constructor() : DataSource {

    override suspend fun <T> loadData(context: Context, keyOrFile: String, type: Type): List<T> {
        return withContext(Dispatchers.IO) {
            try {
                // use{} on both streams — the previous version leaked the asset fd on
                // every call. Gson returns null for an empty/"null" document, and the
                // declared return type is non-null, so the ?: is load-bearing.
                context.assets.open(keyOrFile).use { input ->
                    InputStreamReader(input).use { reader ->
                        Gson().fromJson<List<T>>(reader, type) ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load asset '$keyOrFile'", e)
                emptyList()
            }
        }
    }

    private companion object {
        const val TAG = "LocalSource"
    }
}
