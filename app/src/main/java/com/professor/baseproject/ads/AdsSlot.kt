package com.professor.baseproject.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.professor.baseproject.R
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.umer_tf.ads.domain.ads.native_ad.NativeAdTheme
import com.umer_tf.ads.domain.ads.native_ad.createNativeAdBuilderAutoShimmer
import com.umer_tf.ads.domain.analytics.AdType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The shape of an inline ad slot, as specified by the ad plan.
 *
 * Each shape pairs a native layout with the banner size to fall back to when native does not fill.
 * The fallback is the point: native has lower fill than banner, so a native-only slot is empty a
 * meaningful fraction of the time.
 */
enum class AdSlotStyle {

    /** Small native, no media. Language picker, and list/settings screens. */
    SMALL_NO_MEDIA,

    /** Small native shaped like a banner. Onboarding slides. */
    SMALL_BANNER,

    /** Medium native with media. Survey CTA; falls back to a 300x250 rectangle. */
    MEDIUM;

    internal val nativeLayout: Int
        get() = when (this) {
            SMALL_NO_MEDIA -> com.umer_tf.ads.R.layout.layout_native_ad_small_3a
            SMALL_BANNER -> com.umer_tf.ads.R.layout.layout_native_ad_banner
            MEDIUM -> com.umer_tf.ads.R.layout.layout_native_ad_large_5a
        }

    /** Media is what makes a native ad tall; the plan asks for it only on [MEDIUM]. */
    internal val showMedia: Boolean get() = this == MEDIUM
}

/**
 * Which format a slot tries first. Whichever loses, the other is the fallback - the slot only stays
 * empty if both fail.
 */
enum class AdSlotPreference {
    /** Native first, banner on failure. Higher value per impression. */
    NATIVE_FIRST,

    /** Banner first, native on failure. Higher chance the slot is filled at all. */
    BANNER_FIRST
}

/**
 * Fills an inline ad slot with a native ad, falling back to a banner when native does not fill.
 *
 * Handles the whole slot: shimmer while loading, the native attempt, the banner fallback, and
 * collapsing the slot entirely when neither fills or ads are off. A screen calls [show] once and
 * does not deal with any of it.
 *
 * ### Layout contract
 * The screen supplies one container. This adds the two children it needs:
 *
 * ```xml
 * <FrameLayout
 *     android:id="@+id/adSlot"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 */
