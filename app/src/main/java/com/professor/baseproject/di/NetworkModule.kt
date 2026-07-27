package com.professor.baseproject.di

import com.professor.baseproject.BuildConfig
import com.professor.baseproject.data.source.api.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

/** Distinguishes the long-timeout client used for streaming downloads. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadClient

/** Qualifier name for the API base URL binding. */
const val BASE_URL = "baseUrl"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Qualified deliberately: an unqualified `String` binding in SingletonComponent
     * collides with any other String provider a fork adds, and would be silently
     * injected into any `@Inject lateinit var s: String`.
     *
     * Also normalises the trailing slash — Retrofit throws
     * `IllegalArgumentException: baseUrl must end in /` otherwise, which is an
     * opaque crash at first injection when a fork sets BASE_URL without one.
     */
    @Provides
    @Singleton
    @Named(BASE_URL)
    fun provideBaseUrl(): String = BuildConfig.BASE_URL.trimEnd('/') + "/"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                // Only Accept is set globally. The previous version forced
                // Content-Type: application/json onto *every* request including
                // binary streaming GETs, and rebuilding with .method(m, body) on a
                // bodyless GET is pointless.
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    // Add auth here, e.g.:
                    // .header("Authorization", "Bearer ${BuildConfig.API_KEY}")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Separate client for @Streaming downloads. The default client's 30s read
     * timeout is correct for API calls and wrong for large file transfers, so they
     * must not share one — the old single client used a 15-minute callTimeout for
     * everything, which pinned a socket and a coroutine on any hung API request.
     */
    @Provides
    @Singleton
    @DownloadClient
    fun provideDownloadOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .callTimeout(0, TimeUnit.MILLISECONDS) // no overall cap on a download
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @Named(BASE_URL) baseUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    // Connectivity lives behind ConnectivityObserver (the one implementation that is
    // actually injected). NetworkHelper / NetworkUtils / Utils.isNetworkAvailable /
    // NetworkChangeReceiver were four more copies that disagreed on captive portals.
}
