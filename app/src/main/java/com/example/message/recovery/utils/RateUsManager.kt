package com.example.message.recovery.utils

import com.example.message.recovery.app.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateUsManager @Inject constructor(
    private val appPreferences: AppPreferences,
) {
    companion object {
        const val COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    fun shouldShowRateUs(): Boolean {
        if (appPreferences.getBoolean(AppPreferences.HAS_USER_RATED)) return false
        if (!appPreferences.getBoolean(AppPreferences.PENDING_RATE_US_TRIGGER)) return false

        val lastPromptTime = appPreferences.getLong(AppPreferences.LAST_RATE_US_PROMPT_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        if (lastPromptTime > 0L && (currentTime - lastPromptTime) < COOLDOWN_MS) {
            return false
        }

        return true
    }

    fun consumePendingPrompt(): Boolean {
        if (!shouldShowRateUs()) return false
        appPreferences.setBoolean(AppPreferences.PENDING_RATE_US_TRIGGER, false)
        appPreferences.setLong(AppPreferences.LAST_RATE_US_PROMPT_TIME, System.currentTimeMillis())
        return true
    }

    fun onUserRated() {
        appPreferences.setBoolean(AppPreferences.HAS_USER_RATED, true)
        appPreferences.setBoolean(AppPreferences.PENDING_RATE_US_TRIGGER, false)
    }
}
