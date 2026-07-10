package com.tf.gpsmapcamera.di

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */
import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.tf.gpsmapcamera.constants.Constants
import com.tf.gpsmapcamera.remoteconfig.IRemoteConfigRepository
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigRepositoryImpl
import com.tf.gpsmapcamera.utils.ConnectivityObserver
import com.tf.gpsmapcamera.utils.NetworkConnectivityObserver
import com.umer_tf.ads.domain.core.AdMobManager
import com.tf.gpsmapcamera.data.db.AppDatabase
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
    fun provideGson(): Gson {
        return Gson()
    }

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


    @Module
    @InstallIn(SingletonComponent::class)
    object DatabaseModule {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context, AppDatabase::class.java, Constants.DB_NAME
            ).build()
        }

      /*  @Provides
        fun provideFavoriteDao(db: AppDatabase): DataModelDao = db.favoriteDao()*/
    }


}
