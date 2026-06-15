package com.mzalogics.docuview.remoteconfig

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.mzalogics.docuview.remoteconfig.data.AdsConfigData
import com.mzalogics.docuview.remoteconfig.data.AssetsConfigData
import com.mzalogics.docuview.R


/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */


object RemoteConfigManager {
    private val TAG = RemoteConfigManager::class.java.simpleName

    private var adsConfigData: AdsConfigData = AdsConfigData()
    private var assetsConfigData: AssetsConfigData = AssetsConfigData()
    private var splashAd = 1
    private var showLangNativeMedia: Boolean = false
    private var showOnboardingNativeMedia: Boolean = false
    private var showNativeMedia: Boolean = false
    private var showAds: Boolean = false
    private var showPremiumActivityOnResume: Boolean = false
    private var showPremiumActivityAfterThreeClick: Boolean = false
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

        //Ads Config RemoteConfig
        val json = firebaseRemoteConfig.getString(RemoteConfigKeys.ADS_CONFIG)
        adsConfigData = try {
            if (json.isNotBlank()) {
                Log.e(TAG, "remote config: ")
                Gson().fromJson(json, AdsConfigData::class.java)
            } else {
                Log.e(TAG, "local... ")
                AdsConfigData()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to parse RemoteConfigData: ${ex.message}")
            AdsConfigData()

        }

        //set ads media value
        showLangNativeMedia =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.SHOW_LANG_NATIVE_MEDIA)

        showOnboardingNativeMedia =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.SHOW_ONBOARDING_NATIVE_MEDIA)

        showAds =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.REMOVE_ADS)

        showPremiumActivityAfterThreeClick =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.SHOW_PREMIUM_ACTIVITY_AFTER_THREE_CLICK)


        showPremiumActivityOnResume=firebaseRemoteConfig.getBoolean(RemoteConfigKeys.SHOW_PREMIUM_ACTIVITY_ON_RESUME)

        firebaseRemoteConfig.getBoolean(RemoteConfigKeys.REMOVE_ADS)

        notificationInitialDelay =
            firebaseRemoteConfig.getLong(RemoteConfigKeys.NOTIFICATION_DELAY_TIME)

        notificationRepeatInterval =
            firebaseRemoteConfig.getLong(RemoteConfigKeys.NOTIFICATION_REPEAT_INTERVAL)

        enableRepeatingNotifications =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.ENABLE_REPEATING_NOTIFICATIONS)


    }


    fun getAdsConfig(): AdsConfigData = adsConfigData
    fun getAssetsConfig(): AssetsConfigData = assetsConfigData
    fun getLangNativeMedia() = showLangNativeMedia
    fun getOnBoardingNativeMedia() = showOnboardingNativeMedia
    fun getNativeMedia() = showNativeMedia

    fun getShowPremiumActivityAfterThreeClick() = showPremiumActivityAfterThreeClick

    fun shouldShowAds(): Boolean = showAds

    fun shouldShowPremiumActivityOnResume(): Boolean = showPremiumActivityOnResume


    fun getSplashAd(): Int = splashAd


    fun getNotificationInitialDelay(): Long = notificationInitialDelay // Default 24 hours


    fun getNotificationRepeatInterval(): Long =
        notificationRepeatInterval

    fun shouldEnableRepeatingNotifications(): Boolean = enableRepeatingNotifications


}