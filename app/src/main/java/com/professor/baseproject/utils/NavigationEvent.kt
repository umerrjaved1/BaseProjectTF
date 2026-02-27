package com.professor.baseproject.utils

sealed class NavigationEvent {
    object Main : NavigationEvent()
    data class Premium(val fromOnboarding: Boolean = false) : NavigationEvent()
    object MainWithInterstitial : NavigationEvent()
    object Onboarding : NavigationEvent()
    
}