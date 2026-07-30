package com.professor.baseproject.remoteconfig

import com.professor.baseproject.remoteconfig.data.AdIdsConfig
import com.professor.baseproject.remoteconfig.data.AdRulesConfig
import com.professor.baseproject.remoteconfig.data.AssetsConfigData
import com.professor.baseproject.remoteconfig.data.NativeAdColors

interface IRemoteConfigRepository {
    fun fetchRemoteConfig(callback: (Boolean) -> Unit)
    fun getAdIds(): AdIdsConfig
    fun getAdRules(): AdRulesConfig
    fun getNativeAdColors(): NativeAdColors
    fun getAssetsConfig(): AssetsConfigData
}

class RemoteConfigRepositoryImpl : IRemoteConfigRepository {
    override fun fetchRemoteConfig(callback: (Boolean) -> Unit) {
        RemoteConfigManager.fetchRemoteConfig(callback)
    }

    override fun getAdIds() = RemoteConfigManager.getAdIds()
    override fun getAdRules() = RemoteConfigManager.getAdRules()
    override fun getNativeAdColors() = RemoteConfigManager.getNativeAdColors()
    override fun getAssetsConfig() = RemoteConfigManager.getAssetsConfig()
}
