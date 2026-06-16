package com.example.pianoscales.ui.me

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MeNavHost(
    onNavigateToJourney: (String?) -> Unit,
    onNavigateToFreestyle: () -> Unit,
    onNavigateToAudioAI: (String?) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "me_screen") {
        composable("me_screen") {
            MeScreen(
                onNavigateToPrivacy = { navController.navigate("privacy_policy") },
                onNavigateToGoal = { route ->
                    when {
                        route.startsWith("beginner_lesson/") -> onNavigateToJourney(route)
                        route.startsWith("practice_screen/") -> onNavigateToJourney(route)
                        route == "beginner_journey" -> onNavigateToJourney(route)
                        route == "freestyle_root" -> onNavigateToFreestyle()
                        route == "ear_training" -> onNavigateToAudioAI(route)
                        route == "voice_training" -> onNavigateToAudioAI(route)
                        route == "audio_intelligence_root" -> onNavigateToAudioAI(null)
                        route == "journey_root" -> onNavigateToJourney(null)
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
