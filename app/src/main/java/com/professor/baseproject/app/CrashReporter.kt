package com.professor.baseproject.app

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over Crashlytics.
 *
 * The dependency was linked but **never called from anywhere**, so crashes arrived with no
 * breadcrumbs and every `catch` block in the project swallowed its throwable into `Log`.
 * A logcat line is invisible in production; this makes the same information reachable.
 *
 * Use:
 *  - [breadcrumb] for "what was happening" — attached to the next crash in that session.
 *  - [nonFatal]   for handled failures you still want to see (a parse failure, a cache
 *                 write that fell through). These show up under Crashlytics > Non-fatals.
 *  - [setKey]     for state you want on every report (entitlement, locale, variant).
 */
@Singleton
class CrashReporter @Inject constructor() {

    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    /** Leaves a trail on the session; shows up in the crash's log section. */
    fun breadcrumb(tag: String, message: String) {
        Log.d(tag, message)
        crashlytics.log("[$tag] $message")
    }

    /**
     * Reports a handled exception. Always also logs locally so debugging does not depend
     * on the Firebase console.
     */
    fun nonFatal(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        crashlytics.log("[$tag] $message")
        crashlytics.recordException(throwable ?: HandledFailure(message))
    }

    fun setKey(key: String, value: String) = crashlytics.setCustomKey(key, value)

    fun setKey(key: String, value: Boolean) = crashlytics.setCustomKey(key, value)

    fun setKey(key: String, value: Int) = crashlytics.setCustomKey(key, value)

    /**
     * Records the state worth having on *every* report. Called from MyApp at startup and
     * again whenever entitlement changes, since "was this user premium?" is the first
     * question asked about most billing and paywall reports.
     */
    fun setBaseContext(isPremium: Boolean, languageCode: String, buildVariant: String) {
        setKey(KEY_IS_PREMIUM, isPremium)
        setKey(KEY_LANGUAGE, languageCode.ifBlank { "system" })
        setKey(KEY_VARIANT, buildVariant)
    }

    /** The screen the user is on. Updated from MyApp's activity lifecycle callbacks. */
    fun setCurrentScreen(name: String) = setKey(KEY_SCREEN, name)

    /** Wrapper so [nonFatal] can report a message with no underlying throwable. */
    class HandledFailure(message: String) : Exception(message)

    companion object {
        const val KEY_IS_PREMIUM = "is_premium"
        const val KEY_LANGUAGE = "language"
        const val KEY_VARIANT = "build_variant"
        const val KEY_SCREEN = "current_screen"
    }
}
