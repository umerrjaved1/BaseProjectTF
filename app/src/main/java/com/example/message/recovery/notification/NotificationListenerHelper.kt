package com.example.message.recovery.notification

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationListenerHelper {

    fun isEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        if (enabledPackages.contains(packageName)) return true

        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        val component = ComponentName(context, AppNotificationListenerService::class.java)
        return enabled.split(":").any { value ->
            value.equals(component.flattenToString(), ignoreCase = true) ||
                value.contains(packageName)
        }
    }
}
