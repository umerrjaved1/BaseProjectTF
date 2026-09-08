package com.example.message.recovery.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bytedance.sdk.openadsdk.api.init.PAGConfig
import com.bytedance.sdk.openadsdk.api.init.PAGSdk
import com.example.message.recovery.BuildConfig
import com.umer_tf.ads.domain.core.AdMobManager
import com.example.message.recovery.iab.AppBillingClient
import com.example.message.recovery.iab.ConnectResponse
import com.example.message.recovery.iab.SubscriptionItem
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.ui.navigation.CurrentNavDestination
import com.example.message.recovery.utils.AdUtils
import com.ironsource.mediationsdk.logger.IronSourceError
import com.mbridge.msdk.MBridgeConstans
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.unity3d.ads.InitializationConfiguration
import com.unity3d.ads.UnityAds
import com.unity3d.ironsourceads.InitListener
import com.unity3d.ironsourceads.InitRequest
import com.unity3d.ironsourceads.IronSourceAds
import com.vungle.ads.InitializationListener
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleError
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import dagger.Lazy

private const val PANGLE_APP_ID = "8889520"

private const val IRON_SOURCE_APP_ID = "27e7db28d"

private const val MINTEGRAL_APP_ID = "5530828"

private const val MINTEGRAL_APP_KEY = "864b2d0fa6bcc315357848822816594a"

private const val UNITY_APP_ID = "6184165"

