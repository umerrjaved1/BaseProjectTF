package com.mzalogics.docuview.remoteconfig

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.mzalogics.docuview.remoteconfig.data.*
import com.mzalogics.docuview.R


/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */


object RemoteConfigManager {
    private val TAG = RemoteConfigManager::class.java.simpleName

    private var startScreenConfig = StartScreenConfig()
    private var languageScreenConfig = LanguageScreenConfig()
    private var onboardingScreenConfig = OnboardingScreenConfig()
    private var homeScreenConfig = HomeScreenConfig()
    private var premiumScreenConfig = PremiumScreenConfig()
    private var globalAdRulesConfig = GlobalAdRulesConfig()
    
    private var assetsConfigData: AssetsConfigData = AssetsConfigData()
    private var notificationTime = 3L
    private var notificationInitialDelay = 24L
    private var notificationRepeatInterval = 24L
    private var enableRepeatingNotifications = false

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
        // Parse new modular configs
        startScreenConfig = parseJsonSafe(RemoteConfigKeys.CONFIG_START_SCREEN) ?: StartScreenConfig()
        languageScreenConfig = parseJsonSafe(RemoteConfigKeys.CONFIG_LANGUAGE_SCREEN) ?: LanguageScreenConfig()
        onboardingScreenConfig = parseJsonSafe(RemoteConfigKeys.CONFIG_ONBOARDING_SCREEN) ?: OnboardingScreenConfig()
        homeScreenConfig = parseJsonSafe(RemoteConfigKeys.CONFIG_HOME_SCREEN) ?: HomeScreenConfig()
        premiumScreenConfig = parseJsonSafe(RemoteConfigKeys.CONFIG_PREMIUM_SCREEN) ?: PremiumScreenConfig()
        globalAdRulesConfig = parseJsonSafe(RemoteConfigKeys.CONFIG_GLOBAL_AD_RULES) ?: GlobalAdRulesConfig()

        notificationInitialDelay =
            firebaseRemoteConfig.getLong(RemoteConfigKeys.NOTIFICATION_DELAY_TIME)

        notificationRepeatInterval =
            firebaseRemoteConfig.getLong(RemoteConfigKeys.NOTIFICATION_REPEAT_INTERVAL)

        enableRepeatingNotifications =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.ENABLE_REPEATING_NOTIFICATIONS)

//        showGetStartedButton =
//            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.SHOW_GET_STARTED_BUTTON)

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

    fun getStartScreenConfig() = startScreenConfig
    fun getLanguageScreenConfig() = languageScreenConfig
    fun getOnboardingScreenConfig() = onboardingScreenConfig
    fun getHomeScreenConfig() = homeScreenConfig
    fun getPremiumScreenConfig() = premiumScreenConfig
    fun getGlobalAdRulesConfig() = globalAdRulesConfig

    fun getAssetsConfig(): AssetsConfigData = assetsConfigData

    fun getNotificationInitialDelay(): Long = notificationInitialDelay // Default 24 hours
    fun getNotificationRepeatInterval(): Long = notificationRepeatInterval
    fun shouldEnableRepeatingNotifications(): Boolean = enableRepeatingNotifications
}
