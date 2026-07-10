package com.tf.gpsmapcamera.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.databinding.FragmentSettingsBinding
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.ui.base.BaseFragment
import com.tf.gpsmapcamera.ui.bottomsheets.RateUsBottomSheet
import com.tf.gpsmapcamera.ui.screens.PremiumActivity
import com.tf.gpsmapcamera.ui.screens.UninstallActivity
import com.tf.gpsmapcamera.utils.AdUtils
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(
    FragmentSettingsBinding::inflate
) {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var adMobManager: AdMobManager


    private val appUrl by lazy {
        "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindClicks()
        setupVersionInfo()

    }



    private fun setupVersionInfo() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.tvVersion.text = "Version v${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.tvVersion.text = "Version v1.0.0"
        }
    }

    private fun bindClicks() {
        binding.cardPro.setOnClickListener {
            startActivity(Intent(requireContext(), PremiumActivity::class.java))
        }

        binding.rowRate.setOnClickListener {
            RateUsBottomSheet { openStorePage() }.show(parentFragmentManager, "RateUsBottomSheet")
        }

        binding.rowShare.setOnClickListener {
            shareApp()
        }

        binding.rowPrivacy.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://ranaumer1.github.io/privacy_policy_apps/privacy_policy_doc_viewer.html".toUri()
                )
            )
        }

        binding.rowUninstall.setOnClickListener {
            uninstallApp()
        }
    }

    private fun uninstallApp() {
        startActivity(Intent(requireContext(), UninstallActivity::class.java))
    }

    private fun openStorePage() {
        startActivity(Intent(Intent.ACTION_VIEW, appUrl.toUri()))
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, getString(com.tf.gpsmapcamera.R.string.share_this_app) + "\n" + appUrl)
            putExtra(Intent.EXTRA_SUBJECT, getString(com.tf.gpsmapcamera.R.string.app_name))
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

}
