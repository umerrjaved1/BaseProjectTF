package com.example.message.recovery.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNavigator @Inject constructor() {

    var controller: NavHostController? = null

    /** Bumped before a locale recreate so NavHost does not restore Language. */
    var navHostGeneration: Int = 0
        private set

    private var pendingStartAfterRecreate: AppRoute? = null

    fun navigate(route: AppRoute) {
        controller?.navigate(route)
    }

    /**
     * Drop the current back stack and show [route]. Does not use `popUpTo(graph.id)` —
     * that tears down the graph and NavHost puts [Start] back, which then routes to Language.
     */
    fun replaceAll(route: AppRoute) {
        val nav = controller
        if (nav == null) {
            android.util.Log.w("AppNavigator", "replaceAll($route) dropped - no controller attached")
            return
        }
        android.util.Log.i("AppNavigator", "replaceAll -> $route")
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun prepareLocaleRecreate(nextRoute: AppRoute) {
        pendingStartAfterRecreate = nextRoute
        navHostGeneration++
        controller = null
    }

    fun consumePendingStart(): AppRoute? {
        val route = pendingStartAfterRecreate
        pendingStartAfterRecreate = null
        return route
    }

    fun pop(): Boolean = controller?.popBackStack() == true

    fun openPremiumFromIcon() {
        controller?.navigate(Premium(fromIcon = true))
    }

    fun navigateTab(route: AppRoute) {
        val nav = controller ?: return
        nav.navigate(route) {
            popUpTo<Home> {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    companion object {
        @Volatile
        var instance: AppNavigator? = null
    }

    init {
        instance = this
    }
}
