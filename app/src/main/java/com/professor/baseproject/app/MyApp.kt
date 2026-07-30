package com.professor.baseproject.app

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
import com.professor.baseproject.BuildConfig
import com.professor.baseproject.ads.AdsController
import com.professor.baseproject.constants.AppConfigDefaults
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.iab.AppBillingClient
import com.professor.baseproject.iab.ConnectResponse
import com.professor.baseproject.iab.SubscriptionItem
import com.professor.baseproject.ui.screens.LanguageActivity
import com.professor.baseproject.ui.screens.OnboardingActivity
import com.professor.baseproject.ui.screens.StartActivity
import com.professor.baseproject.ui.viewmodel.SettingsViewModel
import com.professor.baseproject.utils.StatusBarUtils
import com.professor.baseproject.ui.screens.PremiumActivity
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
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var adsController: AdsController

    private lateinit var billingClient: AppBillingClient
    private var activeSubscriptions: List<SubscriptionItem> = emptyList()

    /**
     * Activities that should NEVER trigger the Premium redirect on app resume.
     * Checked by simple class name (suffix after last dot).
     */
    private val excludedSimpleNames = setOf(
        StartActivity::class.java.simpleName,      // Splash
        LanguageActivity::class.java.simpleName,   // Language selection
        OnboardingActivity::class.java.simpleName, // Onboarding
        PremiumActivity::class.java.simpleName     // Premium itself
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

        // Same key and same default as SettingsViewModel — previously this used the raw
        // literal "theme_mode" with a MODE_NIGHT_NO default while the ViewModel used a
        // named constant, so the two could disagree about what "unset" means.
        AppCompatDelegate.setDefaultNightMode(
            appPreferences.getInt(SettingsViewModel.THEME_MODE, SettingsViewModel.DEFAULT_MODE)
        )

        // Attach the state every crash report should carry. Without this, reports arrive
        // with no indication of entitlement, locale, or build variant.
        crashReporter.setBaseContext(
            isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM),
            languageCode = appPreferences.getString(AppPreferences.LANGUAGE_CODE),
            buildVariant = BuildConfig.BUILD_TYPE
        )

        // Initialize language settings
        initializeLanguage()

        // Initialize billing client
        initializeBilling()

        // Installs the premium gate and the ad event listener. Must run before any screen can
        // request an ad; it does no network work and shows nothing. The SDK itself is
        // initialized later, from StartActivity, once consent has been gathered.
        adsController.configure()

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
     * Shows PremiumActivity on eligible screens only.
     */
    fun onAppStart() {
        Log.e(TAG, "onStart: ")
        
        // Track app resume
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APP_RESUME)

        // Re-verify against Play on every foreground. Without this, a cancellation,
        // refund, or expiry was never noticed for the life of the install — entitlement
        // was only ever read at cold start.
        refreshEntitlement()

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

        if (isPremium || isExcluded) {
            Log.d(TAG, "Skipping resume screen — premium=$isPremium, excluded=$isExcluded")
            return
        }

        if (AppConfigDefaults.SHOW_PREMIUM_ON_RESUME) {
            activity.startActivity(
                Intent(activity, PremiumActivity::class.java)
                    .putExtra(Constants.EXTRA_PREMIUM_FROM_RESUME, true)
            )
            Log.d(TAG, "Showing PremiumActivity on app resume")
        } else {
            Log.d(TAG, "No resume screen configured — skipping")
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
        billingClient = AppBillingClient.getInstance()

        billingClient.addConnectionListener(this, object : ConnectResponse {
            override fun onConnected(subscriptionItems: List<SubscriptionItem>) {
                Log.d(TAG, "Billing connected. Products: ${subscriptionItems.size}")
                activeSubscriptions = subscriptionItems
                refreshEntitlement()
            }

            override fun onDisconnected() {
                // AppBillingClient reconnects with backoff on its own.
                Log.w(TAG, "Billing service disconnected")
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                // Deliberately does NOT touch IS_PREMIUM. This fired on
                // BILLING_UNAVAILABLE (devices/regions without Play), DEVELOPER_ERROR
                // and SERVICE_UNAVAILABLE, and used to revoke premium from paying users.
                // Entitlement changes only on a successful queryPurchases response.
                Log.e(TAG, "Billing error $errorCode: $errorMessage — keeping cached entitlement")
            }
        })

        billingClient.initialize(this)
    }

    /**
     * The single writer of [AppPreferences.IS_PREMIUM]. Writes only when Play actually
     * answered, so a network failure can never downgrade a subscriber.
     */
    fun refreshEntitlement(onComplete: ((Boolean) -> Unit)? = null) {
        billingClient.refreshEntitlement { status ->
            if (!status.querySucceeded) {
                Log.w(TAG, "Entitlement query failed — leaving IS_PREMIUM untouched")
                onComplete?.invoke(appPreferences.getBoolean(AppPreferences.IS_PREMIUM))
                return@refreshEntitlement
            }

            val wasPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)
            if (wasPremium != status.isSubscribed) {
                appPreferences.setBoolean(AppPreferences.IS_PREMIUM, status.isSubscribed)
                Log.d(TAG, "Entitlement changed: $wasPremium -> ${status.isSubscribed}")
                // Keep the crash key in step — "was this user premium?" is the first
                // question asked about most billing and paywall reports.
                crashReporter.setKey(CrashReporter.KEY_IS_PREMIUM, status.isSubscribed)
            }
            onComplete?.invoke(status.isSubscribed)
        }
    }

    // handleSubscriptions() was removed: it derived entitlement from the *product
    // details* list (`subscriptionItems.any { it.subscribedItem != null }`), so any
    // failure to fetch product details — flaky network, renamed SKU — produced
    // "owns nothing" and wrote IS_PREMIUM=false for real subscribers.
    // refreshEntitlement() now derives it from queryPurchases instead.

    private fun setupNotifications() {
        // Schedule one-time notification
        scheduleOneTimeNotification()

        // Schedule repeating notification if needed
        if (AppConfigDefaults.ENABLE_REPEATING_NOTIFICATIONS) {
            scheduleRepeatingNotification()
        }
    }

    private fun scheduleOneTimeNotification() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(
                AppConfigDefaults.NOTIFICATION_INITIAL_DELAY_HOURS,
                TimeUnit.HOURS
            )
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        Log.d(TAG, "One-time notification scheduled")
    }

    private fun scheduleRepeatingNotification() {
        val repeatInterval = AppConfigDefaults.NOTIFICATION_REPEAT_INTERVAL_HOURS
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
                StatusBarUtils.applyEdgeToEdge(activity)
                Log.d(TAG, "Activity created: ${activity.javaClass.simpleName}")
            }

            override fun onActivityStarted(activity: Activity) {
                // Set currentActivity HERE — this fires BEFORE ProcessLifecycleOwner.onStart(),
                // so our resume handler always has a valid reference to work with.
                currentActivity = activity
                Log.d(TAG, "Activity started: ${activity.javaClass.simpleName}")
            }

            override fun onActivityResumed(activity: Activity) {
                crashReporter.setCurrentScreen(activity.javaClass.simpleName)
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
     * Re-verify entitlement against Play. Safe to call from anywhere; a no-op result
     * (query failure) leaves the cached value alone.
     */
    fun refreshSubscriptionStatus() = refreshEntitlement()

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
        Log.d(TAG, "BaseApplication terminated")
    }
}
