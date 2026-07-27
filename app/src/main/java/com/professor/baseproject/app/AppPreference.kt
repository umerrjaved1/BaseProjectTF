package com.professor.baseproject.app

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

        const val IS_LANGUAGE_SELECTED = "is_language_selected"

        /** Set when the onboarding pager is completed/skipped. */
        const val IS_ONBOARDING = "is_onboarding"

        /**
         * Set when the survey screen is completed. Tracked separately from
         * [IS_ONBOARDING]: when both steps shared one flag, configuring
         * `skipSurveyScreen = true` meant nothing ever wrote it and onboarding
         * replayed on every launch.
         */
        const val IS_SURVEY_DONE = "is_survey_done"

        /**
         * Set the first time MainActivity is reached. Use this — not a per-step flag —
         * for "has the user finished first-run?", because any individual step can be
         * turned off from remote config and would then never write its flag.
         */
        const val IS_FIRST_RUN_COMPLETE = "is_first_run_complete"
        const val LANGUAGE_ID = "language_id"
        const val LANGUAGE_CODE = "language_code"
        const val AUTO_PLAY = "auto_play"


    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * Synchronous on purpose: a "reset app" flow typically clears prefs and then
     * restarts the process, and an async apply() can be lost in that window.
     */
    fun clearAll() {
        prefs.edit(commit = true) { clear() }
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
        return try {
            prefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun setString(key: String, value: String) {
        prefs.edit(commit = false) { putString(key, value) }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return try {
            prefs.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            // Must return the default, never a diagnostic string. This value is fed
            // to callers like LocaleListCompat.forLanguageTags(LANGUAGE_CODE); an
            // error message here becomes a malformed locale tag.
            defaultValue
        }
    }

    fun remove(key: String) {
        prefs.edit(commit = false) { remove(key) }
    }

}
