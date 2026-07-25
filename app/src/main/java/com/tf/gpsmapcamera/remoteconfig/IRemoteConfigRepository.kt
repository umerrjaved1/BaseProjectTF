package com.tf.gpsmapcamera.remoteconfig

import com.tf.gpsmapcamera.remoteconfig.data.*

interface IRemoteConfigRepository {
    fun fetchRemoteConfig(callback: (Boolean) -> Unit)
    fun getStartScreenConfig(): StartScreenConfig
    fun getLanguageScreenConfig(): LanguageScreenConfig
    fun getOnboardingScreenConfig(): OnboardingScreenConfig
    fun getSurveyScreenConfig(): SurveyScreenConfig
    fun getPremiumScreenConfig(): PremiumScreenConfig
    fun getGlobalConfig(): GlobalConfig
    fun getAssetsConfig(): AssetsConfigData
    fun getNotificationInitialDelay(): Long
    fun getNotificationRepeatInterval(): Long
    fun shouldEnableRepeatingNotifications(): Boolean
}

class RemoteConfigRepositoryImpl : IRemoteConfigRepository {
    override fun fetchRemoteConfig(callback: (Boolean) -> Unit) {
        RemoteConfigManager.fetchRemoteConfig(callback)
    }

    override fun getStartScreenConfig() = RemoteConfigManager.getStartScreenConfig()
    override fun getLanguageScreenConfig() = RemoteConfigManager.getLanguageScreenConfig()
    override fun getOnboardingScreenConfig() = RemoteConfigManager.getOnboardingScreenConfig()
    override fun getSurveyScreenConfig() = RemoteConfigManager.getSurveyScreenConfig()
    override fun getPremiumScreenConfig() = RemoteConfigManager.getPremiumScreenConfig()
    override fun getGlobalConfig() = RemoteConfigManager.getGlobalConfig()
    override fun getAssetsConfig() = RemoteConfigManager.getAssetsConfig()
    override fun getNotificationInitialDelay() = RemoteConfigManager.getNotificationInitialDelay()
    override fun getNotificationRepeatInterval() = RemoteConfigManager.getNotificationRepeatInterval()
    override fun shouldEnableRepeatingNotifications() = RemoteConfigManager.shouldEnableRepeatingNotifications()
}
