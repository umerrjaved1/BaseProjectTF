package com.example.message.recovery.ui.screens.main

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.message.recovery.R
import com.example.message.recovery.ui.compose.ExitConfirmDialog
import com.example.message.recovery.ui.compose.NotificationListenerPermissionDialog
import com.example.message.recovery.ui.compose.NotificationPermissionDialog
import com.example.message.recovery.ui.compose.RateUsDialog
import com.example.message.recovery.ui.navigation.AppRoute
import com.example.message.recovery.ui.navigation.Home
import com.example.message.recovery.ui.navigation.Media
import com.example.message.recovery.ui.navigation.Settings
import com.example.message.recovery.ui.navigation.Status
import com.example.message.recovery.ui.theme.Accent
import com.example.message.recovery.ui.theme.AppTheme
import com.example.message.recovery.ui.theme.BgColor
import com.example.message.recovery.ui.theme.DevicePreviews
import com.example.message.recovery.ui.theme.NavInactive
import com.example.message.recovery.ui.theme.PrimaryGreenLight
import com.example.message.recovery.ui.theme.StrokeColor
import com.example.message.recovery.ui.theme.TextPrimary
import com.example.message.recovery.ui.theme.contentMaxWidth
import com.example.message.recovery.ui.theme.screenHorizontalPadding
import com.example.message.recovery.ui.theme.windowSize

// Hoisted out of the composables below. `RoundedCornerShape(…)` allocates, and these were being
// rebuilt on every recomposition — twice per nav item, four items, plus the bar itself.
private val BarShape = RoundedCornerShape(28.dp)
private val NavItemShape = RoundedCornerShape(16.dp)
private val BadgeShape = RoundedCornerShape(50)
private val SettingsRowShape = RoundedCornerShape(16.dp)

@Composable
fun MediaTab(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    PlaceholderTab(title = stringResource(R.string.media), body = stringResource(R.string.empty_media))
}

@Composable
fun StatusTab(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    PlaceholderTab(title = stringResource(R.string.status), body = stringResource(R.string.empty_status))
}

@Composable
fun SettingsTab(
    onPremium: () -> Unit,
    onLanguage: () -> Unit,
    onBackToHome: () -> Unit,
) {
    BackHandler(onBack = onBackToHome)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(20.dp))
        SettingsRow(stringResource(R.string.go_pro), onPremium)
        Spacer(Modifier.height(10.dp))
        SettingsRow(stringResource(R.string.choose_language), onLanguage)
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsRowShape)
            .border(1.dp, StrokeColor, SettingsRowShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp),
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )
}

@Composable
private fun PlaceholderTab(title: String, body: String) {
    CenteredTabContent {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(text = body, color = NavInactive, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CenteredTabContent(content: @Composable () -> Unit) {
    val window = windowSize()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = window.contentMaxWidth)
                .padding(horizontal = window.screenHorizontalPadding, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Composable
fun MainNavigation(
    current: AppRoute,
    useRail: Boolean,
    onTabSelected: (AppRoute) -> Unit,
    chatBadge: Int = 3,
) {
    if (useRail) MainNavigationRail(current, onTabSelected, chatBadge)
    else MainBottomBar(current, onTabSelected, chatBadge)
}

private data class TabSpec(
    val route: AppRoute,
    val labelRes: Int,
    val icon: ImageVector,
)

private val mainTabs = listOf(
    TabSpec(Home, R.string.chat, Icons.Outlined.ChatBubbleOutline),
    TabSpec(Media, R.string.media, Icons.Outlined.PhotoLibrary),
    TabSpec(Status, R.string.status, Icons.Outlined.Layers),
    TabSpec(Settings, R.string.settings, Icons.Outlined.Settings),
)

@Composable
private fun MainNavigationRail(
    current: AppRoute,
    onTabSelected: (AppRoute) -> Unit,
    chatBadge: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        mainTabs.forEach { tab ->
            NavItem(
                icon = tab.icon,
                label = stringResource(tab.labelRes),
                selected = current == tab.route,
                badge = if (tab.route == Home) chatBadge else 0,
            ) { onTabSelected(tab.route) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MainBottomBar(
    current: AppRoute,
    onTabSelected: (AppRoute) -> Unit,
    chatBadge: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(12.dp)
            // `shadow` already applies the shape and can clip to it, so the separate `clip` that
            // followed was allocating a second render layer for the same rounded rect on every
            // frame the bar redrew.
            .shadow(6.dp, BarShape, clip = true)
            .background(Color.White)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mainTabs.forEach { tab ->
            NavItem(
                icon = tab.icon,
                label = stringResource(tab.labelRes),
                selected = current == tab.route,
                badge = if (tab.route == Home) chatBadge else 0,
            ) { onTabSelected(tab.route) }
        }
    }
}

@Composable
fun MainDialogs(
    showExitDialog: Boolean,
    showNotificationDialog: Boolean,
    showListenerPermissionDialog: Boolean,
    showRateUsDialog: Boolean,
    eventHandler: MainContract.EventHandler,
) {
    if (showExitDialog) {
        ExitConfirmDialog(onDismiss = eventHandler.onExitDismiss, onConfirm = eventHandler.onExitConfirm)
    }
    if (showNotificationDialog) {
        NotificationPermissionDialog(
            onAllow = eventHandler.onNotificationAllow,
            onDismiss = eventHandler.onNotificationDismiss,
        )
    }
    if (showListenerPermissionDialog) {
        NotificationListenerPermissionDialog(
            onAllow = eventHandler.onListenerAllow,
            onDismiss = eventHandler.onListenerDismiss,
        )
    }
    if (showRateUsDialog) {
        RateUsDialog(onRate = eventHandler.onRate, onDismiss = eventHandler.onRateDismiss)
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(NavItemShape)
            .clickable(enabled = !selected, onClick = onClick)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(NavItemShape)
                .background(if (selected) PrimaryGreenLight else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (selected) Accent else NavInactive,
            )
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .clip(BadgeShape)
                        .background(Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badge.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            label,
            color = if (selected) TextPrimary else NavInactive,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

@DevicePreviews
@Composable
private fun SettingsTabPreview() {
    AppTheme {
        SettingsTab(onPremium = {}, onLanguage = {}, onBackToHome = {})
    }
}