private const val VUNGLE_APP_ID = "6a98126745d118b487f23405"

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var analyticsManager: Lazy<AnalyticsManager>

    @Inject
    lateinit var adMobManager: Lazy<AdMobManager>

    @Inject
    lateinit var currentNavDestination: CurrentNavDestination

    private val adsInitialized = AtomicBoolean(false)

    private lateinit var billingClient: AppBillingClient
    private var activeSubscriptions: List<SubscriptionItem> = emptyList()

    /**
     * Destinations that should NEVER trigger app-open on resume.
     * Survey and Home/Settings are eligible, matching the old Activity exclusions.
     */

    /** The activity currently in the foreground (null when app is in background). */
    private var currentActivity: Activity? = null

    /**
     * True once the app has been backgrounded at least once.
     * Prevents showing Premium on the very first cold-start resume.
     */
    private var hasBeenInBackground = false

    /** Wall-clock time when [onAppStop] last ran; used for [AdRules.openAdResumeTimer]. */
    private var backgroundedAtMs: Long = 0L

    companion object {
        private const val TAG = "ApplicationClass"
        var ignoreNextResume = false
        var appStartTimeMs: Long = 0L
    }

    override fun onCreate() {
        appStartTimeMs = android.os.SystemClock.elapsedRealtime()
        Log.i("StartupTiming", "⚡ MyApp.onCreate() started at elapsedRealtime=$appStartTimeMs ms")
        super.onCreate()

        // Seed premium before any ad SDK work so paying users skip the waterfall.
        AdMobManager.isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)

        billingClient = AppBillingClient.getInstance()
        CoroutineScope(Dispatchers.IO).launch {
            initializeBilling()
        }

        MyApp.ignoreNextResume = true
        registerActivityLifecycleCallbacks(createActivityLifecycleCallbacks())

        Looper.myQueue().addIdleHandler {
            initializeAdsIfNeeded()
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    this@MyApp.onAppStart()
                }
                override fun onStop(owner: LifecycleOwner) {
                    this@MyApp.onAppStop()
                }
            })
            false
        }
        Log.i(
            "StartupTiming",
            "MyApp.onCreate() finished in ${android.os.SystemClock.elapsedRealtime() - appStartTimeMs}ms"
        )
    }

    /**
     * Constructs AdMob and starts Mobile Ads init. Safe to call more than once.
     *
     * Must stay on the main thread. Constructing AdMobManager builds an AppOpenAdLoader, which
     * registers a lifecycle observer, and LifecycleRegistry.addObserver throws off the main thread.
     * This was briefly moved to Dispatchers.IO to keep MobileAds.initialize() off the main thread;
     * that crashed on startup, and the initialize call itself measured only 7-18ms here, so it is
     * not worth splitting the construction from the init to reclaim.
     *
     * It already runs after the first frame (from an idle handler), which is what keeps it off the
     * critical path.
     */
    fun initializeAdsIfNeeded() {
        if (!adsInitialized.compareAndSet(false, true)) return
        val started = android.os.SystemClock.elapsedRealtime()
        val ads = adMobManager.get()
        ads.initialize {}
        // Resume app-open is handled in [onAppStart] with nav-aware gating. Keeping the SDK
        // observer off avoids double-showing alongside interstitials (shared FullScreenGate).
        ads.setShouldShowResumeAd(false)
        Log.i(
            "StartupTiming",
            "AdMob initialize took ${android.os.SystemClock.elapsedRealtime() - started}ms"
        )
    }

    private fun initializeMediationSdks() {
        try {
            val panglePackage = BuildConfig.APPLICATION_ID
            runCatching {
                PAGConfig::class.java.getMethod("setPackageName", String::class.java)
                    .invoke(null, panglePackage)
            }
            val pangleConfig = PAGConfig.Builder()
                .appId(PANGLE_APP_ID)
                .setPackageName(panglePackage)
                .build()
            PAGSdk.init(this, pangleConfig, object : PAGSdk.PAGInitCallback {
                override fun success() {
                    Log.d(TAG, "Pangle initialized")
                }

                override fun fail(code: Int, msg: String) {
                    Log.e(TAG, "Pangle init failed: $code $msg")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Pangle init error", e)
        }

        try {
            val ironSourceRequest =
                InitRequest.Builder(IRON_SOURCE_APP_ID).build()
            IronSourceAds.init(this, ironSourceRequest, object : InitListener {
                override fun onInitSuccess() {
                    Log.d(TAG, "IronSource initialized")
                }

                override fun onInitFailed(ironSourceError: IronSourceError) {
                    Log.e(TAG, "IronSource init failed: ${ironSourceError.errorMessage}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "IronSource init error", e)
        }

        try {
            VungleAds.init(this, VUNGLE_APP_ID, object : InitializationListener {
                override fun onSuccess() {
                    Log.d(TAG, "Liftoff/Vungle initialized")
                }

                override fun onError(vungleError: VungleError) {
                    Log.e(TAG, "Liftoff/Vungle init failed: ${vungleError.errorMessage}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Liftoff/Vungle init error", e)
        }

        try {
            val mtgSdk = MBridgeSDKFactory.getMBridgeSDK()
            val mtgConfig = HashMap<String, String>()
            mtgConfig[MBridgeConstans.APP_ID] = MINTEGRAL_APP_ID
            mtgConfig[MBridgeConstans.APP_KEY] = MINTEGRAL_APP_KEY
            mtgSdk.init(mtgConfig, this)
            Log.d(TAG, "Mintegral initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Mintegral init error", e)
        }

        try {
            val unityConfig =
                InitializationConfiguration.Builder(UNITY_APP_ID).build()
            UnityAds.initialize(unityConfig) { unityAdsError ->
                if (unityAdsError != null) {
                    Log.e(
                        TAG,
                        "Unity Ads init failed: ${unityAdsError.code}, ${unityAdsError.message}"
                    )
                } else {
                    Log.d(TAG, "Unity Ads initialized")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unity Ads init error", e)
        }
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
        Log.d(TAG, "onAppStart")

        if (!hasBeenInBackground) {
            return
        }

        analyticsManager.get().sendAnalytics(
            AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APP_RESUME
        )

        if (ignoreNextResume) {
            ignoreNextResume = false
            return
        }

        if (!AdUtils.areAdsEnabled()) {
            Log.d(TAG, "Skipping resume app-open — ads disabled")
            return
        }

        val adRules = RemoteConfigManager.getAdRules()
        if (!adRules.showAppOpenAdOnResume) {
            Log.d(TAG, "Skipping resume app-open — RC showAppOpenAdOnResume=false")
            return
        }

        val activity = currentActivity ?: return
        val isExcluded = currentNavDestination.isAppOpenExcluded()
        Log.d(
            TAG,
            "App resumed to: ${activity.javaClass.simpleName} kind=${currentNavDestination.kind} excluded=$isExcluded"
        )
        if (isExcluded) {
            return
        }

        initializeAdsIfNeeded()
        val analytics = analyticsManager.get()
        val ads = adMobManager.get()
        AdUtils.applyRemoteAdControllerConfig(ads)

        if (ads.isFullScreenAdShowing() || ads.appOpenAdLoader.isShowingAd) {
            Log.d(TAG, "Skipping resume app-open — another full-screen ad is showing")
            return
        }

        val timerSec = adRules.openAdResumeTimer.toLong()
        if (timerSec > 0L && backgroundedAtMs > 0L) {
            val elapsedSec = (System.currentTimeMillis() - backgroundedAtMs) / 1000
            if (elapsedSec < timerSec) {
                Log.d(
                    TAG,
                    "Skipping resume app-open — background ${elapsedSec}s < RC timer ${timerSec}s"
                )
                if (!ads.appOpenAdLoader.isResumeAdAvailable()) {
                    ads.appOpenAdLoader.loadResumeAd(activity, null)
                }
                return
            }
        }

        Log.d(TAG, "Attempting resume app-open ad")
        analytics.sendAnalytics(
            AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.APPOPEN_REQUEST
        )
        ignoreNextResume = true
        ads.appOpenAdLoader.showResumeAdIfAvailable { isShown ->
            if (isShown) {
                analytics.sendAnalytics(
                    AnalyticsManager.Action.ACTION_TYPE,
                    AnalyticsManager.Events.APPOPEN_VIEW
                )
            } else {
                Log.d(TAG, "Resume app-open not shown")
                analytics.sendAnalytics(
                    AnalyticsManager.Action.ACTION_TYPE,
                    AnalyticsManager.Events.APPOPEN_REQUEST_FAIL
                )
            }
            ads.appOpenAdLoader.loadResumeAd(activity) { isLoaded ->
                if (isLoaded) {
                    analytics.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE,
                        AnalyticsManager.Events.APPOPEN_REQUEST_PASS
                    )
                }
            }
        }
    }

    /**
     * Called when the entire app goes to the background.
     * Mark the flag so the next [onStart] knows it is a real resume.
     */
    fun onAppStop() {
        hasBeenInBackground = true
        backgroundedAtMs = System.currentTimeMillis()
        Log.d(TAG, "App went to background at $backgroundedAtMs")
    }



    private fun initializeBilling() {
        billingClient.initialize(this, object : ConnectResponse {
            override fun onConnected(subscriptionItems: List<SubscriptionItem>) {
                Log.d(
                    TAG, "Billing connected successfully. Subscriptions: ${subscriptionItems.size}"
                )
                handleSubscriptions(subscriptionItems)
            }

            override fun onDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                // Billing will automatically try to reconnect when needed
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                Log.e(TAG, "Billing initialization error: $errorCode - $errorMessage")
                // Keep the prefs-seeded premium flag. A transient Play error must not
                // re-enable ads for a paying user.
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
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().setInitialDelay(
            RemoteConfigManager.getNotificationInitialDelay(), TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueue(workRequest)
        Log.d(TAG, "One-time notification scheduled")
    }

    private fun scheduleRepeatingNotification() {
        val repeatInterval = RemoteConfigManager.getNotificationRepeatInterval()
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "repeating_notification", ExistingPeriodicWorkPolicy.KEEP, workRequest
        )
        Log.d(TAG, "Repeating notification scheduled every $repeatInterval hours")
    }

    private fun createActivityLifecycleCallbacks(): ActivityLifecycleCallbacks {
        return object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
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
        return AdMobManager.isPremium
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
        billingClient.disconnect()
        Log.d(TAG, "Application terminated")
    }
}
