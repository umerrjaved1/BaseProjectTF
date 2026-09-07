package com.example.message.recovery.remoteconfig

import com.example.message.recovery.remoteconfig.data.*

interface IRemoteConfigRepository {
    fun fetchRemoteConfig(callback: (Boolean) -> Unit)
    fun getAdRules(): AdRules
    fun getAdIdsConfig(): AdIdsConfig

    fun getNotificationInitialDelay(): Long
    fun getNotificationRepeatInterval(): Long
    fun shouldEnableRepeatingNotifications(): Boolean
    fun getShowAds(): Boolean
}

class RemoteConfigRepositoryImpl : IRemoteConfigRepository {
    override fun fetchRemoteConfig(callback: (Boolean) -> Unit) {
        RemoteConfigManager.fetchRemoteConfig(callback)
    }

    override fun getAdRules() = RemoteConfigManager.getAdRules()
    override fun getAdIdsConfig() = RemoteConfigManager.getAdIdsConfig()
    override fun getNotificationInitialDelay() = RemoteConfigManager.getNotificationInitialDelay()
    override fun getNotificationRepeatInterval() = RemoteConfigManager.getNotificationRepeatInterval()
    override fun shouldEnableRepeatingNotifications() = RemoteConfigManager.shouldEnableRepeatingNotifications()
    override fun getShowAds() = RemoteConfigManager.getShowAds()
}
