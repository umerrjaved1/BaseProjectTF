package com.example.message.recovery.utils

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.coroutineScope
import com.facebook.shimmer.ShimmerFrameLayout
import com.umer_tf.ads.domain.ads.native_ad.NativeAdBuilder
import com.umer_tf.ads.domain.core.AdMobManager
import com.umer_tf.ads.domain.utils.LoadingDialogUtil
import com.example.message.recovery.R
import com.example.message.recovery.app.AdIds
import com.example.message.recovery.remoteconfig.RemoteConfigManager
import com.example.message.recovery.remoteconfig.data.NativeAdConfigData
import com.example.message.recovery.app.AnalyticsManager
import androidx.lifecycle.Lifecycle
import com.example.message.recovery.app.MyApp
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

/**

Created by Umer Javed
Senior Android Developer
Created on 02/09/2025 3:00 pm
Email: umerr8019@gmail.com

 */
object AdUtils {

    /**
     * Master switch for ads. Checks both premium status and global remote config.
     */
    fun areAdsEnabled(): Boolean {
        if (AdMobManager.isPremium) return false
        return RemoteConfigManager.getShowAds()
    }

    /**
     * Pushes Remote Config ad rules and unit IDs into the ads SDK controller.
     * Call on splash setup and when entering Home so resume/open timers and IDs stay in sync.
     */
    fun applyRemoteAdControllerConfig(adMobManager: AdMobManager) {
        val rules = RemoteConfigManager.getAdRules()
        adMobManager
            .setInterstitialAdMaxTime(rules.interstitialMaxTimer.toLong())
            .setInterstitialAdMinTime(rules.interstitialMinTimer.toLong())
            .setInterstitialCounter(rules.interstitialCounter)
            .setOpenAdResumeTime(rules.openAdResumeTimer.toLong())
            .setPremium(AdMobManager.isPremium)
            .setAppOpenAdStartId(AdIds.getAppOpenAdId())
            .setAppOpenAdResumeId(AdIds.getAppResumeAdId())
    }

    fun applyCtaBgFallback(
        adContainer: FrameLayout, colorValue: String, ctaButtonId: Int = R.id.ad_call_to_action
    ) {
        AdUIHelper.applyCtaBgFallback(adContainer, colorValue, ctaButtonId)
    }

    fun applyAdBackgroundColorFallback(
        adContainer: FrameLayout, colorValue: String
    ) {
        AdUIHelper.applyAdBackgroundColorFallback(adContainer, colorValue)
    }

    /**
     * Solid, guaranteed fallback specifically for ViewPager2 Native Ads (Onboarding) and Splash
     * It bypasses the SDK builder completely and forcibly applies all colors immediately when the NativeAdView inflates.
     */
    fun solidApplyNativeAdColors(
        adContainer: FrameLayout, nativeConfig: NativeAdConfigData?
    ) {
        AdUIHelper.solidApplyNativeAdColors(adContainer, nativeConfig)
    }

