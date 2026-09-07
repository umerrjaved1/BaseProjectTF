package com.example.message.recovery.notification

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class AppNotificationListenerService : NotificationListenerService() {

    inner class LocalBinder : Binder() {
        fun getService(): AppNotificationListenerService = this@AppNotificationListenerService
    }

    private val localBinder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return if (SERVICE_INTERFACE == intent.action) {
            super.onBind(intent)
        } else {
            localBinder
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")
        Log.d(TAG, "Posted from ${sbn.packageName}: $title")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        private const val TAG = "NotifListener"
        @Volatile
        var isConnected: Boolean = false
            private set
    }
}
