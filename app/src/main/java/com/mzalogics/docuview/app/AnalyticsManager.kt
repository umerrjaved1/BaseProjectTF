package com.mzalogics.docuview.app

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
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    // Firebase event action constants
    object Action {
        const val VIEW = "view"
        const val OPENED = "opened"
        const val CLOSED = "closed"
        const val CLICKED = "clicked"
        const val ACTION_TYPE: String = "action_type"
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
     * Sanitizes event names to conform to Firebase naming rules.
     */
    private fun String.sanitize(): String = this.trim().replace("\\s+".toRegex(), "_").lowercase()
}

