package com.example.message.recovery.di

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.example.message.recovery.remoteconfig.IRemoteConfigRepository
import com.example.message.recovery.remoteconfig.RemoteConfigRepositoryImpl
import com.example.message.recovery.utils.ConnectivityObserver
import com.example.message.recovery.utils.NetworkConnectivityObserver
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DiModule {

    @Provides
    @Singleton
    fun provideAdMobManager(application: Application): AdMobManager {
        return AdMobManager.getInstance(application)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }

    @Provides
    @Singleton
    fun provideRemoteConfigRepository(): IRemoteConfigRepository {
        return RemoteConfigRepositoryImpl()
    }
}
