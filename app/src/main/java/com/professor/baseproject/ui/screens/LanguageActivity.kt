package com.professor.baseproject.ui.screens

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.professor.baseproject.adapter.LanguageAdapter
import com.professor.baseproject.ads.AdSlotStyle
import com.professor.baseproject.ads.AdsSlot
import com.professor.baseproject.ads.NativePlacement
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.model.LanguageListItem
import com.professor.baseproject.model.LanguageModel
import com.professor.baseproject.ui.viewmodel.LanguageNav
import com.professor.baseproject.ui.viewmodel.LanguageViewModel
import com.professor.baseproject.utils.StartupNavigationManager
import com.professor.baseproject.utils.setClickWithTimeout
import com.professor.baseproject.utils.startShakeAnimation
import com.professor.baseproject.R
import com.professor.baseproject.databinding.ActivityLanguageBinding
import com.professor.baseproject.databinding.DialogExitBinding
import com.umer_tf.ads.domain.consent.AdsConsentManager
import com.umer_tf.ads.domain.core.AdMobManager
import com.umer_tf.ads.domain.viewmodel.AdViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageAdapter

    private val viewModel: LanguageViewModel by viewModels()

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var adsSlot: AdsSlot

    @Inject
    lateinit var adMobManager: AdMobManager

    lateinit var adViewModel: AdViewModel

    lateinit var exitDialog: Dialog
    var isFromStart = false

    //    private var mSelectedLanguage = "en"

    private val TAG = "language_activity"

    override fun onDestroy() {
        if (::exitDialog.isInitialized && exitDialog.isShowing) {
            exitDialog.dismiss()
        }
        // Stops the banner refresh timer for this slot; without it the timer keeps requesting
        // after the screen is gone.
        adsSlot.release(binding.adSlot)
        super.onDestroy()
    }

    /**
     * Small native without media, with a banner fallback when native does not fill. Uses the
     * `language_native` unit from `ad_ids`; [AdsSlot] collapses the slot when `ad_rules.showAds`
     * is off.
     */
    private fun loadAd() {
        adsSlot.show(
            activity = this,
            container = binding.adSlot,
            placement = NativePlacement.LANGUAGE,
            style = AdSlotStyle.SMALL_NO_MEDIA
        )
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFromStart) {
                    exitDialog.show()
                } else {
                    finish()
                }
            }
        })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.e("TAG", "onCreate: language")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.LNG_SCR_VIEW)




        isFromStart = intent.getBooleanExtra(Constants.EXTRA_LANGUAGE_FROM_START, false)

        initAdapter()
        initClickListeners()

        loadAd()

        loadExitDialog()
        setupBackPressHandler()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.navigate.collect { nav ->
                    when (nav) {
                        LanguageNav.ONBOARDING -> {
                            StartupNavigationManager.navigateNext(
                                activity = this@LanguageActivity,
                                currentStep = StartupNavigationManager.Step.LANGUAGE,
                                appPreferences = appPreferences,
                                onBeforeNavigate = {
                                    val code = appPreferences.getString(AppPreferences.LANGUAGE_CODE)
                                    if (code.isNotEmpty()) {
                                        val localeList = LocaleListCompat.forLanguageTags(code)
                                        AppCompatDelegate.setApplicationLocales(localeList)
                                    }
                                }
                            )
                        }

                        LanguageNav.MAIN -> {
                            val code = appPreferences.getString(AppPreferences.LANGUAGE_CODE)
                            if (code.isNotEmpty()) {
                                val localeList = LocaleListCompat.forLanguageTags(code)
                                AppCompatDelegate.setApplicationLocales(localeList)
                            }
                            startActivity(
                                Intent(this@LanguageActivity, MainActivity::class.java).addFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            finish()
                        }
                    }
                }
            }
        }


