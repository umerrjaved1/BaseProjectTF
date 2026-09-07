package com.example.message.recovery.model

data class SurveyItem(
    val id: Int,
    val name: String,
    val iconResId: Int,
    val bgResource: Int,
    var isSelected: Boolean = false
)
