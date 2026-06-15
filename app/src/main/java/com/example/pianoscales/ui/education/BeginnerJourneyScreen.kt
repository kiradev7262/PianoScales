package com.example.pianoscales.ui.education

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.ui.components.PianoScalesDetailTopBar
import com.example.pianoscales.ui.theme.*

data class BeginnerLesson(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isLocked: Boolean = false,
    val isCompleted: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeginnerJourneyScreen(
    onBack: () -> Unit,
    onStartLesson: (Int) -> Unit,
    viewModel: BeginnerJourneyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val allLessons = listOf(
        BeginnerLesson(1, "What Are Musical Notes?", "Learn the names of the keys on the piano.", Icons.Default.PlayArrow),
        BeginnerLesson(2, "Understanding C D E F G A B", "The foundation of Western music notation.", Icons.Default.PlayArrow),
        BeginnerLesson(3, "White Keys and Black Keys", "Distinguishing naturals, sharps, and flats.", Icons.Default.PlayArrow),
        BeginnerLesson(4, "What Is a Scale?", "Different scales create different moods and feelings.", Icons.Default.PlayArrow),
        BeginnerLesson(5, "Your First Major Scale", "Playing the C Major Scale with correct fingering.", Icons.Default.PlayArrow)
    )

    val processedLessons = allLessons.map { lesson ->
        val isCompleted = uiState.completedLessons.contains(lesson.id)
        val isLocked = lesson.id > 1 && !uiState.completedLessons.contains(lesson.id - 1)
        lesson.copy(isCompleted = isCompleted, isLocked = isLocked)
    }

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            PianoScalesDetailTopBar(
                title = "Beginner Journey",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Welcome to your musical path. Start from the basics and build your confidence.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "${(uiState.progressPercentage * 100).toInt()}% Complete",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.progressPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PrimaryAccent,
                        trackColor = CardSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            items(processedLessons) { lesson ->
                LessonItem(lesson = lesson, onClick = { if (!lesson.isLocked) onStartLesson(lesson.id) })
            }
            
            if (uiState.completedLessons.size == 5) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onStartLesson(-1) }, // Signal completion screen
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Journey Summary", color = PrimaryBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonItem(lesson: BeginnerLesson, onClick: () -> Unit) {
    val isCurrent = !lesson.isCompleted && !lesson.isLocked
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (lesson.isLocked) 0.6f else 1f)
            .let { if (!lesson.isLocked) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) PrimaryAccent.copy(alpha = 0.1f) else CardSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = if (isCurrent) PrimaryAccent else PrimaryAccent.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (lesson.isCompleted) Icons.Default.CheckCircle else if (lesson.isLocked) Icons.Default.Lock else lesson.icon,
                    contentDescription = null,
                    tint = if (lesson.isCompleted) SuccessAccent else if (lesson.isLocked) TextDisabled else PrimaryAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lesson ${lesson.id}: ${lesson.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (lesson.isLocked) TextDisabled else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = lesson.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lesson.isLocked) TextDisabled else TextSecondary
                )
            }
        }
    }
}
