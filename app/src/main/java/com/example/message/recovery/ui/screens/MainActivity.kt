package com.example.message.recovery.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.key
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.message.recovery.R
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.app.MyApp
import com.example.message.recovery.fcm.AppFirebaseMessagingService
import com.example.message.recovery.notification.AppNotificationListenerService
import com.example.message.recovery.notification.NotificationListenerHelper
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.ui.base.BaseActivity
import com.example.message.recovery.ui.navigation.AppNavHost
import com.example.message.recovery.ui.navigation.AppNavigator
import com.example.message.recovery.ui.navigation.AppRoute
import com.example.message.recovery.ui.navigation.CurrentNavDestination
import com.example.message.recovery.ui.navigation.Home
import com.example.message.recovery.ui.navigation.Language
import com.example.message.recovery.ui.navigation.Media
import com.example.message.recovery.ui.navigation.NavScreenKind
import com.example.message.recovery.ui.navigation.Premium
import com.example.message.recovery.ui.navigation.Settings
import com.example.message.recovery.ui.navigation.Start
import com.example.message.recovery.ui.navigation.Status
import com.example.message.recovery.ui.navigation.Survey
import com.example.message.recovery.ui.screens.main.MainContract
import com.example.message.recovery.ui.screens.main.MainDialogs
import com.example.message.recovery.ui.theme.LocalWindowSize
import com.example.message.recovery.ui.theme.AppTheme
import com.example.message.recovery.utils.AdUtils
import com.example.message.recovery.utils.RateUsManager
import com.example.message.recovery.utils.applySavedAppLocale
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    companion object {
        var hasAskedNotificationPermissionThisSession = false
        var hasAskedListenerPermissionThisSession = false
    }

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var analyticsManagerLazy: Lazy<AnalyticsManager>

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var adMobManagerLazy: Lazy<AdMobManager>

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var rateUsManager: RateUsManager

    @Inject
    lateinit var appNavigator: AppNavigator

    @Inject
    lateinit var currentNavDestination: CurrentNavDestination

    private var showExitDialog by mutableStateOf(false)
    private var showNotificationDialog by mutableStateOf(false)
    private var showListenerPermissionDialog by mutableStateOf(false)
    private var showRateUsDialog by mutableStateOf(false)

    // Plain field, not snapshot state: nothing in the composition reads it, so making it observable
    // only cost a snapshot write (and a global snapshot invalidation) on every onResume.
    private var listenerEnabled = false
    private var pendingNavRoute by mutableStateOf<AppRoute?>(null)

    private var isListenerBound = false
    private var listenerService: AppNotificationListenerService? = null

    private val listenerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            listenerService = (service as? AppNotificationListenerService.LocalBinder)?.getService()
            isListenerBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            listenerService = null
            isListenerBound = false
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pendingStart = appNavigator.consumePendingStart()
        if (pendingStart == null) {
            applySavedAppLocale(appPreferences.getString(AppPreferences.LANGUAGE_CODE))
        }
        val initialRoute = pendingStart ?: resolveStartDestination(intent)
        setContent {
            AppTheme {
                // Computed here because it needs the Activity; every screen reads it from the
                // CompositionLocal so they stay previewable without one.
                CompositionLocalProvider(
                    LocalWindowSize provides calculateWindowSizeClass(this),
                ) {
                    key(appNavigator.navHostGeneration) {
                        val navController = rememberNavController()
                        DisposableEffect(pendingNavRoute) {
                            pendingNavRoute?.let { route ->
                                when (route) {
                                    is Premium -> appNavigator.navigate(route)
                                    Settings -> appNavigator.navigateTab(Settings)
                                    Home -> appNavigator.navigateTab(Home)
                                    else -> appNavigator.navigate(route)
                                }
                                pendingNavRoute = null
                            }
                            onDispose { }
                        }
                        val eventHandler = remember {
                            MainContract.EventHandler(
                                onTabSelected = { tab ->
                                    if (tab != currentNavDestination.kind.toTab()) {
                                        analyticsManager.sendAnalytics(
                                            AnalyticsManager.Action.CLICKED,
                                            AnalyticsManager.Events.TAB_CLICK,
                                        )
                                        handleTabInterstitial { appNavigator.navigateTab(tab) }
                                    }
                                },
                                onPremium = { appNavigator.openPremiumFromIcon() },
                                onLanguage = { appNavigator.navigate(Language(fromStart = false)) },
                                onRequestListenerAccess = { openNotificationListenerSettings() },
                                onHomeBack = { showExitDialog = true },
                                onSettingsBack = { appNavigator.navigateTab(Home) },
                                onAddApps = { appNavigator.navigate(Survey(fromHome = true)) },
                                onExitConfirm = {
                                    showExitDialog = false
                                    showExitInterstitialThenLeave()
                                },
                                onExitDismiss = { showExitDialog = false },
                                onNotificationAllow = {
                                    showNotificationDialog = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onNotificationDismiss = { showNotificationDialog = false },
                                onListenerAllow = {
                                    showListenerPermissionDialog = false
                                    openNotificationListenerSettings()
                                },
                                onListenerDismiss = { showListenerPermissionDialog = false },
                                onRate = {
                                    showRateUsDialog = false
                                    rateUsManager.onUserRated()
                                    openPlayStore()
                                },
                                onRateDismiss = { showRateUsDialog = false },
                            )
                        }
                        AppNavHost(
                            navController = navController,
                            startDestination = initialRoute,
                            navigator = appNavigator,
                            currentNavDestination = currentNavDestination,
                            adMobManagerLazy = adMobManagerLazy,
                            adMobManager = adMobManager,
                            analyticsManagerLazy = analyticsManagerLazy,
                            analyticsManager = analyticsManager,
                            appPreferences = appPreferences,
                            eventHandler = eventHandler,
                            onMainVisible = { onEnteredMain() },
                        )
                        // Deliberately a sibling of the NavHost rather than a child of it. When the
                        // dialog flags were passed down as AppNavHost parameters, opening or closing
                        // any dialog — and every onResume, via listenerEnabled — recomposed the whole
                        // navigation graph and the visible screen with it. Read here, only this call
                        // recomposes.
                        MainDialogs(
                            showExitDialog = showExitDialog,
                            showNotificationDialog = showNotificationDialog,
                            showListenerPermissionDialog = showListenerPermissionDialog,
                            showRateUsDialog = showRateUsDialog,
                            eventHandler = eventHandler,
                        )
                    }
                }
            }
        }

        if (initialRoute is Home || initialRoute is Settings) {
            onEnteredMain()
        }
        handlePushNotificationIntent(intent, consumeIfStart = false)
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationListenerBinding()
        if (rateUsManager.consumePendingPrompt()) {
            showRateUsDialog = true
        }
    }

    override fun onDestroy() {
        unbindNotificationListener()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushNotificationIntent(intent, consumeIfStart = true)
    }

    private fun resolveStartDestination(intent: Intent?): AppRoute {
        if (intent == null) return Start
        val isFromFcm = intent.getBooleanExtra(AppFirebaseMessagingService.EXTRA_FROM_FCM, false)
        val targetScreen = intent.getStringExtra(AppFirebaseMessagingService.EXTRA_TARGET_SCREEN)
            ?: intent.getStringExtra("screen")
        if (!isFromFcm && targetScreen.isNullOrEmpty()) return Start
        return when (targetScreen?.lowercase()) {
            AppFirebaseMessagingService.TARGET_PREMIUM -> Premium(fromIcon = true)
            AppFirebaseMessagingService.TARGET_SETTINGS -> Settings
            else -> Home
        }
    }

    private fun handlePushNotificationIntent(intent: Intent?, consumeIfStart: Boolean) {
        if (intent == null) return
        val isFromFcm = intent.getBooleanExtra(AppFirebaseMessagingService.EXTRA_FROM_FCM, false)
        val targetScreen = intent.getStringExtra(AppFirebaseMessagingService.EXTRA_TARGET_SCREEN)
            ?: intent.getStringExtra("screen")
        if (!isFromFcm && targetScreen.isNullOrEmpty()) return
        analyticsManager.sendAnalytics(
            AnalyticsManager.Action.ACTION_TYPE, "fcm_notification_clicked"
        )
        if (!consumeIfStart) return
        pendingNavRoute = when (targetScreen?.lowercase()) {
            AppFirebaseMessagingService.TARGET_PREMIUM -> Premium(fromIcon = true)
            AppFirebaseMessagingService.TARGET_SETTINGS -> Settings
            else -> Home
        }
    }

    override fun handleBackPress() {
        when (currentNavDestination.kind) {
            NavScreenKind.Home -> showExitDialog = true
            NavScreenKind.Media, NavScreenKind.Status, NavScreenKind.Settings -> appNavigator.navigateTab(
                Home
            )

            NavScreenKind.Start -> finishAffinity()
            NavScreenKind.Survey -> if (!appNavigator.pop()) finishAffinity()
            else -> if (!appNavigator.pop()) finishAffinity()
        }
    }

    private var hasEnteredMain = false

    private fun onEnteredMain() {
        if (hasEnteredMain) return
        hasEnteredMain = true
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "MainActivity")
        analyticsManager.sendAnalytics(
            AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.HOME_VIEW
        )
        checkNotificationPermission()
        checkNotificationListenerPermission()
        appPreferences.setBoolean(AppPreferences.IS_ONBOARDING, true)

        AdUtils.applyRemoteAdControllerConfig(adMobManager)
        adMobManager.appOpenAdLoader.loadResumeAd(this) {
            Log.e("ApplicationClass", "onCreate: Main Screen $it")
        }
    }

    private fun showExitInterstitialThenLeave() {
        val adRules = RemoteConfigManager.getAdRules()
        if (!adRules.showBackInterstitial) {
            showExitNotification()
            finishAffinity()
            return
        }
        AdUtils.loadAndShowAdWithTimer(
            activity = this,
            lifecycle = lifecycle,
            lifecycleScope = lifecycleScope,
            adMobManager = adMobManager,
            adUnit = AdIds.getHomeBackInterAdId(),
            analyticsManager = analyticsManager,
            eventNamePrefix = "exit_int",
        ) {
            showExitNotification()
            finishAffinity()
        }
    }

    private var tabSwitchCount = 0

    private fun handleTabInterstitial(onComplete: () -> Unit) {
        val adRules = RemoteConfigManager.getAdRules()
        if (!adRules.showTabInterstitial) {
            onComplete()
            return
        }
        tabSwitchCount++
        val everyN = adRules.interstitialCounter.coerceAtLeast(1)
        if (tabSwitchCount % everyN != 0) {
            onComplete()
            return
        }
        AdUtils.loadAndShowAdWithTimer(
            activity = this,
            lifecycle = lifecycle,
            lifecycleScope = lifecycleScope,
            adMobManager = adMobManager,
            adUnit = AdIds.getHomeInterAd(),
            analyticsManager = analyticsManager,
            eventNamePrefix = "tab_int",
        ) { onComplete() }
    }

    private fun checkNotificationPermission() {
        if (appPreferences.getBoolean(AppPreferences.IS_ONBOARDING, false)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        if (hasAskedNotificationPermissionThisSession) return
        hasAskedNotificationPermissionThisSession = true
        MyApp.ignoreNextResume = true
        showNotificationDialog = true
    }

    private fun checkNotificationListenerPermission() {
        if (appPreferences.getBoolean(AppPreferences.IS_ONBOARDING, false)) {
            listenerEnabled = NotificationListenerHelper.isEnabled(this)
            if (listenerEnabled) bindNotificationListener()
            return
        }
        listenerEnabled = NotificationListenerHelper.isEnabled(this)
        if (listenerEnabled) {
            bindNotificationListener()
            return
        }
        if (hasAskedListenerPermissionThisSession) return
        hasAskedListenerPermissionThisSession = true
        MyApp.ignoreNextResume = true
        showListenerPermissionDialog = true
    }

    private fun refreshNotificationListenerBinding() {
        listenerEnabled = NotificationListenerHelper.isEnabled(this)
        if (listenerEnabled) {
            bindNotificationListener()
        } else {
            unbindNotificationListener()
        }
    }

    private fun bindNotificationListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.service.notification.NotificationListenerService.requestRebind(
                ComponentName(this, AppNotificationListenerService::class.java)
            )
        }
        if (isListenerBound) return
        bindService(
            Intent(this, AppNotificationListenerService::class.java),
            listenerConnection,
            Context.BIND_AUTO_CREATE,
        )
    }

    private fun unbindNotificationListener() {
        if (!isListenerBound) return
        runCatching { unbindService(listenerConnection) }
        isListenerBound = false
        listenerService = null
    }

    private fun openNotificationListenerSettings() {
        MyApp.ignoreNextResume = true
        startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    private fun showExitNotification() {
        val adRules = RemoteConfigManager.getAdRules()
        if (!adRules.enableExitNotification) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = "${getString(R.string.app_name)}_channel"
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                android.app.NotificationChannel(
                    channelId,
                    "${getString(R.string.app_name)} Notifications",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification =
            NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(adRules.exitNotificationTitle)
                .setContentText(adRules.exitNotificationDescription).setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        try {
            NotificationManagerCompat.from(this).notify(1002, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

private fun NavScreenKind.toTab(): AppRoute? = when (this) {
    NavScreenKind.Home -> Home
    NavScreenKind.Media -> Media
    NavScreenKind.Status -> Status
    NavScreenKind.Settings -> Settings
    else -> null
}
