package com.example.message.recovery.ui.compose

import android.app.Activity
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.message.recovery.R
import com.umer_tf.ads.domain.ads.banner.BannerAdType
import com.umer_tf.ads.domain.viewmodel.AdViewModel

@Composable
fun BannerAdSlot(
    adUnitId: String,
    slotKey: String,
    modifier: Modifier = Modifier,
    type: BannerAdType = BannerAdType.ADAPTIVE,
) {
    val activity = LocalActivity.current as? Activity ?: return
    val storeOwner = activity as? ViewModelStoreOwner ?: return
    val adViewModel: AdViewModel = viewModel(viewModelStoreOwner = storeOwner)
    var container by remember { mutableStateOf<FrameLayout?>(null) }

    AndroidView(
        modifier = modifier.heightIn(min = 50.dp),
        factory = { context ->
            val root = LayoutInflater.from(context)
                .inflate(R.layout.native_ad_slot, null, false)
            container = root.findViewById(R.id.adFrame)
            root
        },
    )

    LaunchedEffect(container, slotKey, adUnitId, type) {
        val frame = container ?: return@LaunchedEffect
        if (adUnitId.isBlank()) return@LaunchedEffect
        adViewModel.showBanner(
            activity = activity,
            adUnitId = adUnitId,
            container = frame,
            shimmer = frame,
            type = type,
            key = slotKey,
        )
    }

    DisposableEffect(slotKey) {
        onDispose {
            container?.let { adViewModel.destroyBanner(it, slotKey) }
        }
    }
}
