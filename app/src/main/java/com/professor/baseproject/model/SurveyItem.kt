package com.professor.baseproject.model

data class SurveyItem(
    val id: Int,
    val name: String,
    val iconResId: Int,
    val bgColorHex: String,
    var isSelected: Boolean = false
)
