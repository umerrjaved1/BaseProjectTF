package com.example.message.recovery.ui.screens.onboarding

import androidx.compose.foundation.Image
import coil3.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.message.recovery.R
import com.example.message.recovery.model.OnboardingItem
import com.example.message.recovery.ui.compose.NativeAdContainer
import com.example.message.recovery.ui.compose.PageIndicatorDots
import com.example.message.recovery.ui.compose.PrimaryPillButton
import com.example.message.recovery.ui.theme.Accent
import com.example.message.recovery.ui.theme.BgColor
import com.example.message.recovery.ui.theme.DevicePreviews
import com.example.message.recovery.ui.theme.TextPrimary
import com.example.message.recovery.ui.theme.TextSecondary
import com.example.message.recovery.ui.theme.AppTheme
import com.example.message.recovery.ui.theme.contentMaxWidth
import com.example.message.recovery.ui.theme.isShortScreen
import com.example.message.recovery.ui.theme.prefersSideBySide
import com.example.message.recovery.ui.theme.screenHorizontalPadding
import com.example.message.recovery.ui.theme.windowSize

sealed class OnboardingPage {
    data class Content(val item: OnboardingItem, val itemIndex: Int) : OnboardingPage()
    data object FullNativeAd : OnboardingPage()

}

@Composable
fun OnboardingScreen(
    pages: List<OnboardingPage>,
    currentPage: Int,
    /**
     * The inline native ad for content slides, supplied by the route. Null when no ad should show.
     */
    adSlot: (@Composable (Modifier) -> Unit)? = null,
    onPageChanged: (Int) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onFullNativeReady: (android.widget.FrameLayout, com.facebook.shimmer.ShimmerFrameLayout) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = currentPage, pageCount = { pages.size })
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage && currentPage in pages.indices) {
            pagerState.animateScrollToPage(currentPage)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    val current = pages.getOrNull(currentPage)
    val isAdPage = current is OnboardingPage.FullNativeAd
    val indicatorCount = pages.count { it !is OnboardingPage.FullNativeAd }
    val indicatorIndex = pages.take(currentPage + 1).count { it !is OnboardingPage.FullNativeAd } - 1
    val window = windowSize()

    val pager: @Composable (Modifier) -> Unit = { m ->
        HorizontalPager(
            state = pagerState,
            modifier = m,
            beyondViewportPageCount = 1,
        ) { index ->
            when (val page = pages[index]) {
                is OnboardingPage.Content -> ContentOnboardingPage(item = page.item, onSkip = onSkip)
                OnboardingPage.FullNativeAd -> {
                    NativeAdContainer(
                        modifier = Modifier.fillMaxSize(),
                        containerLayoutRes = R.layout.full_native_ad_item_view_pager,
                        onReady = onFullNativeReady,
                    )
                }
            }
        }
    }
    val dotsAndCta: @Composable (Modifier) -> Unit = { m ->
        // Width-capped and centred so the CTA doesn't become a full-bleed slab on a tablet, while
        // staying edge-to-edge on a phone.
        Column(modifier = m.widthIn(max = window.contentMaxWidth)) {
            if (!isAdPage && indicatorCount > 0) {
                PageIndicatorDots(
                    pageCount = indicatorCount,
                    currentPage = indicatorIndex.coerceAtLeast(0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )
            }
            PrimaryPillButton(
                text = stringResource(R.string.next),
                onClick = onContinue,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }

    val showInlineAd = adSlot != null && !isAdPage
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        pager(Modifier.weight(1f).fillMaxWidth())
        if (!isAdPage) {
            dotsAndCta(Modifier.fillMaxWidth().align(Alignment.CenterHorizontally))
            if (showInlineAd) {
                adSlot?.invoke(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                )
            }
        }
    }
}

/**
 * Illustration above copy when there is vertical room, illustration beside copy when there isn't.
 * On a landscape phone the stacked version cannot fit a 260dp image plus a title, a description,
 * the page dots, an inline native ad and a 52dp CTA, so the two halves move side by side instead.
 */
@Composable
private fun ContentOnboardingPage(
    item: OnboardingItem,
    onSkip: () -> Unit,
) {
    val window = windowSize()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = window.screenHorizontalPadding),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Text(
                    text = stringResource(R.string.skip).uppercase(),
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
        if (window.prefersSideBySide) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OnboardingIllustration(
                    item = item,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 16.dp),
                )
                OnboardingCopy(
                    item = item,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                OnboardingIllustration(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                )
                Spacer(Modifier.height(24.dp))
                OnboardingCopy(item = item, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Sized by the space it is given rather than a fixed height, so it shrinks on a short screen
 * instead of pushing the copy off the bottom.
 *
 * Loaded through Coil rather than painterResource. These illustrations are 1560x1832 source
 * images; painterResource decodes them at full size on the main thread, and the pager keeps
 * neighbouring pages composed, so two or three full-size bitmaps were being decoded during a swipe.
 * That measured at 91% janky frames with a 97ms median. Coil decodes off the main thread and
 * downsamples to the size this composable was actually measured at.
 */
@Composable
private fun OnboardingIllustration(
    item: OnboardingItem,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = item.imageRes,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight.coerceAtLeast(96.dp)),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun OnboardingCopy(
    item: OnboardingItem,
    modifier: Modifier = Modifier,
) {
    // A landscape phone gives this column roughly 250dp of height. At the portrait type scale the
    // title alone eats half of it, so the copy steps down a size rather than relying on the scroll.
    val isShort = windowSize().isShortScreen
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = item.title,
            fontSize = if (isShort) 18.sp else 22.sp,
            lineHeight = if (isShort) 24.sp else 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        if (item.description.isNotBlank()) {
            Spacer(Modifier.height(if (isShort) 8.dp else 12.dp))
            Text(
                text = item.description,
                fontSize = if (isShort) 13.sp else 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = if (isShort) 18.sp else 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}


@DevicePreviews
@Composable
private fun OnboardingContentPreview() {
    AppTheme {
        OnboardingScreen(
            pages = listOf(
                OnboardingPage.Content(
                    item = OnboardingItem(
                        imageRes = R.drawable.ic_permission_shield,
                        title = "Recover deleted messages",
                        description = "See messages that were removed from your chats, right here.",
                    ),
                    itemIndex = 0,
                ),
            ),
            currentPage = 0,
            onPageChanged = {},
            onContinue = {},
            onSkip = {},
            onFullNativeReady = { _, _ -> },
        )
    }
}

