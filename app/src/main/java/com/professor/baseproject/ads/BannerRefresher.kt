package com.professor.baseproject.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import com.umer_tf.ads.domain.analytics.AdType
import javax.inject.Inject
import javax.inject.Singleton

/** Banner shape to request. */
enum class BannerSize { ADAPTIVE, MEDIUM_RECTANGLE, COLLAPSIBLE }

/**
 * Reloads banner ads on an interval, per container.
 *
 * The ad plan asks for a 30-second refresh. AdMob can do this server-side via the ad unit's
 * refresh-rate setting, which is the better option when available - **set it in the AdMob console
 * and leave this alone.** This exists for units where server-side refresh is not configured, and
 * for the banner-fallback slots that [AdsSlot] creates, where the container is built at runtime.
 *
 * ### Policy
 * AdMob requires at least 30 seconds between manual refreshes. [intervalMs] clamps to that floor
 * regardless of the remote value, so a mistaken `bannerRefreshSeconds: 5` cannot turn into an
 * invalid-traffic problem.
 *
 * ### Lifecycle
 * Timers are keyed by container and must be stopped, or a backgrounded screen keeps requesting
 * ads. [stop] from `onDestroy`, [pauseAll] / [resumeAll] from `onPause` / `onResume`. The
 * underlying `AdView` still needs `bannerAdLoader.pause()` and `resume()`; this only governs
 * whether a *new* request is made.
 */
@Singleton
class BannerRefresher @Inject constructor(
    private val adsController: AdsController
) {

    private val handler = Handler(Looper.getMainLooper())
    private val active = mutableMapOf<FrameLayout, Runnable>()
    private var isPaused = false

    private val intervalMs: Long
        get() = RemoteConfigManager.getAdRules()
            .bannerRefreshSeconds.coerceAtLeast(MIN_REFRESH_SECS) * 1000L

    /**
     * Shows a banner in [container] now, then again every [intervalMs].
     *
     * Calling it again for the same container replaces the existing timer rather than stacking a
     * second one - which is what would otherwise happen on a configuration change.
     */
    fun start(
        activity: Activity,
        container: FrameLayout,
        shimmer: ShimmerFrameLayout? = null,
        size: BannerSize = BannerSize.ADAPTIVE,
        onFirstResult: ((Boolean) -> Unit)? = null
    ) {
        stop(container)

        if (!adsController.isEnabled(AdType.BANNER)) {
            onFirstResult?.invoke(false)
            return
        }

        var reportedFirst = false
        val loader = adsController.manager.bannerAdLoader

        val tick = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed) {
                    stop(container)
                    return
                }
                // One unit id for all three shapes: `ad_ids` has a single `banner` entry, and an
                // AdMob banner unit serves any requested size.
                val unitId = adsController.units.banner
                when (size) {
                    BannerSize.ADAPTIVE -> loader.showAdaptiveBanner(
                        activity, shimmer, container, unitId
                    )

                    BannerSize.MEDIUM_RECTANGLE -> loader.showMemRecBanner(
                        activity, container, shimmer, unitId
                    )

                    BannerSize.COLLAPSIBLE -> loader.showCollapsableBanner(
                        activity, container, shimmer, unitId, false
                    )
                }
                if (!reportedFirst) {
                    reportedFirst = true
                    // The loader reports fill through AdEventListener, not a per-call callback, so
                    // this reports "a request went out" rather than "an ad rendered". The slot
                    // stays laid out either way; the library hides the container on failure.
                    onFirstResult?.invoke(true)
                }
                if (!isPaused) handler.postDelayed(this, intervalMs)
            }
        }

        active[container] = tick
        handler.post(tick)
        Log.d(TAG, "start: $size banner refreshing every ${intervalMs}ms")
    }

    /** Stops refreshing [container] and destroys its AdView. */
    fun stop(container: FrameLayout) {
        active.remove(container)?.let { handler.removeCallbacks(it) }
        adsController.manager.bannerAdLoader.destroyFor(container)
    }

    /** Suspends every timer. Pair with the host's `onPause`. */
    fun pauseAll() {
        isPaused = true
        active.values.forEach { handler.removeCallbacks(it) }
        adsController.manager.bannerAdLoader.pause()
    }

    /** Resumes every timer, refreshing immediately. Pair with the host's `onResume`. */
    fun resumeAll() {
        isPaused = false
        adsController.manager.bannerAdLoader.resume()
        active.values.forEach { handler.post(it) }
    }

    /** Stops everything. Pair with the host's `onDestroy`. */
    fun releaseAll() {
        active.values.forEach { handler.removeCallbacks(it) }
        active.clear()
        adsController.manager.bannerAdLoader.destroy()
    }

    private companion object {
        const val TAG = "BannerRefresher"

        /** AdMob's minimum manual refresh interval. */
        const val MIN_REFRESH_SECS = 30L
    }
}
