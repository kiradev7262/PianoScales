package com.example.pianoscales.ui.concepts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.theory.ConceptCategory
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.components.LessonCard
import com.example.pianoscales.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptSelectorScreen(
    rootNote: Note,
    onConceptSelected: (ConceptType) -> Unit,
    onBack: () -> Unit,
    viewModel: ConceptSelectorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(rootNote) {
        viewModel.loadProgress(rootNote)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${rootNote.displayName} Lessons") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            val groupedConcepts = ConceptType.entries.groupBy { it.category }
            
            groupedConcepts.forEach { (category, concepts) ->
                item {
                    val icon = when (category) {
                        ConceptCategory.SCALE -> Icons.Default.Menu
                        ConceptCategory.CHORD -> Icons.Default.PlayArrow
                        ConceptCategory.ARPEGGIO -> Icons.Default.Star
                    }
                    SectionHeader(title = category.name.lowercase().replaceFirstChar { it.uppercase() } + "s", icon = icon)
                }
                
                items(concepts) { concept ->
                    val isCompleted = uiState.completedConcepts.contains(concept)
                    LessonCard(
                        title = concept.displayName,
                        subtitle = if (isCompleted) "Mastered" else "Practice this concept",
                        progress = if (isCompleted) 1f else 0f,
                        isCompleted = isCompleted,
                        onClick = { onConceptSelected(concept) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
