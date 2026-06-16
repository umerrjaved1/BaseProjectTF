package com.mzalogics.docuview.utils

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.View
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow
import android.content.Intent
import com.google.firebase.analytics.FirebaseAnalytics
import com.umer_tf.ads.ads .domain.core.AdMobManager
import com.mzalogics.docuview.app.AppPreferences
import com.mzalogics.docuview.constants.Constants
import com.mzalogics.docuview.remoteconfig.RemoteConfigManager
import com.mzalogics.docuview.ui.screens.PremiumActivity

/**

Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 12:47 pm
Email: umerr8019@gmail.com

 */


fun mainCoroutine(work: suspend (() -> Unit)): Job {
    return CoroutineScope(Dispatchers.Main).launch {
        work()
    }
}


fun ioCoroutine(work: suspend (() -> Unit)): Job {
    return CoroutineScope(Dispatchers.IO).launch {
        work()
    }
}


inline fun <reified T> Gson.fromJsonWithType(json: String): T {
    val type: Type = object : TypeToken<T>() {}.type
    return this.fromJson(json, type)
}


inline fun <reified T> List<LinkedTreeMap<String, Any>>.toTypedList(): List<T> {
    val gson = Gson()
    return this.map { mapItem ->
        val json = gson.toJson(mapItem) // Convert map to JSON string
        gson.fromJson(json, T::class.java) // Convert JSON to object
    }
}


fun <T> Gson.fromJsonList(json: String, type: Type): List<T> {
    return this.fromJson(json, type)
}


object SessionClickManager {
    var globalClickCount = 0
    var hasShownPremiumThisSession = false
}

fun View.setClickWithTimeout(
    timeoutMillis: Long = 100L,
    onClick: (View) -> Unit
) {
    var lastClickTime = 0L

    setOnClickListener { view ->
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime < timeoutMillis) {
            return@setOnClickListener
        }
        lastClickTime = currentTime

        // Firebase click event
        val viewName = try {
            view.resources.getResourceEntryName(view.id)
        } catch (_: Exception) {
            "unknown"
        }
        val screenName = (view.context as? android.app.Activity)?.javaClass?.simpleName ?: "unknown"
        val eventName = "${screenName}_${viewName}".take(40)
        Log.d("ClickEvent", "click_event: $eventName")
        FirebaseAnalytics.getInstance(view.context)
            .logEvent(eventName, android.os.Bundle().apply {
                putString("view_name", viewName.take(40))
                putString("screen_name", screenName.take(40))
            })

        if (!AdMobManager.isPremium && !SessionClickManager.hasShownPremiumThisSession) {

            val prefs = view.context.getSharedPreferences(
                AppPreferences.Companion.PREF_NAME,
                Context.MODE_PRIVATE
            )
            val isOnboardingComplete = prefs.getBoolean(AppPreferences.Companion.IS_ONBOARDING, false)


            if (isOnboardingComplete) {

                SessionClickManager.globalClickCount++

                if (SessionClickManager.globalClickCount >= RemoteConfigManager.getAdsConfig().clickCountPremiumActivity &&
                    RemoteConfigManager.getShowPremiumActivityAfterThreeClick()
                ) {
                    SessionClickManager.hasShownPremiumThisSession = true
                    view.context.startActivity(
                        Intent(view.context, PremiumActivity::class.java).apply {
                            putExtra(Constants.EXTRA_PREMIUM_FROM_ICON, true)
                        }
                    )
                }
            }
        }

        onClick(view)
    }
}


// ViewExtensions.kt
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

// FileExtensions.kt
fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(this / 1024.0.pow(digitGroups)) + " " + units[digitGroups]
}



