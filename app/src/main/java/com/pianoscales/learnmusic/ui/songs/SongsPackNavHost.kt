package com.pianoscales.learnmusic.ui.songs

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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
                onStartSong = { song ->
                    navController.navigate("${SongsPackRoute.Coach.route}/${song.songId}")
                }
            )
        }
        composable(
            route = "${SongsPackRoute.Coach.route}/{songId}",
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) {
            SongCoachScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
