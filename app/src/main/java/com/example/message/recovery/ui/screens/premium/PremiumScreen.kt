package com.example.message.recovery.ui.screens.premium

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.message.recovery.R
import com.example.message.recovery.ui.compose.FooterLegalLinks
import com.example.message.recovery.ui.compose.PremiumFeatureRow
import com.example.message.recovery.ui.compose.PrimaryPillButton
import com.example.message.recovery.ui.compose.SelectionIndicator
import com.example.message.recovery.ui.theme.Accent
import com.example.message.recovery.ui.theme.BgColor
import com.example.message.recovery.ui.theme.PrimaryGreenLight
import com.example.message.recovery.ui.theme.ProOrange
import com.example.message.recovery.ui.theme.StrokeColor
import com.example.message.recovery.ui.theme.TextPrimary
import com.example.message.recovery.ui.theme.TextSecondary
import com.example.message.recovery.ui.theme.DevicePreviews
import com.example.message.recovery.ui.theme.AppTheme
import com.example.message.recovery.ui.theme.contentMaxWidth
import com.example.message.recovery.ui.theme.isCompactWidth
import com.example.message.recovery.ui.theme.prefersSideBySide
import com.example.message.recovery.ui.theme.screenHorizontalPadding
import com.example.message.recovery.ui.theme.windowSize

enum class PremiumPlan { WEEKLY, YEARLY }

@Composable
fun PremiumScreen(
    weeklyPrice: String,
    yearlyPrice: String,
    yearlyWeeklyHint: String,
    trialText: String,
    ctaEnabled: Boolean,
    ctaLoading: Boolean,
    selectedPlan: PremiumPlan,
    showClose: Boolean,
    onSelectPlan: (PremiumPlan) -> Unit,
    onUpgrade: () -> Unit,
    onClose: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onRestore: () -> Unit,
) {
    // Two columns whenever the screen is wide enough or too short to stack — that covers landscape
    // phones and tablets alike, which raw ORIENTATION_LANDSCAPE would conflate.
    val stacked = !windowSize().prefersSideBySide
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        if (stacked) {
            PremiumPortraitContent(
                weeklyPrice = weeklyPrice,
                yearlyPrice = yearlyPrice,
                yearlyWeeklyHint = yearlyWeeklyHint,
                trialText = trialText,
                ctaEnabled = ctaEnabled,
                ctaLoading = ctaLoading,
                selectedPlan = selectedPlan,
                showClose = showClose,
                onSelectPlan = onSelectPlan,
                onUpgrade = onUpgrade,
                onClose = onClose,
                onTerms = onTerms,
                onPrivacy = onPrivacy,
                onRestore = onRestore,
            )
        } else {
            PremiumLandscapeContent(
                weeklyPrice = weeklyPrice,
                yearlyPrice = yearlyPrice,
                yearlyWeeklyHint = yearlyWeeklyHint,
                trialText = trialText,
                ctaEnabled = ctaEnabled,
                ctaLoading = ctaLoading,
                selectedPlan = selectedPlan,
                showClose = showClose,
                onSelectPlan = onSelectPlan,
                onUpgrade = onUpgrade,
                onClose = onClose,
                onTerms = onTerms,
                onPrivacy = onPrivacy,
                onRestore = onRestore,
            )
        }
    }
}

