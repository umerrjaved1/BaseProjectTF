package com.mzalogics.docuview.utils

import android.content.Context
import android.util.Log
import com.mzalogics.docuview.remoteconfig.RemoteConfigManager

/**
 * 3-layer ad frequency control:
 * 1. Per-User (daily) — persistent via SharedPreferences
 * 2. Per-Session — in-memory, resets on app launch
 * 3. Per-Ad-Unit — independent limits per ad format
 *
 * All thresholds are driven by Firebase Remote Config via AdsConfigData.
 */

// ─────────────────────────────────────────────────────────────────
// Layer 1: Per-User Daily Cap (Persistent)
// ─────────────────────────────────────────────────────────────────
object AdFrequencyManager {
    private const val TAG = "AdFrequencyManager"
    private const val PREFS_NAME = "ad_frequency_prefs"
    private const val KEY_AD_COUNT = "ad_count_daily"
    private const val KEY_LAST_RESET = "last_reset_time"

    fun canShowAd(context: Context): Boolean {
        val config = RemoteConfigManager.getGlobalAdRulesConfig()
        val resetIntervalMs = config.dailyResetHours * 60 * 60 * 1000L
        val maxAds = config.maxAdsPerDay

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastReset = prefs.getLong(KEY_LAST_RESET, 0L)
        val now = System.currentTimeMillis()

        if (now - lastReset >= resetIntervalMs) {
            prefs.edit()
                .putInt(KEY_AD_COUNT, 0)
                .putLong(KEY_LAST_RESET, now)
                .apply()
            Log.d(TAG, "Daily counter reset")
        }

        val currentCount = prefs.getInt(KEY_AD_COUNT, 0)
        val allowed = currentCount < maxAds
        Log.d(TAG, "canShowAd: count=$currentCount, max=$maxAds, allowed=$allowed")
        return allowed
    }

    fun recordAdShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(KEY_AD_COUNT, 0)
        prefs.edit().putInt(KEY_AD_COUNT, currentCount + 1).apply()
        Log.d(TAG, "Ad recorded: newCount=${currentCount + 1}")
    }
}

// ─────────────────────────────────────────────────────────────────
// Layer 2: Per-Session Cap + Cooldown (In-Memory)
// ─────────────────────────────────────────────────────────────────
object SessionAdTracker {
    private const val TAG = "SessionAdTracker"

    private var sessionAdCount = 0
    private var lastAdShownTime = 0L

    fun canShowAd(): Boolean {
        val config = RemoteConfigManager.getGlobalAdRulesConfig()
        val maxAds = config.maxAdsPerSession
        val cooldownMs = config.sessionCooldownSeconds * 1000L

        val now = System.currentTimeMillis()
        val enoughTimePassed = (now - lastAdShownTime) >= cooldownMs
        val underLimit = sessionAdCount < maxAds

        val allowed = underLimit && enoughTimePassed
        Log.d(TAG, "canShowAd: count=$sessionAdCount/$maxAds, " +
                "cooldown=${(now - lastAdShownTime) / 1000}s/${config.sessionCooldownSeconds}s, allowed=$allowed")
        return allowed
    }

    fun recordAdShown() {
        sessionAdCount++
        lastAdShownTime = System.currentTimeMillis()
        Log.d(TAG, "Ad recorded: sessionCount=$sessionAdCount")
    }

    fun resetSession() {
        sessionAdCount = 0
        lastAdShownTime = 0L
        Log.d(TAG, "Session reset")
    }
}

// ─────────────────────────────────────────────────────────────────
// Layer 3: Per-Ad-Unit / Per-Format (In-Memory)
// ─────────────────────────────────────────────────────────────────
object AdUnitFrequencyController {
    private const val TAG = "AdUnitFreqCtrl"

    const val UNIT_INTERSTITIAL = "interstitial"
    const val UNIT_OPEN_AD = "open_ad"
    const val UNIT_BANNER = "banner"
    const val UNIT_NATIVE = "native"

    data class AdUnitConfig(
        val maxPerSession: Int,
        val cooldownMs: Long
    )

    private val countMap = mutableMapOf<String, Int>()
    private val lastShownMap = mutableMapOf<String, Long>()

