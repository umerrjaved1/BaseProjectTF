package com.tf.phonecleaner.booster.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tf.phonecleaner.booster.app.AppPreferences
import com.tf.phonecleaner.booster.databinding.FragmentSettingsBinding
import com.tf.phonecleaner.booster.ui.bottomsheets.RateUsBottomSheet
import com.tf.phonecleaner.booster.ui.screens.LanguageActivity
import com.tf.phonecleaner.booster.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private val appUrl by lazy {
        "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindClicks()
        bindState()
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

    override fun onResume() {
        super.onResume()
        binding.tvCurrentLanguage.text = appPreferences.getString(AppPreferences.LANGUAGE_CODE, "en")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isDarkMode.collect { enabled ->
                        binding.switchDarkMode.isChecked = enabled
                    }
                }
                launch {
                    viewModel.usedStorageText.collect { used ->
                        binding.tvHistorySize.text = used
                    }
                }
            }
        }
    }

    private fun bindClicks() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleTheme(isChecked)
        }

        binding.rowLanguage.setOnClickListener {
            startActivity(Intent(requireContext(), LanguageActivity::class.java))
        }

        binding.rowRate.setOnClickListener {
            RateUsBottomSheet { openStorePage() }.show(parentFragmentManager, "RateUsBottomSheet")
        }

        binding.rowPrivacy.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://ranaumer1.github.io/privacy_policy_apps/privacy_policy_doc_viewer.html".toUri()
                )
            )
        }

        binding.rowShare.setOnClickListener {
            shareApp()
        }

        binding.rowClearHistory.setOnClickListener {
            viewModel.clearHistory()
            android.widget.Toast.makeText(requireContext(), getString(com.tf.phonecleaner.booster.R.string.history_cleared), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStorePage() {
        startActivity(Intent(Intent.ACTION_VIEW, appUrl.toUri()))
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, getString(com.tf.phonecleaner.booster.R.string.share_this_app) + "\n" + appUrl)
            putExtra(Intent.EXTRA_SUBJECT, getString(com.tf.phonecleaner.booster.R.string.app_name))
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
