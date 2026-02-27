package com.professor.baseproject.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.professor.baseproject.R
import com.professor.baseproject.databinding.FragmentSettingsBinding
import com.professor.baseproject.ui.screens.LanguageActivity
import com.professor.baseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @javax.inject.Inject
    lateinit var appPreferences: com.professor.baseproject.app.AppPreferences

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    var appUrl = ""
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
        listeners()
        appUrl = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLanguage()
    }

    private fun updateCurrentLanguage() {
        val languages = getAvailableLanguages()
        val savedLanguageId =
            appPreferences.getInt(com.professor.baseproject.app.AppPreferences.LANGUAGE_ID)

        val selectedLanguage = languages.find { it.id == savedLanguageId }
            ?: getDefaultLanguage(languages)

        binding.tvCurrentLanguage.text = selectedLanguage.name
    }

    private fun getDefaultLanguage(languages: List<com.professor.baseproject.model.LanguageModel>): com.professor.baseproject.model.LanguageModel {
        val deviceLangCode = java.util.Locale.getDefault().language
        return languages.find { it.code == deviceLangCode }
            ?: languages.find { it.code == "en" } ?: languages.first()
    }

    private fun getAvailableLanguages(): List<com.professor.baseproject.model.LanguageModel> {
        return listOf(
            com.professor.baseproject.model.LanguageModel(
                1,
                R.drawable.flag_arabic,
                getString(R.string.arabic),
                "ar"
            ),
            com.professor.baseproject.model.LanguageModel(
                2,
                R.drawable.flag_english,
                getString(R.string.english),
                "en"
            ),
            com.professor.baseproject.model.LanguageModel(
                3,
                R.drawable.flag_spanish,
                getString(R.string.spanish),
                "es"
            ),
            com.professor.baseproject.model.LanguageModel(
                4,
                R.drawable.flag_indonesia,
                getString(R.string.indonesian),
                "in"
            ),
            com.professor.baseproject.model.LanguageModel(
                6,
                R.drawable.flag_persian,
                getString(R.string.persian),
                "fa"
            ),
            com.professor.baseproject.model.LanguageModel(
                7,
                R.drawable.flag_hindi,
                getString(R.string.hindi),
                "hi"
            ),
            com.professor.baseproject.model.LanguageModel(
                8,
                R.drawable.flag_russia,
                getString(R.string.russian),
                "ru"
            ),
            com.professor.baseproject.model.LanguageModel(
                9,
                R.drawable.flag_portuguese,
                getString(R.string.portuguese),
                "pt"
            ),
            com.professor.baseproject.model.LanguageModel(
                10,
                R.drawable.flag_bangla,
                getString(R.string.bengali),
                "bn"
            ),
            com.professor.baseproject.model.LanguageModel(
                11,
                R.drawable.flag_turkey,
                getString(R.string.turkish),
                "tr"
            )
        )
    }

    private fun listeners() {


        binding.cardRate.setClickWithTimeout {
            showRateUsDialog()
        }

        binding.cardFeedback.setClickWithTimeout {
            openStorePage()
        }

        binding.cardShare.setClickWithTimeout {
            shareApp()
        }

        binding.cardPrivacy.setClickWithTimeout {
            val url = "https://ranaumer1.github.io/privacy_policy_apps/privacy_policy_pdf_converter"
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }

        binding.cardLanguage.setClickWithTimeout {
            startActivity(Intent(requireContext(), LanguageActivity::class.java))
        }
    }

    private fun openStorePage() {

        try {
            startActivity(Intent(Intent.ACTION_VIEW, appUrl.toUri()))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, appUrl.toUri()))
        }
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_this_app) + "\n" + appUrl)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name))
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

    private fun showRateUsDialog() {
        val bottomSheet = com.professor.baseproject.ui.bottomsheets.RateUsBottomSheet {
            openStorePage()
        }
        bottomSheet.show(parentFragmentManager, "RateUsBottomSheet")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
