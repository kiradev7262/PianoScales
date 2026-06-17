package com.pianoscales.learnmusic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pianoscales.learnmusic.theory.ConceptType
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.ui.concepts.ConceptSelectorScreen
import com.pianoscales.learnmusic.ui.education.BeginnerCompletionScreen
import com.pianoscales.learnmusic.ui.education.BeginnerJourneyScreen
import com.pianoscales.learnmusic.ui.education.LessonContentScreen
import com.pianoscales.learnmusic.ui.notes.NoteSelectorScreen
import com.pianoscales.learnmusic.ui.practice.PracticeScreen

sealed class Screen(val route: String) {
    object NoteSelector : Screen("notes_screen")
    object BeginnerJourney : Screen("beginner_journey")
    object BeginnerLesson : Screen("beginner_lesson/{lessonId}") {
        fun createRoute(lessonId: Int) = "beginner_lesson/$lessonId"
    }
    object BeginnerCompletion : Screen("beginner_completion")
    object ConceptSelector : Screen("concept_screen/{note}") {
        fun createRoute(note: Note) = "concept_screen/${note.name}"
    }
    object Practice : Screen("practice_screen/{note}/{concept}") {
        fun createRoute(note: Note, concept: ConceptType) = "practice_screen/${note.name}/${concept.name}"
    }
}

@Composable
fun JourneyNavHost(initialSubRoute: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(initialSubRoute) {
        if (initialSubRoute != null) {
            navController.navigate(initialSubRoute) {
                // To ensure deep link doesn't result in nested stacks of same destination
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.NoteSelector.route
    ) {
        composable(Screen.NoteSelector.route) {
            NoteSelectorScreen(
                onNoteSelected = { note ->
                    navController.navigate(Screen.ConceptSelector.createRoute(note))
                },
                onContinueLesson = { note, concept ->
                    navController.navigate(Screen.Practice.createRoute(note, concept))
                },
                onStartBeginnerJourney = {
                    navController.navigate(Screen.BeginnerJourney.route)
                }
            )
        }
        composable(Screen.BeginnerJourney.route) {
            BeginnerJourneyScreen(
                onBack = { navController.popBackStack() },
                onStartLesson = { lessonId ->
                    if (lessonId == -1) {
                        navController.navigate(Screen.BeginnerCompletion.route)
                    } else {
                        navController.navigate(Screen.BeginnerLesson.createRoute(lessonId))
                    }
                }
            )
        }
        composable(
            route = Screen.BeginnerLesson.route,
            arguments = listOf(navArgument("lessonId") { type = NavType.IntType })
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getInt("lessonId") ?: 1
            LessonContentScreen(
                lessonId = lessonId,
                onBack = { navController.popBackStack() },
                onLessonComplete = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.BeginnerCompletion.route) {
            BeginnerCompletionScreen(
                onContinueToJourney = {
                    navController.navigate(Screen.NoteSelector.route) {
                        popUpTo(Screen.NoteSelector.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.ConceptSelector.route,
            arguments = listOf(navArgument("note") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteName = backStackEntry.arguments?.getString("note")
            val note = noteName?.let { Note.valueOf(it) } ?: Note.C
            ConceptSelectorScreen(
                rootNote = note,
                onConceptSelected = { concept ->
                    navController.navigate(Screen.Practice.createRoute(note, concept))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Practice.route,
            arguments = listOf(
                navArgument("note") { type = NavType.StringType },
                navArgument("concept") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val noteName = backStackEntry.arguments?.getString("note")
            val conceptName = backStackEntry.arguments?.getString("concept")
            val note = noteName?.let { Note.valueOf(it) } ?: Note.C
            val concept = conceptName?.let { ConceptType.valueOf(it) } ?: ConceptType.MAJOR_SCALE
            
            PracticeScreen(
                rootNote = note,
                conceptType = concept,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
