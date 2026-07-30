package com.professor.baseproject.remoteconfig

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.professor.baseproject.R
import com.professor.baseproject.remoteconfig.data.AdIdsConfig
import com.professor.baseproject.remoteconfig.data.AdRulesConfig
import com.professor.baseproject.remoteconfig.data.AssetsConfigData
import com.professor.baseproject.remoteconfig.data.NativeAdColors


/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */


/**
 * Reads the three Remote Config parameters listed in [RemoteConfigKeys] and caches the parsed
 * result.
 *
 * Every getter is safe to call before [fetchRemoteConfig] completes: it returns the Kotlin data
 * class defaults, which are the intended shipping behaviour. Nothing here blocks, and a fetch
 * failure leaves the previous values in place rather than reverting to defaults mid-session.
 */
object RemoteConfigManager {
    private val TAG = RemoteConfigManager::class.java.simpleName

    private var adIds = AdIdsConfig()
    private var adRules = AdRulesConfig()
    private var nativeAdColors = NativeAdColors()
    private var assetsConfigData: AssetsConfigData = AssetsConfigData()

    private val firebaseRemoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setFetchTimeoutInSeconds(10)
                    .setMinimumFetchIntervalInSeconds(0)
                    .build()
            )
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }

    fun fetchRemoteConfig(callback: (Boolean) -> Unit) {
        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    parseFetchData()
                    callback(true)
                } else {
                    Log.e(TAG, "RemoteConfig fetch failed.")
                    callback(false)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "RemoteConfig fetch error: ${it.message}")
                callback(false)
            }
    }

    private fun parseFetchData() {
        adIds = parseJsonSafe(RemoteConfigKeys.AD_IDS) ?: AdIdsConfig()
        adRules = parseJsonSafe(RemoteConfigKeys.AD_RULES) ?: AdRulesConfig()
        nativeAdColors = parseJsonSafe(RemoteConfigKeys.NATIVE_CONFIG) ?: NativeAdColors()
    }

    private inline fun <reified T> parseJsonSafe(key: String): T? {
        val json = firebaseRemoteConfig.getString(key)
        return try {
            if (json.isNotBlank()) {
                Gson().fromJson(json, T::class.java)
            } else null
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to parse JSON for key $key: ${ex.message}")
            null
        }
    }

    fun getAdIds(): AdIdsConfig = adIds
    fun getAdRules(): AdRulesConfig = adRules
    fun getNativeAdColors(): NativeAdColors = nativeAdColors

    fun getAssetsConfig(): AssetsConfigData = assetsConfigData
}
