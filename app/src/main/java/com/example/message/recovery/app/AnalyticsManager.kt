package com.example.message.recovery.app

import android.content.Context
import android.os.Bundle
import android.util.Log

import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }

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
        const val SPLASH_NATIVE = "splash_native"

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
        const val OB1_VIEW = "ob1_view"
        const val OB2_VIEW = "ob2_view"
        const val OB3_VIEW = "ob3_view"
        const val OB4_VIEW = "ob4_view"
        const val OB4_GET_STARTED = "ob4_get_started"

        // Survey
        const val SURVEY_SCR_VIEW = "survey_scr_view"
        const val FEATURE_SELECTED = "feature_selected"
        const val SURVEY_SCR_DONE = "survey_scr_done"

        // Home
        const val HOME_VIEW = "home_view"
        const val HOME_SCREEN_VIEW = "home_screen_view"
        const val PRO_BADGE_CLICK = "pro_badge_click"
        const val TAB_CLICK = "tab_click"
        const val START_DOWNLOAD_CLK = "start_download_clk"

        // Search
        const val LINK_INPUT_FOCUS = "link_input_focus"
        const val LINK_PASTE_CLICK = "link_paste_click"
        const val LINK_SUBMIT = "link_submit"
        const val LINK_INVALID = "link_invalid"
        const val VIDEO_FETCH_START = "video_fetch_start"
        const val VIDEO_FETCH_SUCCESS = "video_fetch_success"
        const val VIDEO_FETCH_FAILED = "video_fetch_failed"

        // Download
        const val QUALITY_SELECT = "quality_select"
        const val DOWNLOAD_START = "download_start"
        const val DOWNLOAD_PROGRESS = "download_progress"
        const val DOWNLOAD_COMPLETE = "download_complete"
        const val DOWNLOAD_SUCCESS = "download_success"
        const val DOWNLOAD_FAILED = "download_failed"
        const val DOWNLOAD_CANCEL = "download_cancel"

        // Explore
        const val EXPLORE_SITE_CLICK = "explore_site_click"
        const val BROWSER_OPEN = "browser_open"
        const val BROWSER_VIDEO_DETECTED = "browser_video_detected"
        const val BROWSER_DOWNLOAD_TAP = "browser_download_tap"

        // Shorts
        const val SHORTS_TAB_OPEN = "shorts_tab_open"
        const val SHORTS_VIDEO_VIEW = "shorts_video_view"
        const val SHORTS_VIDEO_COMPLETE = "shorts_video_complete"
        const val SHORTS_DOWNLOAD = "shorts_download"
        const val SHORTS_SCROLL = "shorts_scroll"

        // Downloads
        const val DOWNLOADS_TAB_OPEN = "downloads_tab_open"
        const val DOWNLOADED_VIDEO_PLAY = "downloaded_video_play"
        const val DOWNLOADED_VIDEO_SHARE = "downloaded_video_share"
        const val DOWNLOADED_VIDEO_DELETE = "downloaded_video_delete"

        // Settings
        const val SETTINGS_OPEN = "settings_open"
        const val SETTING_CHANGED = "setting_changed"
        const val RATE_US_CLICK = "rate_us_click"

        // Drama
        const val DRAMA_SCREEN_VIEW = "drama_screen_view"
        const val DRAMA_CATEGORY_CLICK = "drama_category_click"
        const val DRAMA_ITEM_CLICK = "drama_item_click"
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

    }



    /**
     * Sanitizes event names to conform to Firebase naming rules.
     */
    private fun String.sanitize(): String = this.trim().replace("\\s+".toRegex(), "_").lowercase()
}

