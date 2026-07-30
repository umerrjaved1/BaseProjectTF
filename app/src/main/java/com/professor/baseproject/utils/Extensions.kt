package com.professor.baseproject.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow
import android.content.Intent
import com.google.firebase.analytics.FirebaseAnalytics
import com.professor.baseproject.R
import com.professor.baseproject.app.AppPreferences
import com.professor.baseproject.constants.Constants
import com.professor.baseproject.constants.AppConfigDefaults
import com.professor.baseproject.ui.screens.PremiumActivity


/**

Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 12:47 pm
Email: umerr8019@gmail.com

 */

// mainCoroutine() / ioCoroutine() were removed. Each created a fresh
// CoroutineScope(Dispatchers.X) that was never cancelled and had no
// CoroutineExceptionHandler, so any throw inside crashed the process and the job
// outlived whatever Activity it had captured. Use lifecycleScope,
// viewLifecycleOwner.lifecycleScope, or viewModelScope instead.


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


/** Walks the ContextWrapper chain to find the hosting Activity, or null. */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}


/**
 * Counts user interactions and opens the paywall after N of them.
 *
 * Extracted out of the click extension so the behaviour has a name, can be reset
 * between tests, and — critically — can no longer crash: it resolves a real Activity
 * before starting one. The previous inline version called
 * `view.context.startActivity(...)`, which throws
 * `AndroidRuntimeException: Calling startActivity() from outside of an Activity`
 * whenever a view was inflated with a non-Activity context.
 */
object PaywallTrigger {

    private var interactionCount = 0
    private var shownThisSession = false

    fun onUserInteraction(view: View) {
        if (shownThisSession) return

        val activity = view.context.findActivity() ?: return
        val prefs = activity.getSharedPreferences(
            AppPreferences.PREF_NAME,
            Context.MODE_PRIVATE
        )
        val isPremium = prefs.getBoolean(AppPreferences.IS_PREMIUM, false)
        // IS_FIRST_RUN_COMPLETE, not IS_ONBOARDING: the onboarding screen can be
        // disabled from remote config, in which case its flag is never written and
        // this gate would stay shut forever.
        val isFirstRunComplete = prefs.getBoolean(AppPreferences.IS_FIRST_RUN_COMPLETE, false)
        if (isPremium || !isFirstRunComplete) return

        if (!AppConfigDefaults.SHOW_PREMIUM_AFTER_CLICKS) return

        interactionCount++
        if (interactionCount < AppConfigDefaults.PREMIUM_CLICK_COUNT) return

        shownThisSession = true
        activity.startActivity(
            Intent(activity, PremiumActivity::class.java)
                .putExtra(Constants.EXTRA_PREMIUM_FROM_ICON, true)
        )
    }

    /** Call from tests, or on sign-out / entitlement change. */
    fun resetSession() {
        interactionCount = 0
        shownThisSession = false
    }
}


/**
 * Debounced click listener with no side effects. Prefer this for new code.
 */
fun View.onDebouncedClick(
    timeoutMillis: Long = 200L,
    onClick: (View) -> Unit
) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime < timeoutMillis) return@setOnClickListener
        lastClickTime = now
        onClick(view)
    }
}


/**
 * Debounced click that also logs analytics and feeds [PaywallTrigger].
 *
 * Analytics now emit ONE event name (`ui_click`) with the screen and view as
 * parameters. The previous version built a *dynamic* event name per view
 * (`"${screen}_${view}"`), and Firebase caps a project at 500 distinct event names —
 * so a few dozen screens silently exhausted the quota and lost all click analytics.
 */
fun View.setClickWithTimeout(
    timeoutMillis: Long = 200L,
    onClick: (View) -> Unit
) {
    onDebouncedClick(timeoutMillis) { view ->
        val viewName = runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull() ?: "unknown"
        val screenName = view.context.findActivity()?.javaClass?.simpleName ?: "unknown"

        FirebaseAnalytics.getInstance(view.context).logEvent(
            EVENT_UI_CLICK,
            Bundle().apply {
                putString("view_name", viewName.take(40))
                putString("screen_name", screenName.take(40))
            }
        )

        PaywallTrigger.onUserInteraction(view)
        onClick(view)
    }
}

private const val EVENT_UI_CLICK = "ui_click"


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

/**
 * Periodically shakes the view while [owner] is at least STARTED.
 *
 * Takes a LifecycleOwner rather than a bare CoroutineScope: the previous version ran
 * `while (true) { delay(); startAnimation() }` in whatever scope it was handed, so it
 * kept animating a stopped Activity and retained the View until the scope died.
 */
fun View.startShakeAnimation(
    owner: LifecycleOwner,
    delayMillis: Long = 3000L
) {
    val view = this
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val shakeAnim = android.view.animation.AnimationUtils.loadAnimation(
                view.context,
                R.anim.shake
            )
            while (true) {
                delay(delayMillis)
                view.startAnimation(shakeAnim)
            }
        }
    }
}