@Composable
private fun PremiumPortraitContent(
    weeklyPrice: String,
    yearlyPrice: String,
    yearlyWeeklyHint: String,
    trialText: String,
    ctaEnabled: Boolean,
    ctaLoading: Boolean,
    selectedPlan: PremiumPlan,
    showClose: Boolean,
    onSelectPlan: (PremiumPlan) -> Unit,
    onUpgrade: () -> Unit,
    onClose: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onRestore: () -> Unit,
) {
    val window = windowSize()
    // Three bands. The close/restore row is pinned at the top so the exit is always reachable, and
    // the CTA with its trial line is pinned at the bottom so the action the user came for never
    // scrolls away. Everything in between — crown, copy, features and the plan cards — scrolls in a
    // weight(1f) band, so it gives up space first when the screen is short or the font is large.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = window.screenHorizontalPadding, vertical = 12.dp)
            .widthIn(max = window.contentMaxWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showClose) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = TextPrimary
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRestore) {
                Text(
                    text = stringResource(R.string.restore_purchase),
                    color = TextSecondary,
                    fontSize = 14.sp,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CrownBadge(
                modifier = Modifier.padding(vertical = 16.dp),
                size = if (window.isCompactWidth) 180.dp else 220.dp,
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.premium_headline),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.premium_subline),
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(20.dp))
            PremiumFeatureRow(text = stringResource(R.string.premium_feature_no_ads))
            PremiumFeatureRow(text = stringResource(R.string.premium_feature_watch_apps))
            PremiumFeatureRow(text = stringResource(R.string.premium_feature_repost))

            Spacer(Modifier.height(24.dp))
            PlanCard(
                title = stringResource(R.string.weekly),
                subtitle = stringResource(R.string.weekly_subscription),
                price = weeklyPrice.ifBlank { "$4.99" },
                priceSuffix = "week",
                selected = selectedPlan == PremiumPlan.WEEKLY,
                showBestValue = false,
                onClick = { onSelectPlan(PremiumPlan.WEEKLY) },
            )
            Spacer(Modifier.height(12.dp))
            PlanCard(
                title = stringResource(R.string.yearly),
                subtitle = yearlyWeeklyHint.ifBlank { stringResource(R.string.yearly_plan_subtitle) },
                price = yearlyPrice.ifBlank { "$29.99" },
                priceSuffix = "",
                selected = selectedPlan == PremiumPlan.YEARLY,
                showBestValue = true,
                onClick = { onSelectPlan(PremiumPlan.YEARLY) },
            )
            Spacer(Modifier.height(12.dp))
        }

        // Pinned action block.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryPillButton(
                text = if (ctaLoading) stringResource(R.string.loading) else stringResource(R.string.try_it_free),
                onClick = onUpgrade,
                enabled = ctaEnabled && !ctaLoading,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (trialText.isNotBlank()) {
                Text(
                    text = trialText,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            FooterLegalLinks(
                termsLabel = stringResource(R.string.terms_of_servuce),
                privacyLabel = stringResource(R.string.privacy_policy),
                restoreLabel = stringResource(R.string.restore),
                onTerms = onTerms,
                onPrivacy = onPrivacy,
                onRestore = onRestore,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun PremiumLandscapeContent(
    weeklyPrice: String,
    yearlyPrice: String,
    yearlyWeeklyHint: String,
    trialText: String,
    ctaEnabled: Boolean,
    ctaLoading: Boolean,
    selectedPlan: PremiumPlan,
    showClose: Boolean,
    onSelectPlan: (PremiumPlan) -> Unit,
    onUpgrade: () -> Unit,
    onClose: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showClose) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = TextPrimary
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRestore) {
                Text(
                    text = stringResource(R.string.restore_purchase),
                    color = TextSecondary,
                    fontSize = 14.sp,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    // Also scrollable: the feature rows grow with the font scale and would
                    // otherwise be clipped against the fixed landscape height.
                    .verticalScroll(rememberScrollState())
                    .padding(end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CrownBadge(modifier = Modifier.padding(bottom = 8.dp), size = 96.dp)
                Text(
                    text = stringResource(R.string.premium_headline),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.premium_subline),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(12.dp))
                PremiumFeatureRow(text = stringResource(R.string.premium_feature_no_ads))
                PremiumFeatureRow(text = stringResource(R.string.premium_feature_watch_apps))
                PremiumFeatureRow(text = stringResource(R.string.premium_feature_repost))
            }

            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PlanCard(
                    title = stringResource(R.string.weekly),
                    subtitle = stringResource(R.string.weekly_subscription),
                    price = weeklyPrice.ifBlank { "$4.99" },
                    priceSuffix = "week",
                    selected = selectedPlan == PremiumPlan.WEEKLY,
                    showBestValue = false,
                    onClick = { onSelectPlan(PremiumPlan.WEEKLY) },
                )
                Spacer(Modifier.height(8.dp))
                PlanCard(
                    title = stringResource(R.string.yearly),
                    subtitle = yearlyWeeklyHint.ifBlank { stringResource(R.string.yearly_plan_subtitle) },
                    price = yearlyPrice.ifBlank { "$29.99" },
                    priceSuffix = "",
                    selected = selectedPlan == PremiumPlan.YEARLY,
                    showBestValue = true,
                    onClick = { onSelectPlan(PremiumPlan.YEARLY) },
                )
                Spacer(Modifier.height(12.dp))
                PrimaryPillButton(
                    text = if (ctaLoading) stringResource(R.string.loading) else stringResource(R.string.try_it_free),
                    onClick = onUpgrade,
                    enabled = ctaEnabled && !ctaLoading,
                )
                if (trialText.isNotBlank()) {
                    Text(
                        text = trialText,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                FooterLegalLinks(
                    termsLabel = stringResource(R.string.terms_of_servuce),
                    privacyLabel = stringResource(R.string.privacy_policy),
                    restoreLabel = stringResource(R.string.restore),
                    onTerms = onTerms,
                    onPrivacy = onPrivacy,
                    onRestore = onRestore,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }
}

/**
 * The circular crown mark. Shared by both layouts so the ring, the border and the 50% inset of the
 * icon stay identical; only the diameter differs.
 */
@Composable
private fun CrownBadge(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(BgColor)
            .border(1.dp, StrokeColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_crown_orange),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.5f),
        )
    }
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    price: String,
    priceSuffix: String,
    selected: Boolean,
    showBestValue: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) PrimaryGreenLight else BgColor)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Accent else StrokeColor,
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionIndicator(selected = selected)
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(price, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                if (priceSuffix.isNotBlank()) {
                    Text("per $priceSuffix", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
        if (showBestValue) {
            Text(
                text = stringResource(R.string.best_value),
                color = BgColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ProOrange)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}


@DevicePreviews
@Composable
private fun PremiumScreenPreview() {
    AppTheme {
        PremiumScreen(
            weeklyPrice = "$4.99",
            yearlyPrice = "$29.99",
            yearlyWeeklyHint = "Just $0.58 per week",
            trialText = "3 days free, then $29.99/year. Cancel anytime.",
            ctaEnabled = true,
            ctaLoading = false,
            selectedPlan = PremiumPlan.YEARLY,
            showClose = true,
            onSelectPlan = {},
            onUpgrade = {},
            onClose = {},
            onTerms = {},
            onPrivacy = {},
            onRestore = {},
        )
    }
}
