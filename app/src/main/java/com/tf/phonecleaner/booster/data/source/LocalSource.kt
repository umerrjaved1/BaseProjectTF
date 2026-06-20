package com.tf.phonecleaner.booster.data.source

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
            Log.e("TAG", "load Local Data: ")
            try {
                val inputStream = context.assets.open(keyOrFile)
                val reader = InputStreamReader(inputStream)
                Gson().fromJson(reader, type)
            } catch (
                e: Exception
            ) {
                emptyList()
            }
        }
    }
}