package com.tf.phonecleaner.booster.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val fbLogger = AppEventsLogger.newLogger(context)

    // Firebase event action constants
    object Action {
        const val VIEW = "view"
        const val OPENED = "opened"
        const val CLOSED = "closed"
        const val CLICKED = "clicked"
        const val ACTION_TYPE: String = "action_type"
    }

    object Events {
        // Splash
        const val SPLASH_VIEW = "splash_view"

        // Pro Panel
        const val PRO_VIEW = "pro_view"
        const val PRO_VIEW_SPLASH = "pro_view_splash"
        const val PRO_CLCK = "pro_clck"
        const val PRO_CROSS = "pro_cross"
        const val PURCHASE = "purchase"

        // App Resume
        const val APP_RESUME = "app_resume"
        const val APPOPEN_REQUEST = "appopen_request"
        const val APPOPEN_REQUEST_PASS = "appopen_request_pass"
        const val APPOPEN_REQUEST_FAIL = "appopen_request_fail"
        const val APPOPEN_VIEW = "appopen_view"

        // Language
        const val LNG_SCR_VIEW = "lng_scr_view"
        const val LNG_SELECTED = "lng_selected"
        const val LNG_SCR_NEXT = "lng_scr_next"

        // Onboarding
        const val OB4_GET_STARTED = "ob4_get_started"

        // Home
        const val HOME_VIEW = "home_view"
    }


    fun sendAnalytics(actionDetail: String, actionName: String) {
        val modifiedString: String =
            actionName.sanitize()

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, actionDetail)
        bundle.putString(Action.ACTION_TYPE, modifiedString)
        Log.d("AnalyticsManager", "sendAnalytics: $modifiedString $actionDetail")
        firebaseAnalytics.logEvent(modifiedString, bundle)
    }

    fun sendEvent(key: String, bundle: Bundle) {
        val modifiedString: String =
            key.sanitize()
        Log.d("Analytics", "sendEvent: $modifiedString $bundle")
        firebaseAnalytics.logEvent(modifiedString, bundle)
    }

    /**
     * Log Facebook App Event specifically
     */
    fun logFacebookEvent(eventName: String, bundle: Bundle? = null) {
        if (bundle != null) {
            fbLogger.logEvent(eventName, bundle)
        } else {
            fbLogger.logEvent(eventName)
        }
    }

    /**
     * Helper to explicitly log standard Meta 'ad imp' and 'start trial'
     */
    fun logMetaAdImpression() {
        logFacebookEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION)
    }

    fun logMetaStartTrial() {
        logFacebookEvent(AppEventsConstants.EVENT_NAME_START_TRIAL)
    }

    /**
     * Sanitizes event names to conform to Firebase naming rules.
     */
    private fun String.sanitize(): String = this.trim().replace("\\s+".toRegex(), "_").lowercase()
}

