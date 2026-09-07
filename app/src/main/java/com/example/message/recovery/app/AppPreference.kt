package com.example.message.recovery.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 12:31 pm
Email: umerr8019@gmail.com
 */

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext context: Context) {

    companion object {
        const val PREF_NAME = "MyPreferences"

        // Preference Keys
        const val IS_PREMIUM = "is_premium"
        const val TIME_STAMP = "time_stamp"
        const val IS_LANGUAGE_SELECTED = "is_language_selected"
        const val IS_ONBOARDING = "is_onboarding"
        const val LANGUAGE_ID = "language_id"
        const val LANGUAGE_CODE = "language_code"
        const val AUTO_PLAY = "auto_play"
        const val WIFI_ONLY_DOWNLOAD = "wifi_only_download"
        const val FCM_TOKEN = "fcm_token"

        // Rate Us Keys
        const val HAS_USER_RATED = "has_user_rated"
        const val HAS_COMPLETED_FIRST_DOWNLOAD = "has_completed_first_download"
        const val PENDING_RATE_US_TRIGGER = "pending_rate_us_trigger"
        const val LAST_RATE_US_PROMPT_TIME = "last_rate_us_prompt_time"
        const val WATCHED_APP_PACKAGES = "watched_app_packages"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun clearAll() {
        prefs.edit(commit = false) { clear() }
    }

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit(commit = false) { putBoolean(key, value) }
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        try {
            return prefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            return defaultValue
        }
    }

    fun setInt(key: String, value: Int) {
        prefs.edit(commit = false) { putInt(key, value) }
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        try {
            return prefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            return defaultValue
        }
    }

    fun setLong(key: String, value: Long) {
        prefs.edit(commit = false) { putLong(key, value) }
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        try {
            return prefs.getLong(key, defaultValue)
        } catch (e: Exception) {
            return defaultValue
        }
    }

    fun setString(key: String, value: String) {
        prefs.edit(commit = false) { putString(key, value) }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        try {
            return prefs.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            return defaultValue
        }
    }

    fun remove(key: String) {
        prefs.edit(commit = false) { remove(key) }
    }

    fun saveFav(key: String, ids: Set<Int>) {
        prefs.edit(commit = false) { putStringSet(key, ids.map { it.toString() }.toSet()) }
    }

    fun loadFav(key: String): Set<Int> {
        return prefs.getStringSet(key, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun setStringSet(key: String, values: Set<String>) {
        prefs.edit(commit = false) { putStringSet(key, values) }
    }

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return try {
            prefs.getStringSet(key, defaultValue) ?: defaultValue
        } catch (_: Exception) {
            defaultValue
        }
    }
}
