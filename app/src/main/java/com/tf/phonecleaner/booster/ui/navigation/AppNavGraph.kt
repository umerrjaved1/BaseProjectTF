package com.tf.phonecleaner.booster.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tf.phonecleaner.booster.ui.screens.compose.LanguageScreen
import com.tf.phonecleaner.booster.ui.screens.compose.MainScreen
import com.tf.phonecleaner.booster.ui.screens.compose.OnboardingScreen
import com.tf.phonecleaner.booster.ui.screens.compose.PremiumScreen
import com.tf.phonecleaner.booster.ui.screens.compose.StartScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            StartScreen(
                onNavigateToLanguage = { navController.navigate(Screen.Language.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                onNavigateToMain = { navController.navigate(Screen.Main.route) { popUpTo(Screen.Splash.route) { inclusive = true } } }
            )
        }
        
        composable(Screen.Language.route) {
            LanguageScreen(
                onNavigateNext = { navController.navigate(Screen.Onboarding.route) { popUpTo(Screen.Language.route) { inclusive = true } } }
            )
        }
        
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } } },
                onNavigateToMain = { navController.navigate(Screen.Main.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } } }
            )
        }
        
        composable(Screen.Premium.route) {
            PremiumScreen(
                onNavigateNext = { navController.navigate(Screen.Main.route) { popUpTo(Screen.Premium.route) { inclusive = true } } }
            )
        }
        
        composable(Screen.Main.route) {
            MainScreen()
        }
    }
}
