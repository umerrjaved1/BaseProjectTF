package com.example.message.recovery.ui.compose

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.message.recovery.R
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AnalyticsManager.Action
import com.example.message.recovery.remoteconfig.data.NativeAdConfigData
import com.example.message.recovery.utils.AdUtils
import com.umer_tf.ads.domain.ads.native_ad.NativeAdLayout
import com.umer_tf.ads.domain.ads.native_ad.NativeAdTheme
import com.umer_tf.ads.domain.viewmodel.AdViewModel
import com.umer_tf.ads.domain.viewmodel.NativeAdUiState
import com.umer_tf.ads.domain.viewmodel.bindNativeAd

/**
 * A native ad slot.
 *
 * Two things make this survive a rotation without re-requesting the ad:
 *
 *  - the loaded ad lives in [AdViewModel], scoped to the Activity and cached under [slotKey], so
 *    re-binding renders the ad already in memory instead of asking the network for a new one. That
 *    also covers navigating away and back;
 *  - the caller can keep the composable itself alive across a layout change with
 *    `remember { movableContentOf { … } }`, which avoids even re-inflating the view.
 *
 * [layout] is the design, chosen per slot — a type-safe [NativeAdLayout] rather than a raw layout
 * resource, so the SDK owns the view hierarchy and slots stay consistent. Pass a remote-config
 * string through [NativeAdLayout.fromOrDefault] to make the design server-driven.
 *
 * [slotKey] identifies the cached ad. Give each visually distinct slot its own stable key; two
 * slots sharing a key share one ad.
 */
@Composable
fun NativeAdSlot(
    adUnitId: String,
    slotKey: String,
    modifier: Modifier = Modifier,
    layout: NativeAdLayout = NativeAdLayout.LARGE_DEFAULT,
    nativeConfig: NativeAdConfigData? = null,
    showMedia: Boolean = false,
    analyticsManager: AnalyticsManager? = null,
    eventPrefix: String? = null,
    onStateChanged: (NativeAdUiState) -> Unit = {},
) {
    // The ViewModel is scoped to the Activity rather than to this composable, which is what keeps
    // the cache alive while the user moves between startup screens.
    val storeOwner = LocalActivity.current as? ViewModelStoreOwner ?: return
    val adViewModel: AdViewModel = viewModel(viewModelStoreOwner = storeOwner)
    val lifecycleOwner = LocalLifecycleOwner.current

    var slot by remember { mutableStateOf<AdSlotViews?>(null) }
    var layoutGeneration by remember { mutableIntStateOf(0) }

    // AndroidView's `update` runs on *every* recomposition, not only when [layoutGeneration]
    // changes. Re-measuring the native-ad view tree from there — it is the deepest hierarchy on
    // Home and sits inside the LazyColumn — was dropping frames on every unrelated state change.
    // Held in a plain array rather than snapshot state: `update` is snapshot-observed, so writing
    // observable state from inside it would re-trigger itself.
    val lastLaidOutGeneration = remember { intArrayOf(-1) }

    AndroidView(
        modifier = modifier.wrapContentHeight(),
        factory = { context ->
            val root = LayoutInflater.from(context)
                .inflate(R.layout.native_ad_slot, null, false)
            root.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            val adFrame = root.findViewById<FrameLayout>(R.id.adFrame)
            adFrame.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            root.findViewById<FrameLayout>(R.id.shimmerContainer).visibility = android.view.View.GONE
            slot = AdSlotViews(adFrame = adFrame)
            root
        },
        update = { view ->
            if (lastLaidOutGeneration[0] != layoutGeneration) {
                lastLaidOutGeneration[0] = layoutGeneration
                view.requestLayout()
            }
        },
    )

    val context = LocalContext.current
    val theme = remember(nativeConfig, context) { nativeConfig.toNativeAdTheme(context) }

    // Re-binds only when the slot identity or its inputs actually change; a recomposition on its
    // own does not touch the ad.
    LaunchedEffect(slot, slotKey, adUnitId, layout, showMedia, theme) {
        val views = slot ?: return@LaunchedEffect
        adViewModel.bindNativeAd(
            owner = lifecycleOwner,
            adUnitId = adUnitId,
            layout = layout,
            container = views.adFrame,
            theme = theme,
            showMedia = showMedia,
            key = slotKey,
        )
    }

    val state by adViewModel.nativeState(slotKey).collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        onStateChanged(state)
        if (state is NativeAdUiState.Loaded || state is NativeAdUiState.Failed) {
            layoutGeneration++
        }
    }

    // Same request/pass/fail funnel the previous AdUtils path reported, so existing dashboards keep
    // working. Reported per slot key and only on a real transition, so a rotation that re-renders
    // the cached ad does not inflate the counts.
    if (analyticsManager != null && eventPrefix != null) {
        LaunchedEffect(slotKey, eventPrefix, state) {
            when (state) {
                is NativeAdUiState.Loading ->
                    analyticsManager.sendAnalytics(Action.ACTION_TYPE, "${eventPrefix}_request")
                is NativeAdUiState.Loaded -> {
                    analyticsManager.sendAnalytics(Action.ACTION_TYPE, "${eventPrefix}_pass")
                    analyticsManager.sendAnalytics(Action.ACTION_TYPE, "${eventPrefix}_view")
                }
                is NativeAdUiState.Failed ->
                    analyticsManager.sendAnalytics(Action.ACTION_TYPE, "${eventPrefix}_fail")
                else -> Unit
            }
        }
    }
}

