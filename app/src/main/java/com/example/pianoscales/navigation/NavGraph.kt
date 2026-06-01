package com.example.pianoscales.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.concepts.ConceptSelectorScreen
import com.example.pianoscales.ui.notes.NoteSelectorScreen
import com.example.pianoscales.ui.practice.PracticeScreen

sealed class Screen(val route: String) {
    object NoteSelector : Screen("notes_screen")
    object ConceptSelector : Screen("concept_screen/{note}") {
        fun createRoute(note: Note) = "concept_screen/${note.name}"
    }
    object Practice : Screen("practice_screen/{note}/{concept}") {
        fun createRoute(note: Note, concept: ConceptType) = "practice_screen/${note.name}/${concept.name}"
    }
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.NoteSelector.route
    ) {
        composable(Screen.NoteSelector.route) {
            NoteSelectorScreen { note ->
                navController.navigate(Screen.ConceptSelector.createRoute(note))
            }
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