    private fun getConfig(adUnitKey: String): AdUnitConfig {
        val config = RemoteConfigManager.getGlobalAdRulesConfig()
        return when (adUnitKey) {
            UNIT_INTERSTITIAL -> AdUnitConfig(
                maxPerSession = config.interstitialMaxPerSession,
                cooldownMs = (config.interstitialMaxTimer ?: 60) * 1000L
            )
            UNIT_OPEN_AD -> AdUnitConfig(
                maxPerSession = config.openAdMaxPerSession,
                cooldownMs = (config.openAdResumeTimer ?: 30) * 1000L
            )
            UNIT_BANNER -> AdUnitConfig(
                maxPerSession = config.bannerMaxPerSession,
                cooldownMs = config.bannerCooldownSeconds * 1000L
            )
            UNIT_NATIVE -> AdUnitConfig(
                maxPerSession = config.nativeMaxPerSession,
                cooldownMs = config.nativeCooldownSeconds * 1000L
            )
            else -> AdUnitConfig(maxPerSession = Int.MAX_VALUE, cooldownMs = 0L)
        }
    }

    fun canShow(adUnitKey: String): Boolean {
        val unitConfig = getConfig(adUnitKey)
        val count = countMap.getOrDefault(adUnitKey, 0)
        val lastShown = lastShownMap.getOrDefault(adUnitKey, 0L)
        val now = System.currentTimeMillis()

        val underLimit = count < unitConfig.maxPerSession
        val enoughTimePassed = (now - lastShown) >= unitConfig.cooldownMs

        val allowed = underLimit && enoughTimePassed
        Log.d(TAG, "canShow($adUnitKey): count=$count/${unitConfig.maxPerSession}, " +
                "cooldown=${(now - lastShown) / 1000}s/${unitConfig.cooldownMs / 1000}s, allowed=$allowed")
        return allowed
    }

    fun recordShown(adUnitKey: String) {
        countMap[adUnitKey] = (countMap.getOrDefault(adUnitKey, 0)) + 1
        lastShownMap[adUnitKey] = System.currentTimeMillis()
        Log.d(TAG, "Recorded($adUnitKey): newCount=${countMap[adUnitKey]}")
    }

    fun resetAll() {
        countMap.clear()
        lastShownMap.clear()
        Log.d(TAG, "All units reset")
    }
}

// ─────────────────────────────────────────────────────────────────
// Combined: All 3 Layers
// ─────────────────────────────────────────────────────────────────
object AdFrequencyControl {
    private const val TAG = "AdFrequencyControl"

    /**
     * Check all 3 layers before showing an ad.
     * @param context Application or Activity context
     * @param adUnitKey One of [AdUnitFrequencyController.UNIT_INTERSTITIAL],
     *                  [AdUnitFrequencyController.UNIT_OPEN_AD],
     *                  [AdUnitFrequencyController.UNIT_BANNER]
     * @return true if all 3 layers allow showing the ad
     */
    fun canShowAd(context: Context, adUnitKey: String): Boolean {
        val userAllowed = AdFrequencyManager.canShowAd(context)
        val unitAllowed = AdUnitFrequencyController.canShow(adUnitKey)

        // Session-level cooldown & cap only applies to full-screen ads (interstitial, open ad)
        val isFullScreenAd = adUnitKey == AdUnitFrequencyController.UNIT_INTERSTITIAL ||
                adUnitKey == AdUnitFrequencyController.UNIT_OPEN_AD
        val sessionAllowed = if (isFullScreenAd) SessionAdTracker.canShowAd() else true

        val allowed = userAllowed && sessionAllowed && unitAllowed
        if (!allowed) {
            Log.d(TAG, "Ad BLOCKED ($adUnitKey) — user:$userAllowed, session:$sessionAllowed, unit:$unitAllowed")
        } else {
            Log.d(TAG, "Ad ALLOWED ($adUnitKey)")
        }
        return allowed
    }

    /**
     * Record that an ad was shown across all 3 layers.
     */
    fun recordAdShown(context: Context, adUnitKey: String) {
        AdFrequencyManager.recordAdShown(context)
        // Only count full-screen ads towards session cap & cooldown
        val isFullScreenAd = adUnitKey == AdUnitFrequencyController.UNIT_INTERSTITIAL ||
                adUnitKey == AdUnitFrequencyController.UNIT_OPEN_AD
        if (isFullScreenAd) {
            SessionAdTracker.recordAdShown()
        }
        AdUnitFrequencyController.recordShown(adUnitKey)
        
        // Log Meta (Facebook) Ad Impression automatically
        com.facebook.appevents.AppEventsLogger.newLogger(context)
            .logEvent(com.facebook.appevents.AppEventsConstants.EVENT_NAME_AD_IMPRESSION)
            
        Log.d(TAG, "Ad shown recorded ($adUnitKey, fullScreen=$isFullScreenAd)")
    }

    /**
     * Reset session-level trackers (call on app cold start if needed).
     */
    fun resetSession() {
        SessionAdTracker.resetSession()
        AdUnitFrequencyController.resetAll()
    }
}
