package com.example.message.recovery.ui.navigation

import javax.inject.Inject
import javax.inject.Singleton

enum class NavScreenKind {
    Start,
    Language,
    Onboarding,
    Survey,
    Premium,
    Home,
    Media,
    Status,
    Settings,
    Unknown,
}

@Singleton
class CurrentNavDestination @Inject constructor() {

    @Volatile
    var kind: NavScreenKind = NavScreenKind.Unknown

    fun isAppOpenExcluded(): Boolean = when (kind) {
        NavScreenKind.Start,
        NavScreenKind.Language,
        NavScreenKind.Onboarding,
        NavScreenKind.Premium,
        NavScreenKind.Unknown,
        -> true
        NavScreenKind.Survey,
        NavScreenKind.Home,
        NavScreenKind.Media,
        NavScreenKind.Status,
        NavScreenKind.Settings,
        -> false
    }
}
