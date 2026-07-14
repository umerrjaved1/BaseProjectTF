package com.tf.gpsmapcamera.ui.screens

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.umer_tf.ads.domain.ads.native_ad.NativeAdBuilder
import com.umer_tf.ads.domain.core.AdMobManager
import com.tf.gpsmapcamera.adapter.LanguageAdapter
import com.tf.gpsmapcamera.app.AdIds
import com.tf.gpsmapcamera.app.AnalyticsManager
import com.tf.gpsmapcamera.app.AppPreferences
import com.tf.gpsmapcamera.constants.Constants
import com.tf.gpsmapcamera.model.LanguageListItem
import com.tf.gpsmapcamera.model.LanguageModel
import com.tf.gpsmapcamera.remoteconfig.RemoteConfigManager
import com.tf.gpsmapcamera.ui.viewmodel.LanguageNav
import com.tf.gpsmapcamera.ui.viewmodel.LanguageViewModel
import com.tf.gpsmapcamera.utils.AdFrequencyControl
import com.tf.gpsmapcamera.utils.AdUnitFrequencyController
import com.tf.gpsmapcamera.utils.AdUtils
import com.tf.gpsmapcamera.utils.setClickWithTimeout
import com.tf.gpsmapcamera.utils.startShakeAnimation
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.databinding.ActivityLanguageBinding
import com.tf.gpsmapcamera.databinding.DialogExitBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageAdapter
    private var hasShownSecondAd = false

    private val viewModel: LanguageViewModel by viewModels()

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences
    lateinit var exitDialog: Dialog
    var isFromStart = false

    //    private var mSelectedLanguage = "en"

    private val TAG = "language_activity"

    override fun onDestroy() {
        if (::exitDialog.isInitialized && exitDialog.isShowing) {
            exitDialog.dismiss()
        }
        super.onDestroy()
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


        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars =
            true // Light icons = false, Dark icons = true
        window.statusBarColor = ContextCompat.getColor(this, R.color.bg_color)


        isFromStart = intent.getBooleanExtra(Constants.EXTRA_LANGUAGE_FROM_START, false)
        loadNativeAd()

        initAdapter()
        initClickListeners()

        loadExitDialog()
        setupBackPressHandler()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.navigate.collect { nav ->
                    when (nav) {
                        LanguageNav.ONBOARDING -> {
                            com.tf.gpsmapcamera.utils.StartupNavigationManager.navigateNextWithAd(
                                activity = this@LanguageActivity,
                                currentStep = com.tf.gpsmapcamera.utils.StartupNavigationManager.Step.LANGUAGE,
                                appPreferences = appPreferences,
                                adMobManager = adMobManager,
                                analyticsManager = analyticsManager,
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
        binding.shimmerBtn.startShakeAnimation(lifecycleScope)
    }

    private fun loadExitDialog() {
        exitDialog = Dialog(this)
        val binding = DialogExitBinding.inflate(layoutInflater)
        exitDialog.setContentView(binding.root)
        exitDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadExitBannerAd(binding)

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

    private fun loadExitBannerAd(binding: DialogExitBinding) {
        if (AdMobManager.isPremium || !RemoteConfigManager.getHomeScreenConfig().showExitBanner) {
            binding.includeAd.root.visibility = View.GONE
            return
        }
        if (!AdFrequencyControl.canShowAd(this, AdUnitFrequencyController.UNIT_BANNER)) {
            binding.includeAd.root.visibility = View.GONE
            return
        }
        adMobManager.bannerAdLoader.showMemRecBanner(
            this,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd,
            AdIds.getBannerAdIdExit()
        )
        AdFrequencyControl.recordAdShown(this, AdUnitFrequencyController.UNIT_BANNER)
    }

    private fun loadNativeAd(forceLoadNew: Boolean = !isFromStart) {
        val config = RemoteConfigManager.getLanguageScreenConfig()
        val showNative1 = !hasShownSecondAd && config.showLanguageNative1
        val showNative2 = hasShownSecondAd && config.showLanguageNative2

        if (!showNative1 && !showNative2) {
            binding.includeAd.root.visibility = View.GONE
            return
        }

        val prefix = if (hasShownSecondAd) "lng_scr_native2" else "lng_scr_native1"
        val adId = if (hasShownSecondAd) AdIds.getNativeLanguage2AdId() else AdIds.getNativeLanguageAdId()
        AdUtils.loadAndShowNativeAd(
            adMobManager = adMobManager,
            adUnitId = adId,
            layoutResId = R.layout.native_ad_large,
            frameLayout = binding.includeAd.adFrame,
            shimmerFrameLayout = binding.includeAd.shimmerFbAd,
            showMedia = true,
            forceLoadNew = forceLoadNew,
            analyticsManager = analyticsManager,
            eventNamePrefix = prefix
        ) {
            // Preload the second ad after the first ad finishes loading
            if (!hasShownSecondAd && config.showLanguageNative2) {
                adMobManager.nativeAdLoader.loadAd(AdIds.getNativeLanguage2AdId())
            }
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

        // Force load the second native ad when a language is selected (only once)
        if (!hasShownSecondAd) {
            hasShownSecondAd = true
            loadNativeAd(forceLoadNew = true)
        }
    }


    private fun initAdapter() {
        val languageList = getLanguageList(this)
        val savedLanguageId = appPreferences.getInt(AppPreferences.Companion.LANGUAGE_ID)
        val savedLanguage = languageList.find { it.id == savedLanguageId }

        val displayList = mutableListOf<LanguageListItem>()

        if (savedLanguage != null) {
            displayList.add(LanguageListItem.Header("Default"))
            displayList.add(LanguageListItem.Language(savedLanguage))
        }

        displayList.add(LanguageListItem.Header("All Languages"))
        languageList.filterNot { it == savedLanguage }
            .forEach { displayList.add(LanguageListItem.Language(it)) }

        adapter = LanguageAdapter(savedLanguage) {
            viewModel.setSelectedLanguage(it.model)
            onLanguageSelected(it.model)
        }

        if (savedLanguage != null) {
            viewModel.setSelectedLanguage(savedLanguage)
            onLanguageSelected(savedLanguage)
        } else {
            binding.btnDone.isEnabled = false
            binding.btnDone.alpha = 0.5f
            binding.shimmerBtn.stopShimmer()
        }

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
