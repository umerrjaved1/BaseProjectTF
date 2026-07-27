package com.professor.baseproject.di

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */
import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.remoteconfig.IRemoteConfigRepository
import com.professor.baseproject.remoteconfig.RemoteConfigRepositoryImpl
import com.professor.baseproject.utils.ConnectivityObserver
import com.professor.baseproject.utils.NetworkConnectivityObserver
import com.professor.baseproject.data.db.AppDatabase
import com.professor.baseproject.data.db.DataModelDao
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
            )
                // Pre-release convenience: a version bump wipes local data instead of
                // crashing with "A migration from N to N+1 was required but not found".
                // REMOVE THIS and add real migrations before shipping a schema change
                // on a live app. See the note in AppDatabase.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        @Provides
        fun provideDataModelDao(db: AppDatabase): DataModelDao = db.dataModelDao()
    }


}
