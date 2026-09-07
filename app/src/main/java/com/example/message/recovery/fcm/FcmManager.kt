package com.example.message.recovery.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.example.message.recovery.R
import com.example.message.recovery.app.AppPreferences

object FcmManager {

    private const val TAG = "FcmManager"
    const val DEFAULT_TOPIC = "all"

    /**
     * Creates notification channels for FCM push notifications (required on Android 8.0+ / API 26+).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = context.getString(R.string.fcm_default_channel_id)
            val channelName = context.getString(R.string.fcm_default_channel_name)
            val channelDescription = context.getString(R.string.fcm_default_channel_desc)

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
            Log.d(TAG, "FCM Notification Channel created: $channelId")
        }
    }

    /**
     * Initializes FCM: retrieves current registration token, persists it in AppPreferences,
     * and subscribes the device to the default broadcast topic.
     */
    fun initFcm(context: Context, appPreferences: AppPreferences) {
        createNotificationChannels(context)

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "FCM Token: $token")
                    if (!token.isNullOrEmpty()) {
                        appPreferences.setString(AppPreferences.FCM_TOKEN, token)
                    }
                } else {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                }
            }

        // Subscribe to default "all" topic for broadcast campaigns
        subscribeToTopic(DEFAULT_TOPIC)
    }

    /**
     * Subscribes to a given FCM topic.
     */
    fun subscribeToTopic(topic: String, onComplete: ((Boolean) -> Unit)? = null) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                val isSuccess = task.isSuccessful
                if (isSuccess) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                } else {
                    Log.w(TAG, "Failed to subscribe to topic: $topic", task.exception)
                }
                onComplete?.invoke(isSuccess)
            }
    }

    /**
     * Unsubscribes from a given FCM topic.
     */
    fun unsubscribeFromTopic(topic: String, onComplete: ((Boolean) -> Unit)? = null) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                val isSuccess = task.isSuccessful
                if (isSuccess) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                } else {
                    Log.w(TAG, "Failed to unsubscribe from topic: $topic", task.exception)
                }
                onComplete?.invoke(isSuccess)
            }
    }
}
