package com.example.pianoscales.ui.me

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MeNavHost(
    onNavigateToJourney: () -> Unit,
    onNavigateToFreestyle: () -> Unit,
    onNavigateToAudioAI: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "me_screen") {
        composable("me_screen") {
            MeScreen(
                onNavigateToPrivacy = { navController.navigate("privacy_policy") },
                onNavigateToGoal = { route ->
                    when {
                        route.startsWith("lesson/") -> onNavigateToJourney() // Simplified, could deep link
                        route.startsWith("practice/") -> onNavigateToJourney() // Simplified
                        route == "freestyle_root" -> onNavigateToFreestyle()
                        route == "audio_intelligence_root" -> onNavigateToAudioAI()
                        route == "journey_root" -> onNavigateToJourney()
                        else -> {}
                    }
                }
            )
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
    }
}
