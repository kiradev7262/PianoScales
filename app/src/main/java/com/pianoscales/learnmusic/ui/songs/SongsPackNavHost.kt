package com.pianoscales.learnmusic.ui.songs

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class SongsPackRoute(val route: String) {
    object Home : SongsPackRoute("songs_pack_home")
    object Coach : SongsPackRoute("songs_pack_coach")
}

@Composable
fun SongsPackNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SongsPackRoute.Home.route
    ) {
        composable(SongsPackRoute.Home.route) {
            SongsPackScreen(
                onStartSong = { 
                    navController.navigate(SongsPackRoute.Coach.route)
                }
            )
        }
        composable(SongsPackRoute.Coach.route) {
            SongCoachScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
