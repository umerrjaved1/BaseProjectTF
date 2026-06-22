package com.tf.phonecleaner.booster.ui.screens.compose

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tf.phonecleaner.booster.R
import com.tf.phonecleaner.booster.app.AdIds
import com.tf.phonecleaner.booster.model.LanguageModel
import com.tf.phonecleaner.booster.remoteconfig.RemoteConfigManager
import com.tf.phonecleaner.booster.ui.components.NativeAdView
import com.tf.phonecleaner.booster.ui.viewmodel.LanguageNav
import com.tf.phonecleaner.booster.ui.viewmodel.LanguageViewModel
import com.tf.phonecleaner.booster.utils.AdFrequencyControl
import com.tf.phonecleaner.booster.utils.AdUnitFrequencyController
import com.tf.phonecleaner.booster.utils.AdUtils
import com.umer_tf.ads.domain.core.AdMobManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onNavigateNext: () -> Unit,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? AppCompatActivity
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigate.collect { nav ->
            if (nav == LanguageNav.ONBOARDING || nav == LanguageNav.MAIN) {
                onNavigateNext()
            }
        }
    }

    val languageList = remember { getLanguageList(context) }
    val config = RemoteConfigManager.getLanguageScreenConfig()

    var showSecondAd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Language",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (selectedLanguage == null) return@TextButton
                            if (config.languageScreenInterstitialStrategy == 1 && config.showLanguageInterstitial && activity != null) {
                                if (!AdMobManager.isPremium && AdFrequencyControl.canShowAd(activity, AdUnitFrequencyController.UNIT_INTERSTITIAL)) {
                                    AdUtils.loadAndShowAdWithTimer(
                                        activity = activity,
                                        adMobManager = viewModel.adMobManager,
                                        adUnit = AdIds.getInterstitialLanguageID(),
                                        analyticsManager = viewModel.analyticsManager,
                                        eventNamePrefix = "lng_int"
                                    ) {
                                        viewModel.onDoneClicked()
                                    }
                                } else {
                                    viewModel.onDoneClicked()
                                }
                            } else {
                                viewModel.onDoneClicked()
                            }
                        },
                        enabled = selectedLanguage != null,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2563EB))
                    ) {
                        Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(languageList) { lang ->
                    LanguageItemCard(
                        language = lang,
                        isSelected = selectedLanguage?.id == lang.id,
                        onClick = {
                            viewModel.setSelectedLanguage(lang)
                            if (!showSecondAd) {
                                showSecondAd = true
                            }
                        }
                    )
                }
            }

            // Native Ad
            if (!AdMobManager.isPremium) {
                val showAd = if (showSecondAd) config.showLanguageNative2 else config.showLanguageNative1
                if (showAd) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        NativeAdView(
                            adMobManager = viewModel.adMobManager,
                            adUnitId = AdIds.getNativeLanguageAdId(),
                            layoutResId = R.layout.native_ad_lang,
                            shimmerLayoutResId = R.layout.shimmer_template_lang_layout,
                            nativeConfig = config.nativeConfig,
                            eventNamePrefix = if (showSecondAd) "lng_scr_native2" else "lng_scr_native1",
                            analyticsManager = viewModel.analyticsManager
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageItemCard(
    language: LanguageModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
    val backgroundColor = if (isSelected) Color(0xFFEFF6FF) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = language.flag),
            contentDescription = language.name,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = language.name,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF475569),
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "Selected",
                tint = Color.Unspecified, // Assuming the icon already has colors, otherwise set your own
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_non_check),
                contentDescription = "Not Selected",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun getLanguageList(context: Context): List<LanguageModel> = listOf(
    LanguageModel(1, R.drawable.flag_arabic, context.getString(R.string.arabic), "ar"),
    LanguageModel(2, R.drawable.flag_english, context.getString(R.string.english), "en"),
    LanguageModel(3, R.drawable.flag_spanish, context.getString(R.string.spanish), "es"),
    LanguageModel(4, R.drawable.flag_indonesia, context.getString(R.string.indonesian), "in"),
    LanguageModel(6, R.drawable.flag_persian, context.getString(R.string.persian), "fa"),
    LanguageModel(7, R.drawable.flag_hindi, context.getString(R.string.hindi), "hi"),
    LanguageModel(8, R.drawable.flag_russia, context.getString(R.string.russian), "ru"),
    LanguageModel(9, R.drawable.flag_portuguese, context.getString(R.string.portuguese), "pt"),
    LanguageModel(10, R.drawable.flag_bangla, context.getString(R.string.bengali), "bn"),
    LanguageModel(11, R.drawable.flag_turkey, context.getString(R.string.turkish), "tr")
)
