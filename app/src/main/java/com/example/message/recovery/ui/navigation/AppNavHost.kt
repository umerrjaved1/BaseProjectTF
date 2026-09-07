package com.example.message.recovery.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.example.message.recovery.app.AnalyticsManager
import com.example.message.recovery.app.AppPreferences
import com.example.message.recovery.ui.screens.language.LanguageRoute
import com.example.message.recovery.ui.screens.main.HomeTab
import com.example.message.recovery.ui.screens.main.MainNavigation
import com.example.message.recovery.ui.screens.main.MainContract
import com.example.message.recovery.ui.screens.main.MediaTab
import com.example.message.recovery.ui.screens.main.SettingsTab
import com.example.message.recovery.ui.screens.main.StatusTab
import com.example.message.recovery.ui.screens.onboarding.OnboardingRoute
import com.example.message.recovery.ui.screens.premium.PremiumRoute
import com.example.message.recovery.ui.screens.start.StartRoute
import com.example.message.recovery.ui.screens.survey.SurveyRoute
import com.example.message.recovery.ui.theme.prefersNavigationRail
import com.example.message.recovery.ui.theme.windowSize
import com.umer_tf.ads.domain.core.AdMobManager
import dagger.Lazy

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: AppRoute,
    navigator: AppNavigator,
    currentNavDestination: CurrentNavDestination,
    adMobManagerLazy: Lazy<AdMobManager>,
    adMobManager: AdMobManager,
    analyticsManagerLazy: Lazy<AnalyticsManager>,
    analyticsManager: AnalyticsManager,
    appPreferences: AppPreferences,
    eventHandler: MainContract.EventHandler,
    onMainVisible: () -> Unit,
) {
    DisposableEffect(navController) {
        navigator.controller = navController
        onDispose {
            if (navigator.controller === navController) navigator.controller = null
        }
    }

    val entry by navController.currentBackStackEntryAsState()
    val dest = entry?.destination
    val kind = dest.screenKind()
    val isMainTab = kind == NavScreenKind.Home ||
        kind == NavScreenKind.Media ||
        kind == NavScreenKind.Status ||
        kind == NavScreenKind.Settings
    val showNavigation = isMainTab

    LaunchedEffect(kind) {
        currentNavDestination.kind = kind
        if (isMainTab) onMainVisible()
    }

    // Wide screens put navigation in a side rail; narrow ones keep the bottom bar. Both wrap the
    // same NavHost, so nothing below this point knows which arrangement is in use.
    val useRail = windowSize().prefersNavigationRail
    AdaptiveNavScaffold(
        showNavigation = showNavigation,
        useRail = useRail,
        navigation = {
            MainNavigation(
                current = kind.toMainTab(),
                useRail = useRail,
                onTabSelected = eventHandler.onTabSelected,
            )
        },
    ) { contentModifier ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = contentModifier,
        ) {
            composable<Start> {
                StartRoute(
                    navigator = navigator,
                    adMobManagerLazy = adMobManagerLazy,
                    analyticsManager = analyticsManagerLazy,
                    appPreferences = appPreferences,
                )
            }
            composable<Language> { backStackEntry ->
                val route = backStackEntry.toRoute<Language>()
                LanguageRoute(
                    fromStart = route.fromStart,
                    navigator = navigator,
                    adMobManager = adMobManager,
                    analyticsManager = analyticsManager,
                    appPreferences = appPreferences,
                )
            }
            composable<Onboarding> {
                OnboardingRoute(
                    navigator = navigator,
                    adMobManager = adMobManager,
                    analyticsManager = analyticsManager,
                    appPreferences = appPreferences,
                )
            }
            composable<Survey> { backStackEntry ->
                val route = backStackEntry.toRoute<Survey>()
                SurveyRoute(
                    fromHome = route.fromHome,
                    navigator = navigator,
                    adMobManager = adMobManager,
                    analyticsManager = analyticsManager,
                    appPreferences = appPreferences,
                )
            }
            composable<Premium> { backStackEntry ->
                val route = backStackEntry.toRoute<Premium>()
                PremiumRoute(
                    fromSplash = route.fromSplash,
                    fromSurvey = route.fromSurvey,
                    fromIcon = route.fromIcon,
                    fromOnboarding = route.fromOnboarding,
                    fromResume = route.fromResume,
                    navigator = navigator,
                    adMobManager = adMobManager,
                    analyticsManager = analyticsManager,
                    appPreferences = appPreferences,
                )
            }
            composable<Home> {
                HomeTab(
                    onRequestListenerAccess = eventHandler.onRequestListenerAccess,
                    onBack = eventHandler.onHomeBack,
                    onPremium = eventHandler.onPremium,
                    onAddApps = eventHandler.onAddApps,
                )
            }
            composable<Media> {
                MediaTab(onBack = eventHandler.onSettingsBack)
            }
            composable<Status> {
                StatusTab(onBack = eventHandler.onSettingsBack)
            }
            composable<Settings> {
                SettingsTab(
                    onPremium = eventHandler.onPremium,
                    onLanguage = eventHandler.onLanguage,
                    onBackToHome = eventHandler.onSettingsBack,
                )
            }
        }
    }
}

/**
 * Places [navigation] beside the content on wide screens and below it on narrow ones. The content
 * lambda receives the modifier that makes it fill the remaining space, so the caller does not have
 * to know whether it ended up inside a [Row] or a [Column].
 */
@Composable
private fun AdaptiveNavScaffold(
    showNavigation: Boolean,
    useRail: Boolean,
    navigation: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    if (showNavigation && useRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            navigation()
            content(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            content(Modifier.weight(1f))
            if (showNavigation) navigation()
        }
    }
}

private fun androidx.navigation.NavDestination?.screenKind(): NavScreenKind {
    val name = this?.route
        ?.substringBefore('/')
        ?.substringBefore('?')
        ?.substringAfterLast('.')
        .orEmpty()
    return when (name) {
        "Start" -> NavScreenKind.Start
        "Language" -> NavScreenKind.Language
        "Onboarding" -> NavScreenKind.Onboarding
        "Survey" -> NavScreenKind.Survey
        "Premium" -> NavScreenKind.Premium
        "Home" -> NavScreenKind.Home
        "Media" -> NavScreenKind.Media
        "Status" -> NavScreenKind.Status
        "Settings" -> NavScreenKind.Settings
        else -> NavScreenKind.Unknown
    }
}

private fun NavScreenKind.toMainTab(): AppRoute = when (this) {
    NavScreenKind.Media -> Media
    NavScreenKind.Status -> Status
    NavScreenKind.Settings -> Settings
    else -> Home
}

