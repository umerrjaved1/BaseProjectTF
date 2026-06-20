package com.tf.phonecleaner.booster.model

import androidx.annotation.Keep

@Keep
data class LanguageModel(
    val id: Int,
    val flag: Int = 0,
    val name: String,
    val code: String
)