    fun loadAndShowInterSplash(
        adMobManager: AdMobManager,
        lifecycle: Lifecycle,
        activity: AppCompatActivity,
        adUnit: String,
        lifecycleScope: LifecycleCoroutineScope,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        /**
         * Reports the load result, before the ad is shown. [onComplete] only fires once the ad has
         * been *dismissed*, so it is too late for a caller that needs to react to the ad merely
         * being ready — the splash progress bar being the case this exists for. Called exactly
         * once, on the UI thread.
         */
        onLoaded: (Boolean) -> Unit = {},
        onComplete: (Boolean) -> Unit = {}
    ) {
        val TAG = "SplashNav"

        fun complete(shown: Boolean) {
            Log.i(TAG, "loadAndShowInterSplash complete shown=$shown state=${lifecycle.currentState}")
            activity.runOnUiThread { onComplete(shown) }
        }

        fun reportLoaded(isLoaded: Boolean) {
            activity.runOnUiThread { onLoaded(isLoaded) }
        }

        if (adUnit.isBlank()) {
            Log.w(TAG, "loadAndShowInterSplash: empty ad unit")
            reportLoaded(false)
            complete(false)
            return
        }

        eventNamePrefix?.let {
            analyticsManager?.sendAnalytics(
                AnalyticsManager.Action.ACTION_TYPE, "${it}_request"
            )
        }

        Log.i(TAG, "loadAndShowInterSplash load unit=$adUnit")
        adMobManager.interstitialAdLoader.loadAd(adUnit) { isLoaded ->
            Log.i(TAG, "loadAndShowInterSplash loaded=$isLoaded finishing=${activity.isFinishing} destroyed=${activity.isDestroyed} state=${lifecycle.currentState}")
            reportLoaded(isLoaded)
            if (!isLoaded) {
                eventNamePrefix?.let {
                    analyticsManager?.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE, "${it}_fail"
                    )
                }
                complete(false)
                return@loadAd
            }

            if (activity.isFinishing || activity.isDestroyed) {
                complete(false)
                return@loadAd
            }

            eventNamePrefix?.let {
                analyticsManager?.sendAnalytics(
                    AnalyticsManager.Action.ACTION_TYPE, "${it}_pass"
                )
                analyticsManager?.sendAnalytics(
                    AnalyticsManager.Action.ACTION_TYPE, "${it}_view"
                )
            }

            fun showNow() {
                if (activity.isFinishing || activity.isDestroyed) {
                    complete(false)
                    return
                }
                Log.i(TAG, "loadAndShowInterSplash showAd state=${lifecycle.currentState}")
                MyApp.ignoreNextResume = true
                adMobManager.interstitialAdLoader.showAd(activity, adUnit) {
                    Log.i(TAG, "loadAndShowInterSplash showAd callback state=${lifecycle.currentState}")
                    complete(true)
                }
            }

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                showNow()
            } else {
                lifecycleScope.launch {
                    repeat(40) {
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            showNow()
                            return@launch
                        }
                        delay(100)
                    }
                    Log.w(TAG, "loadAndShowInterSplash resume wait timed out, showing anyway")
                    showNow()
                }
            }
        }
    }

    private var lastInterAdTime: Long = 0

    fun loadAndShowAdWithTimer(
        activity: AppCompatActivity,
        lifecycle: Lifecycle,
        lifecycleScope: LifecycleCoroutineScope,
        adMobManager: AdMobManager,
        adUnit: String = AdIds.getInterstitialAdID(),
        timeOut: Long = RemoteConfigManager.getAdRules().interstitialMinTimer * 1000L,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        ignoreFrequency: Boolean = false,
        onNavigate: () -> Unit
    ) {
        // The startup flow navigates from here, so this callback has to fire exactly once no
        // matter what the ad SDK does. Its dismissal callback is not reliable: the loader can log
        // the ad as dismissed without ever invoking the lambda passed to showAd(), which used to
        // strand the user on the screen that started the transition (most visibly the onboarding
        // permission page, whose button then did nothing because it had already been consumed).
        // StartRoute worked around the same thing locally with its ON_START/ON_RESUME fallbacks;
        // doing it here covers every startup transition instead.
        val navigated = AtomicBoolean(false)
        val go = {
            if (navigated.compareAndSet(false, true)) onNavigate()
        }

        if (!areAdsEnabled()) {
            go()
            return
        }

        val currentTime = SystemClock.elapsedRealtime()
        if (ignoreFrequency || currentTime - lastInterAdTime > timeOut) {
            eventNamePrefix?.let { prefix ->
                analyticsManager?.sendAnalytics(
                    AnalyticsManager.Action.ACTION_TYPE, "${prefix}_request"
                )
            }
            val loadingDialogUtil = LoadingDialogUtil.create(activity)
            loadingDialogUtil.showLoadingDialog(false)

            adMobManager.interstitialAdLoader.loadAd(
                adUnit,
            ) { isLoaded ->

                if (isLoaded && !activity.isFinishing && !activity.isDestroyed) {
                    eventNamePrefix?.let { prefix ->
                        analyticsManager?.sendAnalytics(
                            AnalyticsManager.Action.ACTION_TYPE, "${prefix}_pass"
                        )
                        analyticsManager?.sendAnalytics(
                            AnalyticsManager.Action.ACTION_TYPE, "${prefix}_view"
                        )
                    }
                    lifecycleScope.launch {
                        while (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED).not()) {
                            delay(100)
                        }
                        MyApp.ignoreNextResume = true
                        adMobManager.interstitialAdLoader.showAd(activity, adUnit) {
                            lastInterAdTime = SystemClock.elapsedRealtime()
                            go()
                        }
                        loadingDialogUtil.hideLoadingDialog()
                        watchForAdDismissal(lifecycle, adUnit) {
                            lastInterAdTime = SystemClock.elapsedRealtime()
                            go()
                        }
                    }
                } else {
                    eventNamePrefix?.let { prefix ->
                        analyticsManager?.sendAnalytics(
                            AnalyticsManager.Action.ACTION_TYPE, "${prefix}_fail"
                        )
                    }
                    loadingDialogUtil.hideLoadingDialog()
                    go()
                }
            }
        } else {
            go()
        }
    }

    /**
     * Backstop for [loadAndShowAdWithTimer]. An interstitial takes the foreground, which pauses the
     * host activity; when the ad goes away the host resumes. Watching that transition tells us the
     * ad is finished without having to trust the SDK's dismissal callback.
     *
     * Two cases end the wait:
     *  - the host paused (ad took over) and has now resumed again — the ad has been dismissed;
     *  - the host never paused within [AD_DISPLAY_GRACE_MS] — the ad failed to display at all.
     *
     * [ABSOLUTE_TIMEOUT_MS] is a final guard so a user can never be stranded, however long they
     * spend on the ad. Callbacks are deduplicated by the caller, so a normal dismissal that does
     * fire the SDK callback simply wins the race and this becomes a no-op.
     */
    private fun watchForAdDismissal(
        lifecycle: Lifecycle,
        adUnit: String,
        onFinished: () -> Unit,
    ) {
        lifecycle.coroutineScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            var hostPaused = false
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                val resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (!resumed) hostPaused = true
                if (resumed && hostPaused) {
                    Log.i(WATCHDOG_TAG, "ad dismissed (host resumed) for $adUnit, continuing")
                    break
                }
                if (!hostPaused && elapsed > AD_DISPLAY_GRACE_MS) {
                    Log.w(WATCHDOG_TAG, "ad never took the foreground for $adUnit, continuing")
                    break
                }
                if (elapsed > ABSOLUTE_TIMEOUT_MS) {
                    Log.w(WATCHDOG_TAG, "ad watchdog timed out for $adUnit, continuing")
                    break
                }
                delay(AD_WATCHDOG_POLL_MS)
            }
            onFinished()
        }
    }

    private const val WATCHDOG_TAG = "AdWatchdog"
    private const val AD_DISPLAY_GRACE_MS = 5_000L
    private const val ABSOLUTE_TIMEOUT_MS = 3 * 60_000L
    private const val AD_WATCHDOG_POLL_MS = 150L

    fun loadAndShowNativeAd(
        adMobManager: AdMobManager,
        activity: Activity,
        adUnitId: String,
        layoutResId: Int,
        frameLayout: FrameLayout,
        shimmerFrameLayout: ShimmerFrameLayout,
        showMedia: Boolean = true,
        showInfoIcon: Boolean = true,
        nativeConfig: NativeAdConfigData? = null,
        forceLoadNew: Boolean = false,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        onAdLoaded: ((Boolean) -> Unit)? = null
    ) {
        if (!areAdsEnabled()) {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            frameLayout.visibility = android.view.View.GONE
            onAdLoaded?.invoke(false)
            return
        }

        val builder = NativeAdBuilder.Builder(
            layoutResId, frameLayout, shimmerFrameLayout
        ).setShowMedia(showMedia).setShowBody(true).setIconEnabled(true).setShowRating(false)

        nativeConfig?.let {
            builder.setAdTitleColor(it.heading)
            builder.setAdBodyColor(it.description)
            builder.setCtaTextColor(it.ctaText)
            builder.setCtaBgColor(it.callActionButtonColor)
        }

        if (!forceLoadNew && adMobManager.nativeAdLoader.isAdLoaded()) {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            frameLayout.visibility = android.view.View.VISIBLE

            adMobManager.nativeAdLoader.showLoadedAd(builder.build(), adUnitId, activity)
            nativeConfig?.callActionButtonColor?.let { color ->
                applyCtaBgFallback(frameLayout, color)
            }
            nativeConfig?.backgroundColor?.let { color ->
                applyAdBackgroundColorFallback(frameLayout, color)
            }
            onAdLoaded?.invoke(true)
            return
        }

        frameLayout.visibility = android.view.View.GONE
        if (forceLoadNew) {
            frameLayout.removeAllViews()
        }
        shimmerFrameLayout.visibility = android.view.View.VISIBLE
        shimmerFrameLayout.startShimmer()

        eventNamePrefix?.let { prefix ->
            analyticsManager?.sendAnalytics(
                AnalyticsManager.Action.ACTION_TYPE, "${prefix}_request"
            )
        }

        adMobManager.nativeAdLoader.loadAndShow(
            adUnitId, builder.build(), activity
        ) { success ->
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = android.view.View.GONE
            if (success) {
                eventNamePrefix?.let { prefix ->
                    analyticsManager?.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE, "${prefix}_pass"
                    )
                    analyticsManager?.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE, "${prefix}_view"
                    )
                }
                frameLayout.visibility = android.view.View.VISIBLE
                nativeConfig?.callActionButtonColor?.let { color ->
                    applyCtaBgFallback(frameLayout, color)
                }
                nativeConfig?.backgroundColor?.let { color ->
                    applyAdBackgroundColorFallback(frameLayout, color)
                }
            } else {
                eventNamePrefix?.let { prefix ->
                    analyticsManager?.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE, "${prefix}_fail"
                    )
                }
                frameLayout.visibility = android.view.View.GONE
            }
            onAdLoaded?.invoke(success)
        }
    }

    fun loadAndShowFullScreenNativeAdWithDialog(
        activity: AppCompatActivity,
        adUnitId: String,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String? = null,
        nativeConfig: NativeAdConfigData? = NativeAdConfigData(),
        onComplete: (Boolean) -> Unit
    ) {
        if (!areAdsEnabled()) {
            onComplete(false)
            return
        }

        val dialog = android.app.Dialog(activity, R.style.Theme_App)
        dialog.setContentView(R.layout.layout_full_native_ad)
        dialog.setCancelable(false)

        dialog.window?.let {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, true)
        }

        val adFrame = dialog.findViewById<FrameLayout>(R.id.adFrame)
        val shimmerFbAd = dialog.findViewById<ShimmerFrameLayout>(R.id.shimmerFbAd)
        val fallbackContainer =
            dialog.findViewById<android.widget.LinearLayout>(R.id.fallbackContainer)
        val btnContinue = dialog.findViewById<android.widget.TextView>(R.id.btnContinue)
        val btnCloseAd = dialog.findViewById<android.widget.ImageView>(R.id.btnCloseAd)

        dialog.show()

        btnCloseAd.setOnClickListener {
            dialog.dismiss()
            onComplete(true)
        }

        btnContinue.setOnClickListener {
            dialog.dismiss()
            onComplete(false)
        }

        val builder = NativeAdBuilder.Builder(
            R.layout.full_native_ad_design, adFrame, shimmerFbAd
        ).setShowMedia(true)

            .setShowBody(true).setShowRating(false).setIconEnabled(true)

        nativeConfig?.let {
            builder.setAdTitleColor(it.heading)
            builder.setAdBodyColor(it.description)
            builder.setCtaTextColor(it.ctaText)
            builder.setCtaBgColor(it.callActionButtonColor)
        }

        val loader = com.umer_tf.ads.domain.ads.native_ad.NativeAd(activity)
        eventNamePrefix?.let { prefix ->
            analyticsManager?.sendAnalytics(
                AnalyticsManager.Action.ACTION_TYPE, "${prefix}_request"
            )
        }
        loader.loadAndShow(adUnitId, builder.build(), activity) { success ->
            if (success) {
                eventNamePrefix?.let { prefix ->
                    analyticsManager?.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE, "${prefix}_pass"
                    )
                    analyticsManager?.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE, "${prefix}_view"
                    )
                }
                btnCloseAd.visibility = android.view.View.VISIBLE

                AdUtils.solidApplyNativeAdColors(adFrame, nativeConfig)
            } else {
                eventNamePrefix?.let { prefix ->
                    analyticsManager?.sendAnalytics(
                        AnalyticsManager.Action.ACTION_TYPE, "${prefix}_fail"
                    )
                }
                shimmerFbAd.stopShimmer()
                shimmerFbAd.visibility = android.view.View.GONE
                adFrame.visibility = android.view.View.GONE
                fallbackContainer.visibility = android.view.View.VISIBLE
                btnContinue.visibility = android.view.View.VISIBLE
                btnCloseAd.visibility = android.view.View.VISIBLE

                dialog.dismiss()
                onComplete(false)
            }
        }
    }

    /**
     * Show a timer-gated interstitial ad on back press and then run [onAfterAd] (e.g. finish()).
     */
    fun showBackPressInterstitial(
        activity: AppCompatActivity,
        adMobManager: AdMobManager,
        lifecycle: Lifecycle,
        lifecycleScope: LifecycleCoroutineScope,
        shouldShow: Boolean,
        adId: String,
        analyticsManager: AnalyticsManager? = null,
        eventNamePrefix: String = "back_inter",
        ignoreFrequency: Boolean = false,
        onAfterAd: () -> Unit
    ) {
        AdNavigationHelper.showBackPressInterstitial(
            activity,
            adMobManager,
            lifecycle = lifecycle,
            lifecycleScope = lifecycleScope,
            shouldShow,
            adId,
            analyticsManager,
            eventNamePrefix,
            ignoreFrequency,
            onAfterAd
        )
    }


}
