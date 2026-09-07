package com.example.message.recovery.ui.compose

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.shimmer.ShimmerFrameLayout
import com.example.message.recovery.R

@Composable
fun NativeAdContainer(
    modifier: Modifier = Modifier,
    containerLayoutRes: Int = R.layout.ad_container_onboarding,
    onReady: (FrameLayout, ShimmerFrameLayout) -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = LayoutInflater.from(context).inflate(containerLayoutRes, null, false)
            val frame = view.findViewById<FrameLayout>(R.id.adFrame)
            val shimmer = view.findViewById<ShimmerFrameLayout>(R.id.shimmerFbAd)
            onReady(frame, shimmer)
            view
        },
        update = { view ->
            val frame = view.findViewById<FrameLayout>(R.id.adFrame)
            val shimmer = view.findViewById<ShimmerFrameLayout>(R.id.shimmerFbAd)
            onReady(frame, shimmer)
        },
    )
}
