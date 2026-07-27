package com.professor.baseproject.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room type converters. `List<String>` is provided because it is the most common
 * non-primitive column a forked app needs; add further converters here as required.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<String>>(value, stringListType) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private companion object {
        val gson = Gson()
        val stringListType = object : TypeToken<List<String>>() {}.type
    }
}
