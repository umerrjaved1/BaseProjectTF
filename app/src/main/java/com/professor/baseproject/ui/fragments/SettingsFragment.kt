package com.professor.baseproject.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.professor.baseproject.BuildConfig
import com.professor.baseproject.R
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.databinding.FragmentSettingsBinding
import com.professor.baseproject.ui.base.BaseFragment
import com.professor.baseproject.ui.bottomsheets.RateUsBottomSheet
import com.professor.baseproject.ui.screens.PremiumActivity
import com.professor.baseproject.ui.screens.UninstallActivity
import com.professor.baseproject.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(
    FragmentSettingsBinding::inflate
) {

    @Inject
    lateinit var appPreferences: AppPreferences

    private val settingsViewModel: SettingsViewModel by viewModels()

    private val appUrl by lazy {
        "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindClicks()
        setupVersionInfo()
        setupDarkMode()
    }

    /**
     * Wires the dark-mode switch. `SettingsViewModel.toggleTheme()` already existed and
     * worked, along with values-night colours/themes and the mode being applied in
     * MyApp.onCreate — but nothing ever called it, so dark mode was unreachable.
     *
     * Collected on viewLifecycleOwner (not the fragment's lifecycleScope) so the
     * collector stops when the view is destroyed and never touches a null binding.
     */
    private fun setupDarkMode() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.isDarkMode.collect { isDark ->
                    if (binding.switchDarkMode.isChecked != isDark) {
                        binding.switchDarkMode.isChecked = isDark
                    }
                }
            }
        }

        // The row owns the click; the switch is not independently clickable, so there is
        // no way to get the row and the switch into disagreeing states.
        binding.rowDarkMode.setOnClickListener {
            settingsViewModel.toggleTheme(!binding.switchDarkMode.isChecked)
        }
    }



    private fun setupVersionInfo() {
        // Localised, and sourced from the installed package rather than a hardcoded
        // literal that has to be remembered on every release.
        val versionName = runCatching {
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
                .versionName
        }.getOrNull() ?: BuildConfig.VERSION_NAME

        binding.tvVersion.text = getString(R.string.version_name, versionName)
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
            // From strings.xml (@string/privacy_policy_url), not a hardcoded URL. The
            // previous literal pointed at a *document-viewer* privacy policy inherited
            // from another fork — legally wrong for every app built on this base.
            startActivity(Intent(Intent.ACTION_VIEW, getString(R.string.privacy_policy_url).toUri()))
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
            putExtra(Intent.EXTRA_TEXT, getString(com.professor.baseproject.R.string.share_this_app) + "\n" + appUrl)
            putExtra(Intent.EXTRA_SUBJECT, getString(com.professor.baseproject.R.string.app_name))
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

}
