package com.pianoscales.learnmusic.ui.pianobuddy

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class PianoBuddyRoute(val route: String) {
    object Home : PianoBuddyRoute("pb_home")
    object Freestyle : PianoBuddyRoute("pb_freestyle")
}

@Composable
fun PianoBuddyNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = PianoBuddyRoute.Home.route
    ) {
        composable(PianoBuddyRoute.Home.route) {
            PianoBuddyHomeScreen(
                onNavigateToFreestyle = {
                    navController.navigate(PianoBuddyRoute.Freestyle.route)
                }
            )
        }
        composable(PianoBuddyRoute.Freestyle.route) {
            PianoBuddyFreestyleScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
