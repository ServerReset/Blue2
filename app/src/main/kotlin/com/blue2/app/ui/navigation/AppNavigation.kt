package com.blue2.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.blue2.app.ui.screens.diagnostics.DiagnosticsScreen
import com.blue2.app.ui.screens.home.HomeScreen
import com.blue2.app.ui.screens.login.LoginScreen
import com.blue2.app.ui.screens.settings.SettingsScreen

object Destinations {
    const val LOGIN = "login"
    const val HOME = "home"
    const val DIAGNOSTICS = "diagnostics/{vin}"
    const val SETTINGS = "settings"

    fun diagnostics(vin: String) = "diagnostics/$vin"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(350, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(350, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(350, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(350, easing = FastOutSlowInEasing)
            )
        },
    ) {
        composable(
            Destinations.LOGIN,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(400)) },
        ) {
            LoginScreen(onLoggedIn = {
                navController.navigate(Destinations.HOME) {
                    popUpTo(Destinations.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Destinations.HOME) {
            HomeScreen(
                onDiagnostics = { vin -> navController.navigate(Destinations.diagnostics(vin)) },
                onSettings = { navController.navigate(Destinations.SETTINGS) },
            )
        }

        composable(Destinations.DIAGNOSTICS) { backStack ->
            val vin = backStack.arguments?.getString("vin") ?: return@composable
            DiagnosticsScreen(
                vin = vin,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Destinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
