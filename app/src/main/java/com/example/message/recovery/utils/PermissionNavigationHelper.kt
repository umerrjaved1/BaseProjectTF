package com.example.message.recovery.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.message.recovery.app.MyApp
import com.example.message.recovery.notification.AppNotificationListenerService

object PermissionNavigationHelper {

    fun openNotificationListenerSettings(context: Context) {
        MyApp.ignoreNextResume = true
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /**
     * POST_NOTIFICATIONS only exists from Android 13; below that the permission is granted at
     * install time, so treat it as held.
     */
    fun hasPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notificationListenerComponentName(context: Context) =
        android.content.ComponentName(context, AppNotificationListenerService::class.java)
}