@Singleton
class AdsSlot @Inject constructor(
    private val adsController: AdsController,
    private val bannerRefresher: BannerRefresher
) {

    /**
     * Loads an ad into [container].
     *
     * @param placement Which screen this slot is on. Picks the native ad unit from `ad_ids`.
     * @param style Which shape to request, and therefore which banner to fall back to.
     * @param onResult true when something was shown - native or banner. Use it to decide whether
     *   surrounding chrome (dividers, spacing) should be visible.
     */
    @JvmOverloads
    fun show(
        activity: Activity,
        container: FrameLayout,
        placement: NativePlacement,
        style: AdSlotStyle,
        prefer: AdSlotPreference = AdSlotPreference.NATIVE_FIRST,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        if (!adsController.isEnabled(AdType.NATIVE) && !adsController.isEnabled(AdType.BANNER)) {
            collapse(container)
            onResult?.invoke(false)
            return
        }

        val views = prepare(activity, container)

        if (prefer == AdSlotPreference.BANNER_FIRST) {
            showBannerFirst(activity, container, views, placement, style, onResult)
            return
        }

        if (!adsController.isEnabled(AdType.NATIVE)) {
            // Native disabled remotely; go straight to the banner rather than a doomed attempt.
            showBannerFallback(activity, container, views, style, onResult)
            return
        }

        showNative(activity, container, views, placement, style, onResult)
    }

    /**
     * Banner first, native on failure - the reverse of the default.
     *
     * Banner fill is higher, so this trades native's better rate for a slot that is more often
     * occupied. Worth using where an empty slot would leave a visible hole in the layout.
     */
    private fun showBannerFirst(
        activity: Activity,
        container: FrameLayout,
        views: SlotViews,
        placement: NativePlacement,
        style: AdSlotStyle,
        onResult: ((Boolean) -> Unit)?
    ) {
        if (!adsController.isEnabled(AdType.BANNER)) {
            showNative(activity, container, views, placement, style, onResult)
            return
        }
        bannerRefresher.start(
            activity = activity,
            container = views.adContainer,
            shimmer = views.shimmer,
            size = if (style == AdSlotStyle.MEDIUM) BannerSize.MEDIUM_RECTANGLE else BannerSize.ADAPTIVE,
            onFirstResult = { filled ->
                if (filled) {
                    onResult?.invoke(true)
                } else {
                    bannerRefresher.stop(views.adContainer)
                    showNative(activity, container, views, placement, style, onResult)
                }
            }
        )
    }

    private fun showNative(
        activity: Activity,
        container: FrameLayout,
        views: SlotViews,
        placement: NativePlacement,
        style: AdSlotStyle,
        onResult: ((Boolean) -> Unit)?
    ) {
        if (!adsController.isEnabled(AdType.NATIVE)) {
            collapse(container)
            onResult?.invoke(false)
            return
        }
        val theme = remoteTheme()
        val builder = views.adContainer.createNativeAdBuilderAutoShimmer(
            shimmerHost = views.shimmerHost,
            layoutResId = style.nativeLayout,
            theme = theme,
            showMedia = style.showMedia,
            showBody = style.showMedia,
            iconEnabled = true
        )

        adsController.manager.nativeAdLoader.loadAndShow(
            adUnitId = adsController.units.native(placement),
            builder = builder
        ) { success ->
            if (success) {
                views.shimmerHost.visibility = View.GONE
                onResult?.invoke(true)
            } else {
                Log.d(TAG, "native did not fill for $style, falling back to banner")
                showBannerFallback(activity, container, views, style, onResult)
            }
        }
    }

    /**
     * Builds the native ad theme from the `native_config` parameter.
     *
     * A single palette applied in both light and dark mode - not `NativeAdTheme.auto()`. The
     * remote parameter defines five exact colours, so what is set in the console is what renders;
     * picking a system-derived palette half the time would make the parameter unpredictable. Only
     * the stroke and shimmer colours come from the library's light defaults, since `native_config`
     * says nothing about them.
     */
    private fun remoteTheme(): NativeAdTheme {
        val colors = RemoteConfigManager.getNativeAdColors()
        return NativeAdTheme.light(
            adBgColor = colors.backgroundColor,
            adTitleColor = colors.heading,
            adBodyColor = colors.description,
            ctaBgColor = colors.callActionButtonColor,
            ctaTextColor = colors.ctaText
        )
    }

    /** Releases the banner refresh timer for [container]. Call from the screen's `onDestroy`. */
    fun release(container: FrameLayout) {
        bannerRefresher.stop(container)
    }

    private fun showBannerFallback(
        activity: Activity,
        container: FrameLayout,
        views: SlotViews,
        style: AdSlotStyle,
        onResult: ((Boolean) -> Unit)?
    ) {
        if (!adsController.isEnabled(AdType.BANNER)) {
            collapse(container)
            onResult?.invoke(false)
            return
        }
        views.adContainer.removeAllViews()
        // Banners self-refresh on an interval, so the refresher owns the show call.
        bannerRefresher.start(
            activity = activity,
            container = views.adContainer,
            shimmer = views.shimmer,
            size = if (style == AdSlotStyle.MEDIUM) BannerSize.MEDIUM_RECTANGLE else BannerSize.ADAPTIVE,
            onFirstResult = { filled ->
                if (!filled) collapse(container)
                onResult?.invoke(filled)
            }
        )
    }

    /**
     * Builds the shimmer host and ad container inside the screen's slot, once.
     *
     * Doing it here rather than in every screen layout keeps the slot markup to a single
     * FrameLayout and guarantees the shimmer matches the native layout being requested.
     */
    private fun prepare(activity: Activity, container: FrameLayout): SlotViews {
        (container.getTag(R.id.ad_slot_views_tag) as? SlotViews)?.let { existing ->
            existing.adContainer.removeAllViews()
            return existing
        }

        container.visibility = View.VISIBLE
        container.removeAllViews()

        val shimmerHost = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val adContainer = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(shimmerHost)
        container.addView(adContainer)

        val views = SlotViews(shimmerHost, adContainer)
        container.setTag(R.id.ad_slot_views_tag, views)
        return views
    }

    private fun collapse(container: FrameLayout) {
        container.removeAllViews()
        container.visibility = View.GONE
        container.setTag(R.id.ad_slot_views_tag, null)
    }

    private class SlotViews(
        val shimmerHost: FrameLayout,
        val adContainer: FrameLayout
    ) {
        /** The shimmer the library inflated into [shimmerHost], if any. */
        val shimmer: ShimmerFrameLayout?
            get() = shimmerHost.getChildAt(0) as? ShimmerFrameLayout
    }

    private companion object {
        const val TAG = "AdsSlot"
    }
}
