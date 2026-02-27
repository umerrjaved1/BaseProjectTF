package com.professor.baseproject.data.repository

import android.content.Context
import android.util.Log
import com.professor.baseproject.cache.CacheEntry
import com.professor.baseproject.data.source.LocalSource
import com.professor.baseproject.data.source.RemoteSource
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

/**

Created by Umer Javed
Senior Android Developer
Created on 17/06/2025 3:24 pm
Email: umerr8019@gmail.com

 */

@Suppress("UNCHECKED_CAST")
@Singleton
class DataRepository @Inject constructor(
    private val remoteSource: RemoteSource,
    private val localSource: LocalSource
) {

    // In-memory cache: key = file/key name, value = list
    // Cache with timestamps

    private val cacheMap = mutableMapOf<String, CacheEntry<Any>>()

    // Expiry duration: e.g. 30 minutes
    private val cacheExpiryMillis = 30 * 60 * 1000L

    suspend fun <T> loadData(
        context: Context,
        remoteKey: String,
        assetFileName: String,
        type: Type
    ): List<T> {
        val cacheKey = remoteKey.ifEmpty { assetFileName }

        val now = System.currentTimeMillis()

        val cached = cacheMap[cacheKey]
//        if (cached != null && (now - cached.timestamp) < cacheExpiryMillis)
        if (cached != null) {
            Log.e("TAG", "loadData: From Cache")
            return cached.data as List<T>
        }


        //  2. Load from remote or local
        val remoteData = remoteSource.loadData<T>(context, remoteKey, type)
        val data = remoteData.ifEmpty {
            localSource.loadData(context, assetFileName, type)
        }

        //  3. Cache and return
        cacheMap[cacheKey] = CacheEntry(data as List<Any>, now)

        Log.e("TAG", "loadData: From Data Source")
        return data
    }


    @Suppress("UNCHECKED_CAST")
    fun <T> getCachedData(key: String): List<T>? {
        return cacheMap[key]?.data as? List<T>
    }

    //  Manually clear all cache
    fun clearCache() {
        cacheMap.clear()
    }

    //  Remove a specific entry
    fun removeFromCache(key: String) {
        cacheMap.remove(key)
    }

    //  Check if a key is expired
    fun isCacheExpired(key: String): Boolean {
        val entry = cacheMap[key] ?: return true
        return System.currentTimeMillis() - entry.timestamp >= cacheExpiryMillis
    }

    //  Get remaining time for debug/logging
    fun getCacheRemainingTime(key: String): Long {
        val entry = cacheMap[key] ?: return 0
        val remaining = cacheExpiryMillis - (System.currentTimeMillis() - entry.timestamp)
        return remaining.coerceAtLeast(0)
    }
}