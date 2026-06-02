package com.example.pianoscales.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.domain.progress.NoteProgress
import com.example.pianoscales.domain.progress.OverallProgress
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.components.LessonCard
import com.example.pianoscales.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSelectorScreen(
    viewModel: NoteSelectorViewModel = hiltViewModel(),
    onNoteSelected: (Note) -> Unit,
    onContinueLesson: (Note, ConceptType) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Piano Journey") })
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(3) }) {
                OverallProgressDashboard(uiState.overallProgress)
            }

            uiState.latestProgress?.let { latest ->
                item(span = { GridItemSpan(3) }) {
                    Column {
                        SectionHeader(title = "Continue Learning", icon = Icons.Default.PlayArrow)
                        LessonCard(
                            title = "${latest.rootNote.displayName} ${latest.conceptType.displayName}",
                            subtitle = if (latest.completed) "Mastered - Review?" else "Resume your progress",
                            progress = if (latest.completed) 1f else 0.5f,
                            isCompleted = latest.completed,
                            onClick = { onContinueLesson(latest.rootNote, latest.conceptType) },
                            trailingIcon = Icons.Default.PlayArrow
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            item(span = { GridItemSpan(3) }) {
                SectionHeader(title = "Explore Root Notes", icon = Icons.Default.Menu)
            }

            items(Note.entries) { note ->
                val progress = uiState.noteProgress[note] ?: NoteProgress(0, 6, 0f)
                NoteDashboardCard(
                    note = note,
                    progress = progress,
                    onClick = { onNoteSelected(note) }
                )
            }
        }
    }
}

@Composable
fun NoteDashboardCard(
    note: Note,
    progress: NoteProgress,
    onClick: () -> Unit
) {
    val isFullyCompleted = progress.completedLessons == progress.totalLessons && progress.totalLessons > 0
    
    ElevatedCard(
        modifier = Modifier
            .aspectRatio(0.9f)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isFullyCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎹 ${note.displayName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${progress.completedLessons}/${progress.totalLessons} Complete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isFullyCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun OverallProgressDashboard(progress: OverallProgress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Global Progress",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${progress.completedLessons} / ${progress.totalLessons} Lessons",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${(progress.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    progress: NoteProgress,
    onClick: () -> Unit
) {
    val isFullyCompleted = progress.completedLessons == progress.totalLessons && progress.totalLessons > 0
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(0.85f)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFullyCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        ),
        border = if (isFullyCompleted) BorderStroke(2.dp, Color(0xFF4CAF50)) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isFullyCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp),
                    tint = Color(0xFF4CAF50)
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = note.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${progress.completedLessons}/${progress.totalLessons}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.percentage },
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = if (isFullyCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
