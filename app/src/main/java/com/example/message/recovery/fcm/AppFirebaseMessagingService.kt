package com.example.message.recovery.fcm

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.message.recovery.R
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.ui.screens.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    companion object {
        private const val TAG = "FCMService"

        // Intent extra keys for payload passing
        const val EXTRA_FROM_FCM = "extra_from_fcm"
        const val EXTRA_TARGET_SCREEN = "target_screen"
        const val EXTRA_TARGET_URL = "url"
        const val EXTRA_NOTIFICATION_TITLE = "notification_title"
        const val EXTRA_NOTIFICATION_BODY = "notification_body"

        // Target screen values
        const val TARGET_HOME = "home"
        const val TARGET_SETTINGS = "settings"
        const val TARGET_PREMIUM = "premium"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        try {
            appPreferences.setString(AppPreferences.FCM_TOKEN, token)
            analyticsManager.sendAnalytics(
                AnalyticsManager.Action.ACTION_TYPE,
                "fcm_token_refreshed"
            )
            // Ensure subscription is refreshed with new token
            FcmManager.subscribeToTopic(FcmManager.DEFAULT_TOPIC)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new FCM token", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Log message data
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Extract Title & Body (prefer remoteMessage.notification if present, fallback to data payload)
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: ""

        // Extract Image URL
        val imageUrl = remoteMessage.notification?.imageUrl?.toString()
            ?: remoteMessage.data["image"]
            ?: remoteMessage.data["image_url"]

        showNotification(
            title = title,
            body = body,
            imageUrl = imageUrl,
            dataPayload = remoteMessage.data
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        imageUrl: String?,
        dataPayload: Map<String, String>
    ) {
        // Ensure channel exists
        FcmManager.createNotificationChannels(applicationContext)

        val channelId = getString(R.string.fcm_default_channel_id)

        // Create Intent to open MainActivity with extras
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_FCM, true)
            putExtra(EXTRA_NOTIFICATION_TITLE, title)
            putExtra(EXTRA_NOTIFICATION_BODY, body)

            dataPayload.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val requestCode = (System.currentTimeMillis() % 10000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setColor(ContextCompat.getColor(this, R.color.accent_color))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        // Load image if available
        if (!imageUrl.isNullOrBlank()) {
            val bitmap = downloadBitmap(imageUrl)
            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap)
                notificationBuilder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setSummaryText(body)
                )
            }
        }

        // Check POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission not granted, skipping notification display.")
            return
        }

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        try {
            NotificationManagerCompat.from(this).notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "Notification displayed successfully with ID: $notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying notification", e)
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        return try {
            runBlocking {
                withTimeout(5_000) {
                    val request = ImageRequest.Builder(applicationContext)
                        .data(url)
                        .allowHardware(false)
                        .build()
                    applicationContext.imageLoader.execute(request).image?.toBitmap()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load notification image from $url: ${e.message}")
            null
        }
    }
}
