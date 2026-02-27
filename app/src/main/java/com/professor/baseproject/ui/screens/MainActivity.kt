package com.professor.baseproject.ui.screens

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.baseproject.R
import com.professor.baseproject.app.AdIds
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.databinding.ActivityMainBinding
import com.professor.baseproject.databinding.DialogExitBinding
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.professor.baseproject.ui.fragments.HomeFragment
import com.professor.baseproject.ui.fragments.SettingsFragment
import com.professor.baseproject.utils.NetworkChangeReceiver
import com.professor.baseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences


    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var binding: ActivityMainBinding
    private lateinit var exitDialog: Dialog
    private val notificationPermissionBottomSheet = NotificationPermissionBottomSheet()


    private val homeIcons = Pair(R.drawable.ic_home_filled, R.drawable.ic_home)
    private val convertedIcons = Pair(R.drawable.ic_converted_filled, R.drawable.ic_converted)
    private val settingIcons = Pair(R.drawable.ic_setting_filled, R.drawable.ic_setting)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(appPreferences.getString(AppPreferences.LANGUAGE_CODE))
        )
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)

        loadAd()
        init()
        listeners()
        loadExitDialog()
    }

    override fun onResume() {
        super.onResume()
        networkChangeReceiver = NetworkChangeReceiver(this)
        registerReceiver(
            networkChangeReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkChangeReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun init() {
        binding.ivRemoveAd.isVisible = !RemoteConfigManager.getDisableAds()
        requestPermission()
        setActiveTab(binding.tvHome)
        loadFragment(HomeFragment(), getString(R.string.splash_title))
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        updateToolbar(fragment, title)
    }

    private fun updateToolbar(fragment: Fragment, title: String) {
        binding.tvTitle.text = title
        if (fragment is HomeFragment) {
            binding.ivBack.visibility = android.view.View.GONE
        } else {
            binding.ivBack.visibility = android.view.View.VISIBLE
        }
    }


    fun setActiveTab(activeTab: AppCompatTextView) {
        val tabs = listOf(binding.tvHome, binding.tvConverted, binding.tvSetting)
        val activeColor = ContextCompat.getColor(this, R.color.primary_color)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_color_secondary)

        tabs.forEach { tab ->
            val isActive = tab == activeTab
            tab.setTextColor(if (isActive) activeColor else inactiveColor)

            // Update icons based on selected state
            when (tab.id) {
                R.id.tv_home -> {
                    val iconRes = if (isActive) homeIcons.first else homeIcons.second
                    tab.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRes, 0, 0)
                }

                R.id.tv_converted -> {
                    val iconRes = if (isActive) convertedIcons.first else convertedIcons.second
                    tab.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRes, 0, 0)
                }

                R.id.tv_setting -> {
                    val iconRes = if (isActive) settingIcons.first else settingIcons.second
                    tab.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRes, 0, 0)
                }
            }
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionBottomSheet.show(
                supportFragmentManager,
                "NotificationPermissionBottomSheet"
            )
        }
    }


    private fun listeners() {


        binding.ivRemoveAd.setClickWithTimeout {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, TAG + "_premium")
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        binding.ivBack.setClickWithTimeout {
            onBackPressed()
        }


        binding.tvHome.setClickWithTimeout {
            setActiveTab(binding.tvHome)
            loadFragment(HomeFragment(), getString(R.string.splash_title))
        }


        binding.tvSetting.setClickWithTimeout {
            setActiveTab(binding.tvSetting)
            loadFragment(SettingsFragment(), getString(R.string.settings))
        }
    }

    private fun loadAd() {

        adMobManager.bannerAdLoader.showCollapsableBanner(
            this,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd,
            AdIds.getBannerHomeAdId(), false
        )
    }

    private fun loadExitBannerAd(binding: DialogExitBinding) {
        adMobManager.bannerAdLoader.showMemRecBanner(
            this,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd,
            AdIds.getBannerAdIdExit()
        )
    }


    private fun backPressed() {
        exitDialog.show()
    }


    private fun loadExitDialog() {
        exitDialog = Dialog(this).apply {
            val exitBinding = DialogExitBinding.inflate(layoutInflater)
            setContentView(exitBinding.root)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            loadExitBannerAd(exitBinding)
            exitBinding.root.layoutParams.width =
                (resources.displayMetrics.widthPixels * 0.9).toInt()
            setCancelable(true)
            exitBinding.btnNo.setClickWithTimeout { dismiss() }
            exitBinding.btnYes.setClickWithTimeout {
                dismiss()
                finishAffinity()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Notification permission granted
        }
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is HomeFragment) {
            setActiveTab(binding.tvHome)
            loadFragment(HomeFragment(), getString(R.string.splash_title))
        } else {
            analyticsManager.sendAnalytics("clicked", "$TAG _back_btn")
            backPressed()
        }
    }

    companion object {
        const val NOTIFICATION_PERMISSION_CODE = 1001
    }

}
