package com.example.message.recovery.remoteconfig

import android.util.Log
import com.example.message.recovery.BuildConfig
import com.example.message.recovery.R
import com.example.message.recovery.remoteconfig.data.AdIdsConfig
import com.example.message.recovery.remoteconfig.data.AdRules
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */


object RemoteConfigManager {
    private val TAG = RemoteConfigManager::class.java.simpleName
    private val gson = Gson()
    private val parseScope = CoroutineScope(Dispatchers.Default)

    private var adRules = AdRules()
    private var adIdsConfig = AdIdsConfig()

    private var notificationInitialDelay = 24L
    private var notificationRepeatInterval = 24L
    private var enableRepeatingNotifications = false
    // Ads stay on until Remote Config is parsed. XML + Firebase `show_ads=false` is the kill switch.
    private var showAds = true

    private val firebaseRemoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setFetchTimeoutInSeconds(10)
                    .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                    .build()
            )
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }

    fun fetchRemoteConfig(callback: (Boolean) -> Unit) {
        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    parseScope.launch {
                        parseFetchData()
                        withContext(Dispatchers.Main) {
                            callback(true)
                        }
                    }
                } else {
                    Log.e(TAG, "RemoteConfig fetch failed.")
                    parseScope.launch {
                        parseFetchData()
                        withContext(Dispatchers.Main) {
                            callback(false)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "RemoteConfig fetch error: ${it.message}")
                parseScope.launch {
                    parseFetchData()
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                }
            }
    }

    private fun parseFetchData() {
        adRules = parseJsonSafe(RemoteConfigKeys.CONFIG_AD_RULES) ?: AdRules()
        adIdsConfig = parseJsonSafe(RemoteConfigKeys.CONFIG_AD_IDS) ?: AdIdsConfig()

        notificationInitialDelay =
            firebaseRemoteConfig.getLong(RemoteConfigKeys.NOTIFICATION_DELAY_TIME)

        notificationRepeatInterval =
            firebaseRemoteConfig.getLong(RemoteConfigKeys.NOTIFICATION_REPEAT_INTERVAL)

        enableRepeatingNotifications =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.ENABLE_REPEATING_NOTIFICATIONS)

        showAds = firebaseRemoteConfig.getBoolean(RemoteConfigKeys.SHOW_ADS)
    }

    private inline fun <reified T> parseJsonSafe(key: String): T? {
        val json = firebaseRemoteConfig.getString(key)
        return try {
            if (json.isNotBlank()) {
                gson.fromJson(json, T::class.java)
            } else null
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to parse JSON for key $key: ${ex.message}")
            null
        }
    }


    fun getAdRules() = adRules
    fun getAdIdsConfig() = adIdsConfig

    fun getNotificationInitialDelay(): Long = notificationInitialDelay
    fun getNotificationRepeatInterval(): Long = notificationRepeatInterval
    fun shouldEnableRepeatingNotifications(): Boolean = enableRepeatingNotifications
    fun getShowAds(): Boolean = showAds
}
