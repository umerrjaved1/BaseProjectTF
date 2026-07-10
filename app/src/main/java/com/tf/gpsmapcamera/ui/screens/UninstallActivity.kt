package com.tf.gpsmapcamera.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.facebook.shimmer.ShimmerFrameLayout
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.databinding.ActivityUninstallBinding
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.ui.screens.MainActivity
import com.tf.gpsmapcamera.utils.AdUtils
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UninstallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUninstallBinding

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUninstallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_uninstall")

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        binding.btnBack.setOnClickListener {
            handleBackPress { finish() }
        }

        binding.btnKeepApp.setOnClickListener {
            handleAction {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }

        binding.btnUninstall.setOnClickListener {
            handleAction {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        loadNativeAd()
    }

    private fun handleAction(onComplete: () -> Unit) {
        val config = RemoteConfigManager.getUninstallScreenConfig()
        if (config.showUninstallInterstitial && !AdMobManager.isPremium) {
            AdUtils.loadAndShowAdWithTimer(
                activity = this,
                adMobManager = adMobManager,
                adUnit = AdIds.getInterstitialUninstallAdId(),
                analyticsManager = analyticsManager,
                timeOut = 0L,
                ignoreFrequency = true,
                eventNamePrefix = "uninstall_int",
                onNavigate = onComplete
            )
        } else {
            onComplete()
        }
    }

    private fun handleBackPress(onComplete: () -> Unit) {
        val config = RemoteConfigManager.getUninstallScreenConfig()
        AdUtils.showBackPressInterstitial(
            activity = this,
            adMobManager = adMobManager,
            shouldShow = config.showBackInterstitial,
            adId = AdIds.getUninstallBackInterAdId() ?: "",
            eventNamePrefix = "uninstall_back_int"
        ) {
            onComplete()
        }
    }

    private fun loadNativeAd() {
        if (AdMobManager.isPremium) {
            binding.adContainer.visibility = android.view.View.GONE
            return
        }

        val config = RemoteConfigManager.getUninstallScreenConfig()
        if (!config.showUninstallNative1 && !config.showUninstallNative2) {
            binding.adContainer.visibility = android.view.View.GONE
            return
        }

        binding.adContainer.visibility = android.view.View.VISIBLE

        val adFrame = binding.adContainer.findViewById<FrameLayout>(R.id.adFrame)
        val shimmerFbAd = binding.adContainer.findViewById<ShimmerFrameLayout>(R.id.shimmerFbAd)

        val adId = if (config.showUninstallNative1) AdIds.getUninstallNative1AdId() else AdIds.getUninstallNative2AdId()

        AdUtils.loadAndShowNativeAd(
            adMobManager = adMobManager,
            adUnitId = adId,
            layoutResId = R.layout.small_native_ad_no_cta,
            frameLayout = adFrame,
            shimmerFrameLayout = shimmerFbAd,
            showMedia = false,
            forceLoadNew = true,
            nativeConfig = config.nativeConfig,
            analyticsManager = null,
            eventNamePrefix = "uninstall_native1"
        )
    }
}

