package com.example.pianoscales.ui.education

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    onStartLesson: (Int) -> Unit
) {
    val lessons = listOf(
        BeginnerLesson(1, "What are musical notes?", "Learn the names of the keys on the piano.", Icons.Default.PlayArrow, isCompleted = true),
        BeginnerLesson(2, "Understanding C D E F G A B", "The foundation of Western music notation.", Icons.Default.PlayArrow),
        BeginnerLesson(3, "White keys and black keys", "Distinguishing naturals, sharps, and flats.", Icons.Default.Lock, isLocked = true),
        BeginnerLesson(4, "What is a scale?", "Introduction to step patterns (Whole and Half).", Icons.Default.Lock, isLocked = true),
        BeginnerLesson(5, "Your first scale", "Playing the C Major Scale with correct fingering.", Icons.Default.Lock, isLocked = true)
    )

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            TopAppBar(
                title = { Text("Beginner Journey", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBackground)
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
                Text(
                    text = "Welcome to your musical path. Start from the basics and build your confidence.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(lessons) { lesson ->
                LessonItem(lesson = lesson, onClick = { if (!lesson.isLocked) onStartLesson(lesson.id) })
            }
        }
    }
}

@Composable
private fun LessonItem(lesson: BeginnerLesson, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (!lesson.isLocked) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (lesson.isLocked) PrimaryBackground.copy(alpha = 0.5f) else CardSurface
        ),
        border = if (lesson.isLocked) null else androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.1f))
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
