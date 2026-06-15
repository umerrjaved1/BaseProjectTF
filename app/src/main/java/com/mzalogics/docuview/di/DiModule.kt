package com.mzalogics.docuview.di

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */
import android.app.Application
import com.google.gson.Gson
import com.mzalogics.ads.domain.core.AdMobManager
import com.mzalogics.docuview.data.DocumentRepository
import com.mzalogics.docuview.data.DocumentRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideDocumentRepository(repositoryImpl: DocumentRepositoryImpl): DocumentRepository {
        return repositoryImpl
    }


}