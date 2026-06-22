package com.tf.phonecleaner.booster.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Language : Screen("language")
    object Onboarding : Screen("onboarding")
    object Premium : Screen("premium")
    object Main : Screen("main")
}
