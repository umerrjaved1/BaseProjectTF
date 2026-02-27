package com.professor.baseproject.data.source

import android.content.Context
import android.util.Log
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
class RemoteSource @Inject constructor() : DataSource {

    override suspend fun <T> loadData(context: Context, keyOrFile: String, type: Type): List<T> {
        return withContext(Dispatchers.IO) {
            Log.e("TAG", "load Remote Data: ")
            try {
                val json = RemoteConfigManager.getAssetsConfig().getString(keyOrFile)
                if (json.isNotEmpty()) {
                    Gson().fromJson(json, type)
                } else emptyList()

            } catch (
                e: Exception
            ) {
                Log.e("TAG", "load Remote Data: $e")
                emptyList()
            }
        }
    }
}