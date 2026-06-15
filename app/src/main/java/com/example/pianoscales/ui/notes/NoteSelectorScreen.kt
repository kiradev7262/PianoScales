package com.example.pianoscales.ui.notes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.domain.progress.NoteProgress
import com.example.pianoscales.domain.progress.OverallProgress
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.components.LessonCard
import com.example.pianoscales.ui.components.PianoScalesHomeTopBar
import com.example.pianoscales.ui.components.SectionHeader
import com.example.pianoscales.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSelectorScreen(
    viewModel: NoteSelectorViewModel = hiltViewModel(),
    onNoteSelected: (Note) -> Unit,
    onContinueLesson: (Note, ConceptType) -> Unit = { _, _ -> },
    onStartBeginnerJourney: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val file = File(context.filesDir, "profile_image.jpg")
            FileOutputStream(file).use { out ->
                it.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            viewModel.updateProfileImage(file.absolutePath)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        }
    }

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            PianoScalesHomeTopBar(
                profileImagePath = uiState.profileImagePath,
                onAvatarClick = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) -> {
                            cameraLauncher.launch()
                        }
                        else -> {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Header
            item(span = { GridItemSpan(2) }) {
                DashboardHeader(name = uiState.displayName)
            }

            // Beginner Journey Banner/Card
            item(span = { GridItemSpan(2) }) {
                if (uiState.isBeginnerJourneyComplete) {
                    BeginnerCompleteBanner(
                        onRelearnBasics = onStartBeginnerJourney
                    )
                } else {
                    BeginnerOnboardingCard(
                        onStartJourney = onStartBeginnerJourney
                    )
                }
            }

            // Global Progress Card
            item(span = { GridItemSpan(2) }) {
                val masteredNotes = uiState.noteProgress.values.count { it.completedLessons == it.totalLessons && it.totalLessons > 0 }
                GlobalProgressCard(
                    progress = uiState.overallProgress,
                    masteredNotes = masteredNotes
                )
            }

            // Continue Learning (if exists)
            uiState.latestProgress?.let { latest ->
                item(span = { GridItemSpan(2) }) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = "Continue Learning", 
                            icon = Icons.Default.PlayArrow
                        )
                        LessonCard(
                            title = "${latest.rootNote.displayName} ${latest.conceptType.displayName}",
                            subtitle = if (latest.completed) "Mastered - Review?" else "Resume your progress",
                            progress = if (latest.completed) 1f else 0.5f,
                            isCompleted = latest.completed,
                            onClick = { onContinueLesson(latest.rootNote, latest.conceptType) },
                            trailingIcon = Icons.Default.PlayArrow
                        )
                    }
                }
            }

            // Explore Root Notes Title
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null, 
                            tint = PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Musical Roots",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    // not needed currently
//                    TextButton(onClick = {}) {
//                        Text("VIEW ALL", style = MaterialTheme.typography.labelMedium, color = TextMuted)
//                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
//                    }
                }
            }

            // Root Note Grid
            items(Note.entries) { note ->
                val progress = uiState.noteProgress[note] ?: NoteProgress(0, 6, 0f)
                RootNoteCard(
                    note = note,
                    progress = progress,
                    onClick = { onNoteSelected(note) }
                )
            }
            
            // Extra bottom padding
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BeginnerCompleteBanner(onRelearnBasics: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = SuccessAccent.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessAccent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎓 Beginner Journey Complete",
                    style = MaterialTheme.typography.titleLarge,
                    color = SuccessAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You've built the foundations of music learning. Explore the dashboard to continue practicing. Need a refresher?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRelearnBasics,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Relearn Basics", color = PrimaryBackground, fontWeight = FontWeight.Bold)
                }
            }
            
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessAccent.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
private fun BeginnerOnboardingCard(onStartJourney: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryAccent.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "👋 New to Music?",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Learn notes, scales, and basic music theory in a beginner-friendly order.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStartJourney,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("Start Beginner Journey", color = PrimaryBackground, fontWeight = FontWeight.Bold)
                }
            }
            
            // Decorative element
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = PrimaryAccent.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
private fun DashboardHeader(name: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "Learning Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Welcome back, $name. Ready for your daily practice?",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun GlobalProgressCard(progress: OverallProgress, masteredNotes: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star, 
                    contentDescription = null, 
                    tint = PrimaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Global Progress",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "JOURNEY MASTERY",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Text(
                    text = "${(progress.percentage * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = PrimaryAccent,
                trackColor = PrimaryBackground
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Secondary info boxes matching design reference
                InfoBox(
                    modifier = Modifier.weight(1f),
                    label = "NOTES MASTERED",
                    value = "$masteredNotes",
                    valueColor = SuccessAccent
                )
                InfoBox(
                    modifier = Modifier.weight(1f),
                    label = "LESSONS DONE",
                    value = "${progress.completedLessons}",
                    valueColor = PrimaryAccent
                )
            }
        }
    }
}

@Composable
private fun InfoBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PrimaryBackground.copy(alpha = 0.5f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun RootNoteCard(
    note: Note,
    progress: NoteProgress,
    onClick: () -> Unit
) {
    val isCompleted = progress.completedLessons == progress.totalLessons && progress.totalLessons > 0
    val isInProgress = progress.completedLessons > 0 && !isCompleted
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .scale(scale)
            .clickable { 
                isPressed = true
                onClick() 
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessAccent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) SuccessAccent else if (isInProgress) PrimaryAccent else TextPrimary
                )
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${progress.completedLessons}/${progress.totalLessons} Complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "${(progress.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.percentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (isCompleted) SuccessAccent else PrimaryAccent,
                        trackColor = PrimaryBackground
                    )
                }
            }
        }
    }
    
    // Reset press state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}
