package com.tf.gpsmapcamera.model

import androidx.annotation.Keep

@Keep
data class OnboardingItem(
    val title: String,
    val description: String,
    val imageRes: Int
)
