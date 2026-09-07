package com.example.message.recovery.notification

data class WatchedApp(
    val id: Int,
    val packageName: String,
    val displayName: String,
    val letter: String,
    val avatarColor: Long,
)

object WatchedApps {
    const val WHATSAPP = "com.whatsapp"
    const val MESSENGER = "com.facebook.orca"
    const val INSTAGRAM = "com.instagram.android"
    const val SNAPCHAT = "com.snapchat.android"
    const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    val catalog: List<WatchedApp> = listOf(
        WatchedApp(1, WHATSAPP, "WhatsApp", "W", 0xFFC8E6C9),
        WatchedApp(2, MESSENGER, "Messenger", "M", 0xFFBBDEFB),
        WatchedApp(3, INSTAGRAM, "Instagram", "I", 0xFFDCEDC8),
        WatchedApp(4, SNAPCHAT, "Snapchat", "S", 0xFFB3E5FC),
        WatchedApp(5, WHATSAPP_BUSINESS, "WhatsApp Business", "W", 0xFFB3E5FC),
    )

    val defaultSelectedPackages: Set<String> = setOf(WHATSAPP, MESSENGER, INSTAGRAM)

    fun byPackage(packageName: String): WatchedApp? = catalog.find { it.packageName == packageName }
}
