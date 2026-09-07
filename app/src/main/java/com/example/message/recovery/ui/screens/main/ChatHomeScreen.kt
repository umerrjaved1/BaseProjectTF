package com.example.message.recovery.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.message.recovery.R
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.ui.compose.NativeOrBannerAdSlot
import com.example.message.recovery.ui.theme.Accent
import com.example.message.recovery.ui.theme.AppTheme
import com.example.message.recovery.ui.theme.BgColor
import com.example.message.recovery.ui.theme.DevicePreviews
import com.example.message.recovery.ui.theme.ProOrange
import com.example.message.recovery.ui.theme.StrokeColor
import com.example.message.recovery.ui.theme.TextPrimary
import com.example.message.recovery.ui.theme.TextSecondary
import com.umer_tf.ads.domain.ads.native_ad.NativeAdLayout

private val CardShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(50)
private val NativeInsertIndex = 4

@Composable
fun HomeTab(
    onRequestListenerAccess: () -> Unit,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onAddApps: () -> Unit,
    viewModel: ChatHomeViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onBack)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    // Resolved once instead of on every recomposition of the slot. `fromOrDefault` parses a remote
    // string into an enum and the unit IDs walk the remote-config object each time; none of it can
    // change while Home is on screen.
    val adRules = remember { RemoteConfigManager.getAdRules() }
    val nativeHomeAdId = remember { AdIds.getNativeHomeAdId() }
    val bannerHomeAdId = remember { AdIds.getBannerHomeAdId() }
    val homeNativeLayout = remember(adRules) {
        NativeAdLayout.fromOrDefault(adRules.homeNativeLayout, NativeAdLayout.LARGE_DEFAULT)
    }

    ChatHomeScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                ChatHomeContract.Event.AddApps -> onAddApps()
                ChatHomeContract.Event.Premium -> onPremium()
                ChatHomeContract.Event.Notifications -> onRequestListenerAccess()
                is ChatHomeContract.Event.FilterSelected -> viewModel.onEvent(event)
            }
        },
        adSlot = if (state.showHomeNative) {
            { modifier ->
                NativeOrBannerAdSlot(
                    enabled = true,
                    nativeAdUnitId = nativeHomeAdId,
                    bannerAdUnitId = bannerHomeAdId,
                    nativeSlotKey = "home_native",
                    modifier = modifier,
                    layout = homeNativeLayout,
                    nativeConfig = adRules.nativeConfig,
                    showMedia = true,
                    eventPrefix = "home_native",
                )
            }
        } else {
            null
        },
    )
}

@Composable
fun ChatHomeScreen(
    state: ChatHomeContract.UiState,
    onEvent: (ChatHomeContract.Event) -> Unit,
    adSlot: (@Composable (Modifier) -> Unit)? = null,
) {
    // `state.visibleChats` is a computed getter that allocates a filtered list on every read. Read
    // straight from the LazyColumn content it re-ran on every recomposition and handed the list a
    // fresh instance each time, defeating item reuse.
    val visibleChats = remember(state.chats, state.selectedFilter) { state.visibleChats }
    // The ad is spliced into the middle of the list, so the two halves are memoised as well —
    // `take`/`drop` in the item block allocated two more lists per recomposition.
    val chatsBeforeAd = remember(visibleChats) { visibleChats.take(NativeInsertIndex) }
    val chatsAfterAd = remember(visibleChats) { visibleChats.drop(NativeInsertIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recover_messages),
                modifier = Modifier.weight(1f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ProButton(onClick = { onEvent(ChatHomeContract.Event.Premium) })
            IconButton(onClick = { onEvent(ChatHomeContract.Event.Notifications) }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.notifications),
                    tint = TextPrimary,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, StrokeColor, CircleShape)
                    .clickable(role = Role.Button, onClick = { onEvent(ChatHomeContract.Event.AddApps) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_apps),
                    tint = TextPrimary,
                )
            }
            FilterChip(
                label = stringResource(R.string.all_filter),
                selected = state.selectedFilter == null,
                onClick = { onEvent(ChatHomeContract.Event.FilterSelected(null)) },
            )
            state.watchedApps.forEach { app ->
                FilterChip(
                    label = app.name,
                    selected = state.selectedFilter == app.packageName,
                    onClick = { onEvent(ChatHomeContract.Event.FilterSelected(app.packageName)) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.chats_header),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.newest_first),
                color = TextSecondary,
                fontSize = 12.sp,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(chatsBeforeAd, key = { it.id }) { chat ->
                ChatCard(chat)
            }
            if (adSlot != null) {
                item(key = "home_native") {
                    adSlot(Modifier.fillMaxWidth())
                }
            }
            items(chatsAfterAd, key = { it.id }) { chat ->
                ChatCard(chat)
            }
        }
    }
}

@Composable
private fun ProButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .background(Brush.horizontalGradient(listOf(ProOrange, Color(0xFFFFC107))))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.pro),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clip(ChipShape)
            .background(if (selected) Accent else Color.White)
            .border(1.dp, if (selected) Accent else StrokeColor, ChipShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = if (selected) Color.White else TextPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        maxLines = 1,
    )
}

@Composable
private fun ChatCard(chat: ChatPreviewUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, StrokeColor, CardShape)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(chat.avatarColor)),
                contentAlignment = Alignment.Center,
            ) {
                Text(chat.letter, fontWeight = FontWeight.Bold, color = Accent)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp,
                    )
                    if (chat.unread) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Accent),
                        )
                    }
                }
                Text(
                    text = chat.snippet,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${chat.deletedLabel} · ${chat.appLabel}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun ChatHomePreview() {
    AppTheme {
        ChatHomeScreen(
            state = ChatHomeContract.UiState(
                chats = listOf(
                    ChatPreviewUi("1", "Sarah", "S", 0xFFE1BEE7, "Hello there", "Deleted 14:22", "WhatsApp", "wa", true),
                ),
            ),
            onEvent = {},
        )
    }
}
