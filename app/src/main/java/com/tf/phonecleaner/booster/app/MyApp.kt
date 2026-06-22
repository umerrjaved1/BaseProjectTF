package com.tf.phonecleaner.booster.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.umer_tf.ads.domain.core.AdMobManager
import com.tf.phonecleaner.booster.constants.Constants
import com.tf.phonecleaner.booster.iab.AppBillingClient
import com.tf.phonecleaner.booster.iab.ConnectResponse
import com.tf.phonecleaner.booster.iab.SubscriptionItem
import com.tf.phonecleaner.booster.remoteconfig.RemoteConfigManager
import com.tf.phonecleaner.booster.utils.StatusBarUtils
// import com.tf.phonecleaner.booster.ui.screens.PremiumActivity
import com.tf.phonecleaner.booster.utils.AdFrequencyControl
import com.tf.phonecleaner.booster.utils.AdUnitFrequencyController
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var adMobManager: AdMobManager

    private lateinit var billingClient: AppBillingClient
    private var activeSubscriptions: List<SubscriptionItem> = emptyList()

    /**
     * Activities that should NEVER trigger the Premium redirect on app resume.
     * Checked by simple class name (suffix after last dot) so it works
     * even if the AdMob library's AdActivity lives in a different package.
     */
    private val excludedSimpleNames = setOf(
        "AdActivity"                               // AdMob full-screen overlay
    )

    /** The activity currently in the foreground (null when app is in background). */
    private var currentActivity: Activity? = null

    /**
     * True once the app has been backgrounded at least once.
     * Prevents showing Premium on the very first cold-start resume.
     */
    private var hasBeenInBackground = false

    companion object {
        private const val TAG = "ApplicationClass"
        var ignoreNextResume = false
    }

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(
            appPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
        )

        // Immediately seed the in-memory premium flag from SharedPreferences.
        // The AppBillingClient will verify with Google Play asynchronously and
        // update this again once it gets a response. Without this line, there
        // is a 1-2 second window at startup where AdMobManager.isPremium is
        // false (its default) even though the user is actually premium, which
        // causes ads to load and show before billing verifies the purchase.
        AdMobManager.isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)


        // Initialize language settings
        initializeLanguage()

        // Initialize billing client
        initializeBilling()

        // Setup notifications
        //setupNotifications()

        // Observe app-level lifecycle (foreground / background transitions)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                this@MyApp.onAppStart()
            }
            override fun onStop(owner: LifecycleOwner) {
                this@MyApp.onAppStop()
            }
        })

        // Track which activity is currently on top
        registerActivityLifecycleCallbacks(createActivityLifecycleCallbacks())
    }

    // -----------------------------------------------------------------------
    // App-level lifecycle  (DefaultLifecycleObserver)
    // -----------------------------------------------------------------------

    /**
     * Called when the app moves from background to foreground.
     * This is the replacement for the old App Open Resume Ad:
     * we show PremiumActivity instead, on eligible screens only.
     */
    fun onAppStart() {
        Log.e(TAG, "onStart: ")
        
        // Track app resume
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APP_RESUME)

        if (!hasBeenInBackground) {
            // First resume after cold-start — don't show Premium yet
            return
        }

        if (ignoreNextResume) {
            ignoreNextResume = false
            return
        }

        val activity = currentActivity ?: return
        val simpleName = activity.javaClass.simpleName

        val isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)
        val isExcluded = simpleName in excludedSimpleNames
        Log.d(TAG, "App resumed to: $simpleName | isPremium=$isPremium | isExcluded=$isExcluded")

        if (!isPremium && !isExcluded && RemoteConfigManager.getPremiumScreenConfig().showPremiumActivityOnResume) {
            // activity.startActivity(
            //    Intent(activity, PremiumActivity::class.java)
            //        .putExtra(Constants.EXTRA_PREMIUM_FROM_RESUME, true)
            // )
            Log.d(TAG, "Showing PremiumActivity on app resume (DISABLED)")
        } else if (!isPremium && !isExcluded && RemoteConfigManager.getGlobalAdRulesConfig().showAppOpenAdOnResume) {
            if (!AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_OPEN_AD)) {
                Log.d(TAG, "App Open Ad blocked by frequency control on resume")
                return
            }
            Log.d(TAG, "Showing App Open Ad on app resume")
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APPOPEN_REQUEST)
            adMobManager.appOpenAdLoader.loadAppOpenAd(activity) { isLoaded ->
                if (isLoaded) {
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APPOPEN_REQUEST_PASS)
                    adMobManager.appOpenAdLoader.showAppOpenAdIfAvailable {
                        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APPOPEN_VIEW)
                        AdFrequencyControl.recordAdShown(activity, AdUnitFrequencyController.UNIT_OPEN_AD)
                    }
                } else {
                    analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APPOPEN_REQUEST_FAIL)
                }
            }
        } else {
            Log.d(TAG, "Not showing PremiumActivity or App Open Ad on app resume")
        }
    }

    /**
     * Called when the entire app goes to the background.
     * Mark the flag so the next [onStart] knows it is a real resume.
     */
    fun onAppStop() {
        hasBeenInBackground = true
        Log.d(TAG, "App went to background")
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
        billingClient = AppBillingClient.Companion.getInstance()

        billingClient.initialize(this, object : ConnectResponse {
            override fun onConnected(subscriptionItems: List<SubscriptionItem>) {
                Log.d(
                    TAG,
                    "Billing connected successfully. Subscriptions: ${subscriptionItems.size}"
                )
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
            Log.d(
                TAG,
                "Subscription: ${subscription.sku}, Subscribed: ${subscription.subscribedItem != null}"
            )
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
                // Set currentActivity HERE — this fires BEFORE ProcessLifecycleOwner.onStart(),
                // so our resume handler always has a valid reference to work with.
                currentActivity = activity
                Log.d(TAG, "Activity started: ${activity.javaClass.simpleName}")
            }

            override fun onActivityResumed(activity: Activity) {
                // Keep reference up-to-date (handles activity transitions within the app)
                currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                // Do NOT clear here — we still need currentActivity when the app
                // later resumes from background (ProcessLifecycleOwner.onStart fires
                // before onActivityResumed, so clearing on pause would leave it null).
            }

            override fun onActivityStopped(activity: Activity) {
                // Clear only on stop (fully off-screen). If another activity is
                // already on top, currentActivity was already updated by its onStart.
                if (currentActivity == activity) currentActivity = null
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

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
