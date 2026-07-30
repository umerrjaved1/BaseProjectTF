package com.professor.baseproject.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.professor.baseproject.ads.AdsController
import com.professor.baseproject.ads.BannerRefresher
import com.professor.baseproject.ads.InterstitialGate
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.R
import com.professor.baseproject.databinding.ActivityMainBinding
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.ui.base.BaseActivity
import com.professor.baseproject.update.AppUpdateManager
import com.professor.baseproject.update.AppUpdateReadyDialogFragment
import com.professor.baseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    companion object {
        var hasAskedNotificationPermissionThisSession = false

        private const val TAG_HOME = "home"
        private const val TAG_SETTINGS = "settings"
        private val ALL_TAGS = setOf(TAG_HOME, TAG_SETTINGS)
        private const val KEY_ACTIVE_TAG = "active_tag"
    }

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    @Inject
    lateinit var adsController: AdsController

    @Inject
    lateinit var interstitialGate: InterstitialGate

    @Inject
    lateinit var bannerRefresher: BannerRefresher

    private lateinit var binding: ActivityMainBinding

    /** Latches once the exit interstitial has been shown, so Back cannot request a second one. */
    private var isExiting = false

    // Fragments are NOT held as fields any more. They were constructed eagerly, so on
    // recreation the FragmentManager restored its own instances while setupFragments()
    // replaced them with these fresh ones — silently discarding all restored state.
    // They are now looked up by tag and created only when absent.
    private var activeTag = TAG_HOME

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Reaching main is the only reliable signal that first-run is over; every
        // individual startup step can be disabled from remote config.
        appPreferences.setBoolean(AppPreferences.IS_FIRST_RUN_COMPLETE, true)

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "MainActivity")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.HOME_VIEW)

        if (savedInstanceState != null) {
            activeTag = savedInstanceState.getString(KEY_ACTIVE_TAG, TAG_HOME) ?: TAG_HOME
        }


        setupFragments()
        setupClicks()
        updateToolbarForFragment(activeTag)

        checkNotificationPermission()

        // App-open resume ads stay suppressed until this point, so one can never appear over the
        // splash, language picker, onboarding or survey. This is the screen where the user is
        // finally past the startup flow. Honours `ad_rules.showAppOpenAdOnResume`.
        adsController.allowResumeAds()

        appUpdateManager.addOnFlexibleDownloadCompleteListener(this) {
            showFlexibleUpdateReadyDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.handleOnResume(this, updateLauncher)
        bannerRefresher.resumeAll()
    }

    override fun onPause() {
        // Stops the 30s refresh timers while this screen is not in front. Without it a
        // backgrounded screen keeps requesting banners nobody can see.
        bannerRefresher.pauseAll()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.removeOnFlexibleDownloadCompleteListener(this)
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
        if (activeTag != TAG_HOME) {
            switchFragmentTo(TAG_HOME)
            return
        }
        exitWithAd()
    }

    /**
     * Back on home: interstitial first (the `exit_inter` unit), then exit.
     *
     * Still time-capped, so backing out of a screen the user only just arrived at cannot chain an
     * ad onto one they have just dismissed. The exit itself hangs off the gate's callback, which
     * always runs - so a missing or unfilled ad can never trap the user on this screen.
     */
    private fun exitWithAd() {
        if (isExiting) return
        isExiting = true
        interstitialGate.showOnExit(this) {
            showExitNotification()
            finish()
        }
    }



    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ACTIVE_TAG, activeTag)
    }

    // applySystemBars() removed: StatusBarUtils.applyEdgeToEdge() (invoked for every
    // Activity from MyApp) now owns bar appearance, and it derives light/dark icons from
    // the active theme instead of each screen hand-rolling it.

    private fun newFragmentFor(tag: String) = when (tag) {
        TAG_SETTINGS -> com.professor.baseproject.ui.fragments.SettingsFragment()
        else -> com.professor.baseproject.ui.fragments.HomeFragment()
    }

    /**
     * Adds each tab lazily and uses hide/show, so switching tabs preserves scroll
     * position and view state instead of tearing the fragment down as `replace()` did.
     */
    private fun showFragment(tag: String) {
        val fm = supportFragmentManager
        val target = fm.findFragmentByTag(tag) ?: newFragmentFor(tag).also {
            fm.beginTransaction()
                .add(binding.fragmentContainer.id, it, tag)
                .commitNow()
        }

        fm.beginTransaction().apply {
            for (other in fm.fragments) {
                if (other !== target && other.tag in ALL_TAGS) hide(other)
            }
            show(target)
        }.commit()

        activeTag = tag
        updateToolbarForFragment(tag)
    }

    private fun setupFragments() {
        showFragment(activeTag)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(TAG_HOME)
                    true
                }
                // nav_time_stamp is a leftover from the GPS-camera fork with no
                // destination of its own; it is handled here only so the tap is not
                // swallowed. Give it a real screen or remove the menu item.
                R.id.nav_settings, R.id.nav_time_stamp -> {
                    switchFragment(TAG_SETTINGS)
                    true
                }
                else -> false
            }
        }

        // Assign without re-entering the listener: showFragment() already ran above,
        // and the old code's assignment re-triggered a second transaction.
        val desired = if (activeTag == TAG_SETTINGS) R.id.nav_settings else R.id.nav_home
        if (binding.bottomNav.selectedItemId != desired) {
            binding.bottomNav.selectedItemId = desired
        }
    }

    private fun switchFragment(tag: String) {
        if (tag == activeTag) return
        showFragment(tag)
    }
    private fun updateToolbarForFragment(tag: String) {
        // Toolbar is now custom, we can hide/show elements based on fragment
        val isSettings = tag == TAG_SETTINGS
        binding.toolbar.visibility = if (isSettings) View.GONE else View.VISIBLE
        binding.divider.visibility = if (isSettings) View.GONE else View.VISIBLE
        binding.tvTitle.text = when (tag) {
            TAG_SETTINGS -> getString(R.string.settings)
            // Was the hardcoded literal "Live Earth" — unlocalised, and a rename
            // touchpoint hiding in Kotlin rather than in strings.xml.
            else -> getString(R.string.app_name)
        }
        binding.btnBack.visibility = View.GONE

        binding.btnPro.visibility =
            if (appPreferences.getBoolean(AppPreferences.IS_PREMIUM)) View.GONE else View.VISIBLE
    }
    fun switchFragmentTo(tag: String) {
        binding.bottomNav.selectedItemId = when (tag) {
            TAG_HOME -> R.id.nav_home
            TAG_SETTINGS -> R.id.nav_settings
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
                    val bottomSheet = com.professor.baseproject.ui.screens.NotificationPermissionBottomSheet()
                    bottomSheet.show(supportFragmentManager, "NotificationPermissionBottomSheet")
                }
            }
        }
    }

    private fun showExitNotification() {
        val adRules = RemoteConfigManager.getAdRules()
        if (!adRules.enableExitNotification) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val channelId = "${getString(R.string.app_name)}_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager

        val channel = android.app.NotificationChannel(
            channelId,
            "${getString(R.string.app_name)} Notifications",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

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
            .setContentTitle(adRules.exitNotificationTitle)
            .setContentText(adRules.exitNotificationDescription)
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