/**
 * Convenience wrapper for the common case: reserve the space only while the ad can still succeed,
 * and collapse the slot entirely once it has failed, so a dead ad leaves no gap behind.
 */
@Composable
fun CollapsingNativeAdSlot(
    adUnitId: String,
    slotKey: String,
    modifier: Modifier = Modifier,
    layout: NativeAdLayout = NativeAdLayout.LARGE_DEFAULT,
    nativeConfig: NativeAdConfigData? = null,
    showMedia: Boolean = false,
    analyticsManager: AnalyticsManager? = null,
    eventPrefix: String? = null,
) {
    var failed by remember(slotKey) { mutableStateOf(false) }
    if (failed) return
    Box(modifier = modifier) {
        NativeAdSlot(
            adUnitId = adUnitId,
            slotKey = slotKey,
            layout = layout,
            nativeConfig = nativeConfig,
            showMedia = showMedia,
            analyticsManager = analyticsManager,
            eventPrefix = eventPrefix,
            onStateChanged = { state -> failed = state is NativeAdUiState.Failed },
        )
    }
}

@Composable
fun NativeOrBannerAdSlot(
    enabled: Boolean,
    nativeAdUnitId: String,
    bannerAdUnitId: String,
    nativeSlotKey: String,
    modifier: Modifier = Modifier,
    layout: NativeAdLayout = NativeAdLayout.LARGE_DEFAULT,
    nativeConfig: NativeAdConfigData? = null,
    showMedia: Boolean = false,
    analyticsManager: AnalyticsManager? = null,
    eventPrefix: String? = null,
) {
    if (!enabled || AdUtils.areAdsEnabled().not()) return
    var nativeFailed by remember(nativeSlotKey) { mutableStateOf(false) }
    if (!nativeFailed) {
        Box(modifier = modifier) {
            NativeAdSlot(
                adUnitId = nativeAdUnitId,
                slotKey = nativeSlotKey,
                layout = layout,
                nativeConfig = nativeConfig,
                showMedia = showMedia,
                analyticsManager = analyticsManager,
                eventPrefix = eventPrefix,
                onStateChanged = { state ->
                    nativeFailed = state is NativeAdUiState.Failed
                },
            )
        }
    } else if (bannerAdUnitId.isNotBlank()) {
        BannerAdSlot(
            adUnitId = bannerAdUnitId,
            slotKey = "${nativeSlotKey}_banner",
            modifier = modifier,
        )
    }
}

private data class AdSlotViews(
    val adFrame: FrameLayout,
)

/**
 * Starts from the SDK's light/dark-aware defaults and overrides only the colours remote config
 * actually specifies, so an unset field keeps a sane value instead of becoming transparent.
 */
private fun NativeAdConfigData?.toNativeAdTheme(context: Context): NativeAdTheme {
    val base = NativeAdTheme.auto(context)
    val config = this ?: return base
    return base.copy(
        adTitleColor = config.heading.ifBlank { base.adTitleColor },
        adBodyColor = config.description.ifBlank { base.adBodyColor },
        ctaTextColor = config.ctaText.ifBlank { base.ctaTextColor },
        ctaBgColor = config.callActionButtonColor.ifBlank { base.ctaBgColor },
        adBgColor = config.backgroundColor.ifBlank { base.adBgColor },
    )
}
