package com.tf.gpsmapcamera.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.tf.gpsmapcamera.R
import com.tf.gpsmapcamera.remoteconfig.data.NativeAdConfigData

object AdUIHelper {

    fun applyCtaBgFallback(
        adContainer: FrameLayout,
        colorValue: String,
        ctaButtonId: Int = R.id.ad_call_to_action
    ) {
        val parsedColor = runCatching { Color.parseColor(colorValue) }.getOrNull() ?: return
        adContainer.post {
            val cta = adContainer.findViewById<AppCompatButton>(ctaButtonId) ?: return@post
            val tint = ColorStateList.valueOf(parsedColor)
            cta.backgroundTintList = tint
            cta.supportBackgroundTintList = tint
        }
    }

    fun applyAdBackgroundColorFallback(
        adContainer: FrameLayout,
        colorValue: String
    ) {
        val parsedColor = runCatching { Color.parseColor(colorValue) }.getOrNull() ?: return
        adContainer.post {
            val clAd = adContainer.findViewById<View>(R.id.clAd)
            clAd?.setBackgroundColor(parsedColor)
            val llBottomPanel = adContainer.findViewById<View>(R.id.llBottomPanel)
            llBottomPanel?.setBackgroundColor(parsedColor)
        }
    }

    /**
     * Solid, guaranteed fallback specifically for ViewPager2 Native Ads (Onboarding) and Splash
     * It bypasses the SDK builder completely and forcibly applies all colors immediately when the NativeAdView inflates.
     */
    fun solidApplyNativeAdColors(
        adContainer: FrameLayout,
        nativeConfig: NativeAdConfigData?
    ) {
        if (nativeConfig == null) return

        val applyColors = {
            val view = adContainer
            // Background
            val parsedBgColor = runCatching { Color.parseColor(nativeConfig.backgroundColor) }.getOrNull()
            if (parsedBgColor != null) {
                view.findViewById<View>(R.id.clAd)?.setBackgroundColor(parsedBgColor)
                view.findViewById<View>(R.id.llBottomPanel)?.setBackgroundColor(parsedBgColor)
            }
            // CTA Background & Text Color
            val parsedCtaBg = runCatching { Color.parseColor(nativeConfig.callActionButtonColor) }.getOrNull()
            val parsedCtaText = runCatching { Color.parseColor(nativeConfig.ctaText) }.getOrNull()
            val cta = view.findViewById<AppCompatButton>(R.id.ad_call_to_action)
            if (cta != null) {
                if (parsedCtaBg != null) {
                    val tint = ColorStateList.valueOf(parsedCtaBg)
                    cta.backgroundTintList = tint
                    cta.supportBackgroundTintList = tint
                }
                if (parsedCtaText != null) {
                    cta.setTextColor(parsedCtaText)
                }
            }
            // Title Text Color
            val parsedTitle = runCatching { Color.parseColor(nativeConfig.heading) }.getOrNull()
            if (parsedTitle != null) {
                view.findViewById<TextView>(R.id.ad_headline)?.setTextColor(parsedTitle)
            }
            // Body Text Color
            val parsedBody = runCatching { Color.parseColor(nativeConfig.description) }.getOrNull()
            if (parsedBody != null) {
                view.findViewById<TextView>(R.id.ad_body)?.setTextColor(parsedBody)
            }
        }

        applyColors()

        adContainer.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                applyColors()
            }
            override fun onChildViewRemoved(parent: View?, child: View?) {}
        })
    }
}
