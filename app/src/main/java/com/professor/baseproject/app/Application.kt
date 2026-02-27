package com.professor.baseproject.app

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.baseproject.iab.AppBillingClient
import com.professor.baseproject.iab.ConnectResponse
import com.professor.baseproject.iab.SubscriptionItem

import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.utils.StatusBarUtils
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class Application : MultiDexApplication(), LifecycleObserver {

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var billingClient: AppBillingClient
    private var activeSubscriptions: List<SubscriptionItem> = emptyList()

    companion object {
        private const val TAG = "ApplicationClass"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize language settings
        initializeLanguage()

        // Initialize billing client
        initializeBilling()

        // Setup notifications
        //setupNotifications()

        // Setup lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Register activity lifecycle callbacks
        registerActivityLifecycleCallbacks(createActivityLifecycleCallbacks())
    }

    private fun initializeLanguage() {
        val savedLang = appPreferences.getString(AppPreferences.LANGUAGE_CODE)
        val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()

        if (savedLang.isNotEmpty() && savedLang != currentLang) {
            val localeList = LocaleListCompat.forLanguageTags(savedLang)
            AppCompatDelegate.setApplicationLocales(localeList)
            Log.d(TAG, "Language set to: $savedLang")
        }
    }

    private fun initializeBilling() {
        billingClient = AppBillingClient.getInstance()

        billingClient.initialize(this, object : ConnectResponse {
            override fun onConnected(subscriptionItems: List<SubscriptionItem>) {
                Log.d(TAG, "Billing connected successfully. Subscriptions: ${subscriptionItems.size}")
                handleSubscriptions(subscriptionItems)
            }

            override fun onDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                // Billing will automatically try to reconnect when needed
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                Log.e(TAG, "Billing initialization error: $errorCode - $errorMessage")
                // Set premium to false on billing errors to be safe
                appPreferences.setBoolean(AppPreferences.IS_PREMIUM, true)
                AdMobManager.isPremium = true
            }
        })
    }

    private fun handleSubscriptions(subscriptionItems: List<SubscriptionItem>) {
        activeSubscriptions = subscriptionItems

        // Check if user has any active subscription
        val hasActiveSubscription = subscriptionItems.any { it.subscribedItem != null }

        Log.d(TAG, "Subscription check - Has active subscription: $hasActiveSubscription")
        Log.d(TAG, "Found ${subscriptionItems.size} subscription items")

        subscriptionItems.forEach { subscription ->
            Log.d(TAG, "Subscription: ${subscription.sku}, Subscribed: ${subscription.subscribedItem != null}")
        }

        // Update premium status
        appPreferences.setBoolean(AppPreferences.IS_PREMIUM, hasActiveSubscription)
        AdMobManager.isPremium = hasActiveSubscription

        Log.d(TAG, "Premium status updated to: $hasActiveSubscription")
    }

    private fun setupNotifications() {
        // Schedule one-time notification
        scheduleOneTimeNotification()

        // Schedule repeating notification if needed
        if (RemoteConfigManager.shouldEnableRepeatingNotifications()) {
            scheduleRepeatingNotification()
        }
    }

    private fun scheduleOneTimeNotification() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(
                RemoteConfigManager.getNotificationInitialDelay(),
                TimeUnit.HOURS
            )
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        Log.d(TAG, "One-time notification scheduled")
    }

    private fun scheduleRepeatingNotification() {
        val repeatInterval = RemoteConfigManager.getNotificationRepeatInterval()
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval,
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "repeating_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.d(TAG, "Repeating notification scheduled every $repeatInterval hours")
    }

    private fun createActivityLifecycleCallbacks(): ActivityLifecycleCallbacks {
        return object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Apply edge-to-edge for all activities except ad activities
                if (activity.javaClass.simpleName != "AdActivity") {
                    StatusBarUtils.applyEdgeToEdge(activity)
                }
                Log.d(TAG, "Activity created: ${activity.javaClass.simpleName}")
            }

            override fun onActivityStarted(activity: Activity) {
                // Activity started logic if needed
            }

            override fun onActivityResumed(activity: Activity) {
                // Activity resumed logic if needed
            }

            override fun onActivityPaused(activity: Activity) {
                // Activity paused logic if needed
            }

            override fun onActivityStopped(activity: Activity) {
                // Activity stopped logic if needed
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                // Save instance state logic if needed
            }

            override fun onActivityDestroyed(activity: Activity) {
                Log.d(TAG, "Activity destroyed: ${activity.javaClass.simpleName}")
            }
        }
    }

    /**
     * Refresh subscription status - can be called from anywhere in the app
     */
    fun refreshSubscriptionStatus() {
        if (billingClient.isReady()) {
            billingClient.refreshSubscriptionStatus { subscriptions ->
                handleSubscriptions(subscriptions)
            }
        } else {
            Log.w(TAG, "Billing client not ready for refresh")
        }
    }

    /**
     * Check if user has active subscription
     */
    fun hasActiveSubscription(): Boolean {
        return appPreferences.getBoolean(AppPreferences.IS_PREMIUM)
    }

    /**
     * Get active subscriptions list
     */
    fun getActiveSubscriptions(): List<SubscriptionItem> {
        return activeSubscriptions
    }

    /**
     * Get billing client instance
     */
    fun getBillingClient(): AppBillingClient {
        return billingClient
    }

    override fun onTerminate() {
        super.onTerminate()
        // Clean up billing client
        billingClient.disconnect()
        Log.d(TAG, "Application terminated")
    }
}