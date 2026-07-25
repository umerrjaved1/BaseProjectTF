package com.tf.gpsmapcamera.remoteconfig

object RemoteConfigKeys {
    const val CONFIG_START_SCREEN = "splash_screen"
    const val CONFIG_LANGUAGE_SCREEN = "language_screen"
    const val CONFIG_ONBOARDING_SCREEN = "onboarding_screen"
    const val CONFIG_PREMIUM_SCREEN = "premium_screen"
    const val CONFIG_SURVEY_SCREEN = "survey_screen"

    /** Legacy remote key name — kept so existing Firebase console values keep resolving. */
    const val CONFIG_GLOBAL = "global_ad_rules"

    const val FIRST_LAUNCH = "first_launch"
    const val NOTIFICATION_DELAY_TIME = "notification_delay_time"
    const val NOTIFICATION_REPEAT_INTERVAL = "notification_time"
    const val ENABLE_REPEATING_NOTIFICATIONS = "enable_repeating_notifications"
    const val NOTIFICATION_TIME = "notification_time"

}
