package com.tf.phonecleaner.booster.ui.components

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.shimmer.ShimmerFrameLayout
import com.tf.phonecleaner.booster.R
import com.tf.phonecleaner.booster.remoteconfig.data.NativeAdConfigData
import com.tf.phonecleaner.booster.utils.AdUtils
import com.umer_tf.ads.domain.core.AdMobManager
import com.tf.phonecleaner.booster.app.AnalyticsManager

@Composable
fun NativeAdView(
    modifier: Modifier = Modifier,
    adMobManager: AdMobManager,
    adUnitId: String,
    layoutResId: Int,
    shimmerLayoutResId: Int,
    showMedia: Boolean = true,
    nativeConfig: NativeAdConfigData? = null,
    analyticsManager: AnalyticsManager? = null,
    eventNamePrefix: String? = null,
    onAdLoaded: ((Boolean) -> Unit)? = null
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val container = FrameLayout(context)
            
            // Inflate Shimmer
            val shimmerView = ShimmerFrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                LayoutInflater.from(context).inflate(shimmerLayoutResId, this, true)
            }
            container.addView(shimmerView)
            
            // Inflate Ad Frame
            val adFrame = FrameLayout(context)
            container.addView(adFrame)

            AdUtils.loadAndShowNativeAd(
                adMobManager = adMobManager,
                adUnitId = adUnitId,
                layoutResId = layoutResId,
                frameLayout = adFrame,
                shimmerFrameLayout = shimmerView,
                showMedia = showMedia,
                nativeConfig = nativeConfig,
                analyticsManager = analyticsManager,
                eventNamePrefix = eventNamePrefix,
                onAdLoaded = onAdLoaded
            )

            container
        }
    )
}
