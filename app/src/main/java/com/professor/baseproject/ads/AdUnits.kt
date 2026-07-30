package com.professor.baseproject.ads

import android.content.Context
import com.professor.baseproject.BuildConfig
import com.professor.baseproject.R
import com.professor.baseproject.remoteconfig.RemoteConfigManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A native ad slot, one per screen that carries one. Each has its own remote ad unit id. */
enum class NativePlacement { LANGUAGE, ONBOARDING, INTEREST, HOME }

/**
 * An interstitial trigger. Split so the exit interstitial can be measured - and turned off -
 * independently of the in-app ones.
 */
enum class InterstitialPlacement { HOME, EXIT }

/**
 * Single source of truth for AdMob ad unit ids.
 *
 * ### Resolution order
 * 1. **Debug build** - always Google's test ids. Real impressions from a developer's device are
 *    invalid traffic, and AdMob suspends accounts for it; relying on "remember to swap the ids
 *    back" is how that happens by accident.
 * 2. **The `ad_ids` Remote Config parameter**, so a unit can be replaced without a release.
 * 3. **`res/values/ad_units.xml`** - the offline fallback, used on a first cold start before
 *    Remote Config has ever resolved.
 *
 * Every id is a `get()`, not a stored value: Remote Config resolves after `MyApp.onCreate`, so
 * anything snapshotted at construction would hold the fallback for the whole session.
 */
@Singleton
class AdUnits @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val ids get() = RemoteConfigManager.getAdIds()

    val appOpen: String
        get() = resolve(ids.appOpen, R.string.ad_unit_app_open, TEST_APP_OPEN)

    /** One unit for every banner shape - adaptive, medium rectangle and collapsible. */
    val banner: String
        get() = resolve(ids.banner, R.string.ad_unit_banner, TEST_BANNER)

    val homeInterstitial: String
        get() = resolve(ids.homeInterstitial, R.string.ad_unit_home_interstitial, TEST_INTERSTITIAL)

    val exitInterstitial: String
        get() = resolve(ids.exitInterstitial, R.string.ad_unit_exit_interstitial, TEST_INTERSTITIAL)

    fun interstitial(placement: InterstitialPlacement): String = when (placement) {
        InterstitialPlacement.HOME -> homeInterstitial
        InterstitialPlacement.EXIT -> exitInterstitial
    }

    fun native(placement: NativePlacement): String = when (placement) {
        NativePlacement.LANGUAGE ->
            resolve(ids.languageNative, R.string.ad_unit_language_native, TEST_NATIVE)

        NativePlacement.ONBOARDING ->
            resolve(ids.onboardingNative, R.string.ad_unit_onboarding_native, TEST_NATIVE)

        NativePlacement.INTEREST ->
            resolve(ids.interestNative, R.string.ad_unit_interest_native, TEST_NATIVE)

        NativePlacement.HOME ->
            resolve(ids.homeNative, R.string.ad_unit_home_native, TEST_NATIVE)
    }

    private fun resolve(remoteId: String, fallbackRes: Int, testId: String): String = when {
        BuildConfig.DEBUG -> testId
        remoteId.isNotBlank() -> remoteId
        else -> context.getString(fallbackRes)
    }

    private companion object {
        // https://developers.google.com/admob/android/test-ads
        const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
        const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
        const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    }
}
