package com.example.pianoscales.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pianoscales.audio_intelligence.AudioIntelligenceNavHost
import com.example.pianoscales.navigation.JourneyNavHost
import com.example.pianoscales.ui.freestyle.FreestyleScreen
import com.example.pianoscales.ui.theme.CardSurface
import com.example.pianoscales.ui.theme.PrimaryAccent
import com.example.pianoscales.ui.theme.TextMuted
import com.example.pianoscales.ui.theme.TextPrimary

sealed class BottomNavScreen(val route: String, val label: String, val icon: ImageVector) {
    object Journey : BottomNavScreen("journey_root", "Journey", Icons.Default.Home)
    object Freestyle : BottomNavScreen("freestyle_root", "Freestyle", Icons.Default.PlayArrow)
    object AudioIntelligence : BottomNavScreen("audio_intelligence_root", "Audio AI", Icons.Default.Star)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavScreen.Journey,
        BottomNavScreen.Freestyle,
        BottomNavScreen.AudioIntelligence
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardSurface,
                contentColor = PrimaryAccent
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Journey.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavScreen.Journey.route) {
                JourneyNavHost()
            }
            composable(BottomNavScreen.Freestyle.route) {
                FreestyleScreen()
            }
            composable(BottomNavScreen.AudioIntelligence.route) {
                AudioIntelligenceNavHost()
            }
        }
    }
}
