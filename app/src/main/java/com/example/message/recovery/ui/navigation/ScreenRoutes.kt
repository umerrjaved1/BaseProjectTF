package com.example.message.recovery.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute

@Serializable
data object Start : AppRoute

@Serializable
data class Language(val fromStart: Boolean = true) : AppRoute

@Serializable
data object Onboarding : AppRoute

@Serializable
data class Survey(val fromHome: Boolean = false) : AppRoute

@Serializable
data class Premium(
    val fromSplash: Boolean = false,
    val fromSurvey: Boolean = false,
    val fromIcon: Boolean = false,
    val fromOnboarding: Boolean = false,
    val fromResume: Boolean = false,
) : AppRoute

@Serializable
data object Home : AppRoute

@Serializable
data object Media : AppRoute

@Serializable
data object Status : AppRoute

@Serializable
data object Settings : AppRoute
