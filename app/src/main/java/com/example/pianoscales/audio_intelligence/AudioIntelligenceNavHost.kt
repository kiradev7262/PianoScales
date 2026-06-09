package com.example.pianoscales.audio_intelligence

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pianoscales.audio_intelligence.ear_training.EarTrainingScreen
import com.example.pianoscales.audio_intelligence.voice_training.VoiceTrainingScreen

sealed class AudioIntelligenceRoute(val route: String) {
    object Main : AudioIntelligenceRoute("ai_main")
    object EarTraining : AudioIntelligenceRoute("ear_training")
    object VoiceTraining : AudioIntelligenceRoute("voice_training")
}

@Composable
fun AudioIntelligenceNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AudioIntelligenceRoute.Main.route
    ) {
        composable(AudioIntelligenceRoute.Main.route) {
            AudioIntelligenceScreen(
                onNavigateToEarTraining = {
                    navController.navigate(AudioIntelligenceRoute.EarTraining.route)
                },
                onNavigateToVoiceTraining = {
                    navController.navigate(AudioIntelligenceRoute.VoiceTraining.route)
                }
            )
        }
        composable(AudioIntelligenceRoute.EarTraining.route) {
            EarTrainingScreen(onBack = { navController.popBackStack() })
        }
        composable(AudioIntelligenceRoute.VoiceTraining.route) {
            VoiceTrainingScreen(onBack = { navController.popBackStack() })
        }
    }
}
