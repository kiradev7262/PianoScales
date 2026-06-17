package com.pianoscales.learnmusic.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pianoscales.learnmusic.audio_intelligence.AudioIntelligenceNavHost
import com.pianoscales.learnmusic.navigation.JourneyNavHost
import com.pianoscales.learnmusic.ui.freestyle.FreestyleScreen
import com.pianoscales.learnmusic.ui.me.MeNavHost
import com.pianoscales.learnmusic.ui.theme.CardSurface
import com.pianoscales.learnmusic.ui.theme.PrimaryAccent
import com.pianoscales.learnmusic.ui.theme.TextMuted

sealed class BottomNavScreen(val route: String, val label: String, val icon: ImageVector) {
    object Journey : BottomNavScreen("journey_root", "Journey", Icons.Default.Home)
    object Freestyle : BottomNavScreen("freestyle_root", "Freestyle", Icons.Default.PlayArrow)
    object AudioIntelligence : BottomNavScreen("audio_intelligence_root", "Audio AI", Icons.Default.Star)
    object Me : BottomNavScreen("me_root", "Me", Icons.Default.Person)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavScreen.Journey,
        BottomNavScreen.Freestyle,
        BottomNavScreen.AudioIntelligence,
        BottomNavScreen.Me
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isFreestyle = currentDestination?.hierarchy?.any { it.route == BottomNavScreen.Freestyle.route } == true
    val showBottomBar = !(isLandscape && isFreestyle)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = CardSurface,
                    contentColor = PrimaryAccent
                ) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryAccent,
                                selectedTextColor = PrimaryAccent,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = PrimaryAccent.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Journey.route,
            modifier = Modifier.padding(
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp
            )
        ) {
            composable(
                route = BottomNavScreen.Journey.route + "?subRoute={subRoute}",
                arguments = listOf(navArgument("subRoute") { nullable = true; defaultValue = null })
            ) { backStackEntry ->
                val subRoute = backStackEntry.arguments?.getString("subRoute")
                JourneyNavHost(initialSubRoute = subRoute)
            }
            composable(BottomNavScreen.Freestyle.route) {
                FreestyleScreen()
            }
            composable(
                route = BottomNavScreen.AudioIntelligence.route + "?subRoute={subRoute}",
                arguments = listOf(navArgument("subRoute") { nullable = true; defaultValue = null })
            ) { backStackEntry ->
                val subRoute = backStackEntry.arguments?.getString("subRoute")
                AudioIntelligenceNavHost(initialSubRoute = subRoute)
            }
            composable(BottomNavScreen.Me.route) {
                MeNavHost(
                    onNavigateToJourney = { subRoute -> 
                        val route = if (subRoute != null) {
                            "${BottomNavScreen.Journey.route}?subRoute=$subRoute"
                        } else {
                            BottomNavScreen.Journey.route
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = (subRoute == null)
                        }
                    },
                    onNavigateToFreestyle = {
                        navController.navigate(BottomNavScreen.Freestyle.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAudioAI = { subRoute ->
                        val route = if (subRoute != null) {
                            "${BottomNavScreen.AudioIntelligence.route}?subRoute=$subRoute"
                        } else {
                            BottomNavScreen.AudioIntelligence.route
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = (subRoute == null)
                        }
                    }
                )
            }
        }
    }
}
