package com.tf.phonecleaner.booster.ui.screens.compose

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.tf.phonecleaner.booster.R
import com.tf.phonecleaner.booster.app.AdIds
import com.tf.phonecleaner.booster.model.OnboardingItem
import com.tf.phonecleaner.booster.remoteconfig.RemoteConfigManager
import com.tf.phonecleaner.booster.ui.components.NativeAdView
import com.tf.phonecleaner.booster.ui.viewmodel.OnboardingViewModel
import com.tf.phonecleaner.booster.utils.AdFrequencyControl
import com.tf.phonecleaner.booster.utils.AdUnitFrequencyController
import com.tf.phonecleaner.booster.utils.AdUtils
import com.umer_tf.ads.domain.core.AdMobManager
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onNavigateToPremium: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? AppCompatActivity
    val scope = rememberCoroutineScope()

    val onboardingItems = remember {
        listOf(
            OnboardingItem(
                title = context.getString(R.string.onboarding_title_1),
                description = "",
                imageRes = R.drawable.ob_1
            ),
            OnboardingItem(
                title = context.getString(R.string.onboarding_title_2),
                description = "",
                imageRes = R.drawable.ob_2
            ),
            OnboardingItem(
                title = context.getString(R.string.onboarding_title_3),
                description = "",
                imageRes = R.drawable.ob_3
            ),
            OnboardingItem(
                title = context.getString(R.string.onboarding_title_4),
                description = "",
                imageRes = R.drawable.ob_4
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { onboardingItems.size })
    val config = RemoteConfigManager.getOnboardingScreenConfig()

    val handleFinishOnboarding = {
        viewModel.markOnboardingComplete()
        if (activity != null) {
            when (config.onBoardingMonetizationStrategy) {
                0 -> onNavigateToMain()
                1 -> onNavigateToPremium()
                2 -> {
                    if (AdMobManager.isPremium || !config.showOnboardingInterstitial) {
                        onNavigateToMain()
                    } else {
                        AdUtils.loadAndShowInterAdWithDialog(
                            adMobManager = viewModel.adMobManager,
                            activity = activity,
                            adUnit = AdIds.getInterstitialOnboardingID(),
                            lifecycleScope = activity.lifecycleScope,
                            analyticsManager = viewModel.analyticsManager,
                            eventNamePrefix = "getstarted_int"
                        ) {
                            onNavigateToMain()
                        }
                    }
                }
                else -> onNavigateToPremium()
            }
        } else {
            onNavigateToMain()
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top action bar (Skip button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = handleFinishOnboarding,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF64748B))
                ) {
                    Text("Skip", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val item = onboardingItems[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = item.imageRes),
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = item.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )
                    if (item.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = item.description,
                            fontSize = 16.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Dots Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(onboardingItems.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .clip(if (isSelected) RoundedCornerShape(4.dp) else CircleShape)
                            .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                    )
                }
            }

            // Native Ad
            val shouldShowAdThisSlide = when (pagerState.currentPage) {
                0 -> config.showOb1Native
                1 -> config.showOb2Native
                2 -> config.showOb3Native
                3 -> config.showOb4Native
                else -> false
            }

            if (!AdMobManager.isPremium && shouldShowAdThisSlide) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    NativeAdView(
                        adMobManager = viewModel.adMobManager,
                        adUnitId = AdIds.getNativeOnboardingAdId(),
                        layoutResId = R.layout.native_ad_large,
                        shimmerLayoutResId = R.layout.shimmer_template_large_native,
                        nativeConfig = config.nativeConfig,
                        eventNamePrefix = "ob${pagerState.currentPage + 1}_native",
                        analyticsManager = viewModel.analyticsManager
                    )
                }
            }

            // Bottom Action Button
            Button(
                onClick = {
                    if (pagerState.currentPage < onboardingItems.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        handleFinishOnboarding()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(
                    text = if (pagerState.currentPage == onboardingItems.size - 1) 
                        stringResource(R.string.get_started) 
                    else 
                        stringResource(R.string.continuee),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
