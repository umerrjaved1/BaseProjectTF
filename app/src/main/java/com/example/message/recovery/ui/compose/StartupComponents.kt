package com.example.message.recovery.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.message.recovery.R
import com.example.message.recovery.ui.theme.Accent
import com.example.message.recovery.ui.theme.BadgeOptional
import com.example.message.recovery.ui.theme.BadgeOptionalText
import com.example.message.recovery.ui.theme.BadgeRequired
import com.example.message.recovery.ui.theme.BgColor
import com.example.message.recovery.ui.theme.DotInactive
import com.example.message.recovery.ui.theme.StrokeColor
import com.example.message.recovery.ui.theme.TextPrimary
import com.example.message.recovery.ui.theme.TextSecondary

@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            disabledContainerColor = Accent.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun TopPillAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCheckmark: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (showCheckmark) {
            Icon(
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null,
                tint = BgColor,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text.uppercase(),
            color = BgColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(Accent)
                } else {
                    Modifier
                        .background(BgColor)
                        .border(2.dp, StrokeColor, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null,
                tint = BgColor,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
fun PageIndicatorDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 8.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (selected) Accent else DotInactive),
            )
        }
    }
}

enum class PermissionBadgeType { Required, Optional }

@Composable
fun PermissionCard(
    iconRes: Int,
    title: String,
    description: String,
    badgeType: PermissionBadgeType,
    badgeText: String,
    modifier: Modifier = Modifier,
) {
    val badgeBg = when (badgeType) {
        PermissionBadgeType.Required -> BadgeRequired
        PermissionBadgeType.Optional -> BadgeOptional
    }
    val badgeFg = when (badgeType) {
        PermissionBadgeType.Required -> BgColor
        PermissionBadgeType.Optional -> BadgeOptionalText
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, StrokeColor, RoundedCornerShape(12.dp))
            .background(BgColor)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            colorFilter = ColorFilter.tint(Accent),
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
fun PremiumFeatureRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_check_item_iap),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 10.dp),
            fontSize = 15.sp,
            color = TextPrimary,
        )
    }
}

@Composable
fun FooterLegalLinks(
    termsLabel: String,
    privacyLabel: String,
    restoreLabel: String,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = termsLabel,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.clickable(onClick = onTerms),
        )
        Text(text = " — ", fontSize = 12.sp, color = TextSecondary)
        Text(
            text = privacyLabel,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.clickable(onClick = onPrivacy),
        )
        Text(text = " — ", fontSize = 12.sp, color = TextSecondary)
        Text(
            text = restoreLabel,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.clickable(onClick = onRestore),
        )
    }
}
