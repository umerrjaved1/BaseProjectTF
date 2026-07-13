package com.tf.gpsmapcamera.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.databinding.ActivityMainBinding
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.ui.base.BaseActivity
import com.tf.gpsmapcamera.update.AppUpdateManager
import com.tf.gpsmapcamera.update.AppUpdateReadyDialogFragment
import com.tf.gpsmapcamera.utils.AdUtils
import com.tf.gpsmapcamera.utils.setClickWithTimeout
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    companion object {
        var hasAskedNotificationPermissionThisSession = false
    }

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    private lateinit var binding: ActivityMainBinding
    private val homeFragment = com.tf.gpsmapcamera.ui.fragments.HomeFragment()
    private val settingsFragment = com.tf.gpsmapcamera.ui.fragments.SettingsFragment()
    private var activeTag = "MainActivity"

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBars()

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "MainActivity")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.HOME_VIEW)

        if (savedInstanceState != null) {
            activeTag = savedInstanceState.getString("active_tag", "home")
        }


        setupFragments(savedInstanceState == null)
        setupClicks()
        updateToolbarForFragment(activeTag)

        checkNotificationPermission()

        appUpdateManager.setOnFlexibleDownloadCompleteListener {
            showFlexibleUpdateReadyDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.handleOnResume(this, updateLauncher)
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.setOnFlexibleDownloadCompleteListener(null)
    }

    private fun showFlexibleUpdateReadyDialog() {
        if (isFinishing || isDestroyed) return
        if (supportFragmentManager.findFragmentByTag(AppUpdateReadyDialogFragment.TAG) != null) return

        val dialog = AppUpdateReadyDialogFragment.newInstance()
        dialog.setOnInstallListener {
            appUpdateManager.completeFlexibleUpdate()
        }
        dialog.show(supportFragmentManager, AppUpdateReadyDialogFragment.TAG)
    }

    override fun handleBackPress() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return
        }
        if (activeTag != "home") {
            switchFragmentTo("home")
            return
        }
        // On final back (exit), show interstitial then finish
        val config = RemoteConfigManager.getHomeScreenConfig()
        if (!config.showBackInterstitial) {
            showExitNotification()
            finish()
            return
        }

        AdUtils.loadAndShowAdWithTimer(
            activity = this@MainActivity,
            adMobManager = adMobManager,
            adUnit = AdIds.getHomeBackInterAdId() ?: "",
            analyticsManager = analyticsManager,
            eventNamePrefix = "exit_int"
        ) {
            showExitNotification()
            finish()
        }
    }



    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("active_tag", activeTag)
    }

    private fun applySystemBars() {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        window.statusBarColor = ContextCompat.getColor(this, R.color.bg_color)
    }

    private fun setupFragments(firstLaunch: Boolean) {
        if (firstLaunch) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, homeFragment, "home")
                .commit()
        } else {
            // Restore fragment reference if needed, but here we use the ones created in class
            // Actually, after recreation, we should find them by tag or just replace
            val fragment = when (activeTag) {
                "home" -> homeFragment
                else -> settingsFragment
            }
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, fragment, activeTag)
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment("home")
                    return@setOnItemSelectedListener true
                }
                R.id.nav_time_stamp -> {
                    switchFragment("settings")
                    return@setOnItemSelectedListener false
                }
                R.id.nav_settings -> {
                    switchFragment("settings")
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
        
        // Correctly set selected item without triggering listener unnecessarily if possible
        binding.bottomNav.selectedItemId = when(activeTag) {
            "settings" -> R.id.nav_settings
            else -> R.id.nav_home
        }
    }
    private fun switchFragment(tag: String) {
        if (tag == activeTag) return

        val fragment = when (tag) {
            "home" -> homeFragment
            else -> settingsFragment
        }

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment, tag)
            .commit()

        activeTag = tag
        updateToolbarForFragment(tag)
    }
    private fun updateToolbarForFragment(tag: String) {
        // Toolbar is now custom, we can hide/show elements based on fragment
        val isSettings = tag == "settings"
        binding.toolbar.visibility = if (isSettings) View.GONE else View.VISIBLE
        binding.divider.visibility = if (isSettings) View.GONE else View.VISIBLE
        binding.tvTitle.text = when (tag) {
            "settings" -> getString(R.string.settings)
            else -> "Live Earth"
        }
        binding.btnBack.visibility = View.GONE

        if (AdMobManager.isPremium) {
            binding.btnPro.visibility = View.GONE
        } else {
            binding.btnPro.visibility = View.VISIBLE
        }
    }
    fun switchFragmentTo(tag: String) {
        binding.bottomNav.selectedItemId = when (tag) {
            "home" -> R.id.nav_home
            "settings" -> R.id.nav_settings
            else -> R.id.nav_home
        }
    }
    private fun setupClicks() {
        binding.btnPro.setClickWithTimeout {
            startActivity(Intent(this, PremiumActivity::class.java))
        }
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (!hasAskedNotificationPermissionThisSession) {
                    hasAskedNotificationPermissionThisSession = true
                    val bottomSheet = com.tf.gpsmapcamera.ui.screens.NotificationPermissionBottomSheet()
                    bottomSheet.show(supportFragmentManager, "NotificationPermissionBottomSheet")
                }
            }
        }
    }

    private fun showExitNotification() {
        val globalConfig = RemoteConfigManager.getGlobalAdRulesConfig()
        if (!globalConfig.enableExitNotification) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val channelId = "${getString(R.string.app_name)}_channel"
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "${getString(R.string.app_name)} Notifications",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(globalConfig.exitNotificationTitle)
            .setContentText(globalConfig.exitNotificationDescription)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            androidx.core.app.NotificationManagerCompat.from(this).notify(1002, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

