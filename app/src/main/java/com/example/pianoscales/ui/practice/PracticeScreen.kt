package com.example.pianoscales.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.theory.ConceptCategory
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.theory.TheoryExplanation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    rootNote: Note,
    conceptType: ConceptType,
    onBack: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.toggleListening()
        }
    }

    LaunchedEffect(rootNote, conceptType) {
        viewModel.init(rootNote, conceptType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${rootNote.displayName} ${conceptType.displayName}")
                        if (uiState.isLessonAlreadyCompleted || uiState.guidedPractice.lessonCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (conceptType.category == ConceptCategory.SCALE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("Include Octave Note", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = uiState.includeOctave,
                        onCheckedChange = { viewModel.toggleOctave() },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = "Notes to practice:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(uiState.generatedNotes.indices.toList()) { index ->
                    val note = uiState.generatedNotes[index]
                    NoteBubble(
                        note = note,
                        isPlaying = uiState.currentPlayingNote == note,
                        isCompleted = if (uiState.guidedPractice.isRunning) {
                            uiState.guidedPractice.completedNotes.contains(index)
                        } else {
                            uiState.completedNotes.contains(note)
                        },
                        isDetected = uiState.isStablePitch && uiState.detectedNote == note,
                        isTarget = uiState.guidedPractice.isRunning && uiState.guidedPractice.currentIndex == index
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PracticeProgressBar(
                current = uiState.generatedNotes.count { uiState.completedNotes.contains(it) },
                total = uiState.generatedNotes.size
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (uiState.isListening) {
                VolumeMeter(amplitude = uiState.inputVolume)
                Spacer(modifier = Modifier.height(16.dp))
                
                DetectedNoteDisplay(
                    detectedNote = uiState.detectedNote,
                    frequency = uiState.detectedFrequency,
                    isStable = uiState.isStablePitch
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            Button(
                onClick = { viewModel.playSequence() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isPlaying && !uiState.isListening
            ) {
                Text(if (uiState.isPlaying) "Playing..." else "Play Sequence")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) -> {
                            viewModel.toggleListening()
                        }
                        else -> {
                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isPlaying
            ) {
                Text(if (uiState.isListening) "Stop Listening" else "Start Listening")
            }

            if (uiState.completedNotes.isNotEmpty() && !uiState.guidedPractice.isRunning) {
                TextButton(
                    onClick = { viewModel.resetProgress() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Progress")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GuidedPracticeCard(
                state = uiState.guidedPractice,
                totalNotes = uiState.generatedNotes.size,
                onStart = { viewModel.startGuidedPractice() },
                onReset = { viewModel.resetGuidedPractice() },
                onPlayTarget = { viewModel.playTargetNote() }
            )

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            uiState.theoryExplanation?.let { theory ->
                TheoryPanel(
                    theory = theory,
                    isExpanded = uiState.isTheoryExpanded,
                    onToggleExpansion = { viewModel.toggleTheoryExpansion() }
                )
            }
        }

        if (uiState.showFirstTimeCompletion) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCompletionDialog() },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissCompletionDialog() }) {
                        Text("Awesome!")
                    }
                },
                title = { Text("🎉 Lesson Completed!") },
                text = { Text("Great job! You've mastered the ${rootNote.displayName} ${conceptType.displayName}. Your progress has been saved.") },
                icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700)) }
            )
        }
    }
}

@Composable
fun GuidedPracticeCard(
    state: GuidedPracticeState,
    totalNotes: Int,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onPlayTarget: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (state.isRunning) BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Guided Practice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (!state.isRunning) {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Guided Lesson")
                }
            } else {
                if (state.lessonCompleted) {
                    Text(
                        text = "🎉 Lesson Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalNotes / $totalNotes Notes Correct",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restart Lesson")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TARGET NOTE", style = MaterialTheme.typography.labelSmall)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = state.targetNote?.displayName ?: "--",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            IconButton(onClick = onPlayTarget) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Target", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f).padding(start = 16.dp)
                        ) {
                            Text(
                                text = "${state.currentIndex} / $totalNotes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { state.currentIndex.toFloat() / totalNotes },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            when (val result = state.lastResult) {
                                is PracticeResult.Correct -> {
                                    Text("✅ Correct!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                }
                                is PracticeResult.Incorrect -> {
                                    Text("❌ Try Again", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    Text("Expected ${result.expected.displayName}, detected ${result.detected.displayName}", style = MaterialTheme.typography.bodySmall)
                                }
                                null -> {
                                    Text("Play the note above", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                        Text("Reset Lesson")
                    }
                }
            }
        }
    }
}

@Composable
fun TheoryPanel(
    theory: TheoryExplanation,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpansion() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Theory Explanation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = theory.generalExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Formula", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = theory.formula,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    theory.formulaMeaning.forEach { (key, value) ->
                        Text(
                            text = "$key = $value",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Construction", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("Start at ${theory.title.split(" ")[0]}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        theory.constructionSteps.forEach { step ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = step.interval,
                                    modifier = Modifier.width(40.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = step.resultNote.displayName,
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scale Degrees", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        theory.scaleDegrees.forEach { degreeInfo ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(degreeInfo.degree, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = degreeInfo.note.displayName,
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = degreeInfo.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeProgressBar(current: Int, total: Int) {
    if (total == 0) return
    val progress = current.toFloat() / total.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ProgressBar"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$current / $total Notes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        if (progress >= 1f) {
            Text(
                text = "Scale Complete! 🎉",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF4CAF50),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VolumeMeter(amplitude: Float) {
    val animatedAmplitude by animateFloatAsState(
        targetValue = (amplitude * 5f).coerceIn(0f, 1f), // Scale up for better visibility
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "VolumeMeter"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Input Level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedAmplitude },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (animatedAmplitude > 0.8f) Color.Red else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun DetectedNoteDisplay(detectedNote: Note?, frequency: Float, isStable: Boolean) {
    val statusText = when {
        detectedNote == null -> "🎤 Listening..."
        !isStable -> "Detecting..."
        else -> "✅ Stable Note Detected"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            color = if (isStable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            fontWeight = if (isStable) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isStable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = detectedNote?.displayName ?: "--",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (isStable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (frequency > 0) {
            Text(
                text = String.format("%.1f Hz", frequency),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun NoteBubble(
    note: Note,
    isPlaying: Boolean,
    isCompleted: Boolean,
    isDetected: Boolean,
    isTarget: Boolean = false
) {
    val targetContainerColor = when {
        isTarget -> MaterialTheme.colorScheme.primaryContainer
        isCompleted -> Color(0xFF4CAF50) // Material Green 500
        isPlaying -> MaterialTheme.colorScheme.primary
        isDetected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val targetContentColor = when {
        isTarget -> MaterialTheme.colorScheme.onPrimaryContainer
        isCompleted || isPlaying -> Color.White
        isDetected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 300),
        label = "ContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 300),
        label = "ContentColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDetected || isPlaying || isTarget) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "Scale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .size(width = 64.dp, height = 80.dp)
            .scale(scale)
            .then(
                if (isDetected || isTarget) Modifier.border(
                    width = if (isTarget) 3.dp else 2.dp,
                    color = if (isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shadowElevation = if (isPlaying || isDetected || isTarget) 8.dp else 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isTarget && !isCompleted) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = note.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            AnimatedVisibility(visible = isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
        }
    }
}