//      binding.ivDone.isEnabled = adapter.selectedLanguage != null
        binding.shimmerBtn.startShakeAnimation(this)
    }

    private fun loadExitDialog() {
        exitDialog = Dialog(this)
        val binding = DialogExitBinding.inflate(layoutInflater)
        exitDialog.setContentView(binding.root)
        exitDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layoutParams = binding.root.layoutParams
        val width = resources.displayMetrics.widthPixels
        layoutParams.width = (width * 0.9).toInt()
        binding.root.layoutParams = layoutParams

        exitDialog.setCancelable(true)
        binding.btnNo.setClickWithTimeout { exitDialog.dismiss() }
        binding.btnYes.setClickWithTimeout {
            exitDialog.dismiss()
            finishAffinity()
        }
    }

    private fun initClickListeners() {


        binding.btnDone.setClickWithTimeout {
            val popAnim = AnimationUtils.loadAnimation(this, R.anim.pop_button)
            binding.btnDone.startAnimation(popAnim)
            binding.btnDone.isEnabled = false // Prevent double taps

            analyticsManager.sendAnalytics(
                AnalyticsManager.Action.CLICKED, "btn_select_language_done"
            )
            analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.LNG_SCR_NEXT)

            val selectedLanguage = adapter.selectedLanguageModel
            if (selectedLanguage == null) {
                Toast.makeText(
                    this, getString(R.string.please_select_a_language), Toast.LENGTH_SHORT
                ).show()
                binding.btnDone.isEnabled = true
                return@setClickWithTimeout
            }

            viewModel.setSelectedLanguage(selectedLanguage)
            viewModel.onDoneClicked()
        }
    }

    private fun onLanguageSelected(selectedLang: LanguageModel) {
        binding.btnDone.isEnabled = true
        binding.btnDone.alpha = 1.0f
        binding.shimmerBtn.startShimmer()
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_button)
        binding.btnDone.startAnimation(pulseAnim)
        binding.btnDone.text = getString(
            when (selectedLang.id) {
                1 -> R.string.done_in_arabic
                2 -> R.string.done_in_english
                3 -> R.string.done_in_spanish
                4 -> R.string.done_in_indonesian
                5 -> R.string.done_in_french
                6 -> R.string.done_in_persian
                7 -> R.string.done_in_hindi
                8 -> R.string.done_in_russian
                9 -> R.string.done_in_portuguese
                10 -> R.string.done_in_bengali
                11 -> R.string.done_in_turkish
                else -> R.string.done_in_english
            }
        )

        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.LNG_SELECTED)
    }


    /**
     * Resolves the language to pre-select. There is **always** a selection now:
     *  1. a previously saved choice, else
     *  2. the device locale, if this app ships it, else
     *  3. English.
     *
     * Two consequences, both intended: Done is enabled immediately instead of requiring a
     * tap to become usable, and the attention-grabbing hand pointer never appears — it was
     * gated on `selectedLanguageModel == null`, which can no longer happen.
     */
    private fun resolveDefaultLanguage(languageList: List<LanguageModel>): LanguageModel {
        val savedId = appPreferences.getInt(AppPreferences.Companion.LANGUAGE_ID)
        languageList.firstOrNull { it.id == savedId }?.let { return it }

        val deviceLanguage = java.util.Locale.getDefault().language.lowercase()
        languageList.firstOrNull { it.code.lowercase() == deviceLanguage }?.let { return it }

        return languageList.firstOrNull { it.code.equals("en", ignoreCase = true) }
            ?: languageList.first()
    }

    private fun initAdapter() {
        val languageList = getLanguageList(this)
        val savedLanguage = resolveDefaultLanguage(languageList)

        val displayList = mutableListOf<LanguageListItem>()

        displayList.add(LanguageListItem.Header("Default"))
        displayList.add(LanguageListItem.Language(savedLanguage))

        displayList.add(LanguageListItem.Header("All Languages"))
        languageList.filterNot { it == savedLanguage }
            .forEach { displayList.add(LanguageListItem.Language(it)) }

        adapter = LanguageAdapter(savedLanguage) {
            viewModel.setSelectedLanguage(it.model)
            onLanguageSelected(it.model)
        }

        viewModel.setSelectedLanguage(savedLanguage)
        onLanguageSelected(savedLanguage)

        binding.rvLanguage.layoutManager = LinearLayoutManager(this)
        binding.rvLanguage.adapter = adapter
        adapter.submitList(displayList)
    }


    private fun getLanguageList(activity: Activity): List<LanguageModel> {
        val languageModelList: MutableList<LanguageModel> = ArrayList()
        languageModelList.add(
            LanguageModel(
                2,
                R.drawable.flag_english,
                activity.resources.getString(R.string.english),
                "en",

                )
        )
        languageModelList.add(
            LanguageModel(
                1,
                R.drawable.flag_arabic,
                activity.resources.getString(R.string.arabic),
                "ar",

                )
        )

        languageModelList.add(
            LanguageModel(
                3,
                R.drawable.flag_spanish,
                activity.resources.getString(R.string.spanish),
                "es",

                )
        )


        languageModelList.add(
            LanguageModel(
                4,
                R.drawable.flag_indonesia,
                activity.resources.getString(R.string.indonesian),
                "in",

                )
        )
        languageModelList.add(
            LanguageModel(
                6,
                R.drawable.flag_persian,
                activity.resources.getString(R.string.persian),
                "fa",

                )
        )
        languageModelList.add(
            LanguageModel(
                7,
                R.drawable.flag_hindi,
                activity.resources.getString(R.string.hindi),
                "hi",

                )
        )

        languageModelList.add(
            LanguageModel(
                8,
                R.drawable.flag_russia,
                activity.resources.getString(R.string.russian),
                "ru",

                )
        )
        languageModelList.add(
            LanguageModel(
                9,
                R.drawable.flag_portuguese,
                activity.resources.getString(R.string.portuguese),
                "pt",

                )
        )
        languageModelList.add(
            LanguageModel(
                10,
                R.drawable.flag_bangla,
                activity.resources.getString(R.string.bengali),
                "bn",

                )
        )
        languageModelList.add(
            LanguageModel(
                11,
                R.drawable.flag_turkey,
                activity.resources.getString(R.string.turkish),
                "tr",

                )
        )

        return languageModelList
    }

}
