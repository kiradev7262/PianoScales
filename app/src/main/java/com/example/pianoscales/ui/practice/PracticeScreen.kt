package com.example.pianoscales.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
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
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note

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
                title = { Text("${rootNote.displayName} ${conceptType.displayName}") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                items(uiState.generatedNotes) { note ->
                    NoteBubble(
                        note = note,
                        isPlaying = uiState.currentPlayingNote == note,
                        isCompleted = uiState.completedNotes.contains(note),
                        isDetected = uiState.isStablePitch && uiState.detectedNote == note
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PracticeProgressBar(
                current = uiState.completedNotes.size,
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

            if (uiState.completedNotes.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.resetProgress() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Progress")
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
    isDetected: Boolean
) {
    val targetContainerColor = when {
        isCompleted -> Color(0xFF4CAF50) // Material Green 500
        isPlaying -> MaterialTheme.colorScheme.primary
        isDetected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val targetContentColor = when {
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
        targetValue = if (isDetected || isPlaying) 1.15f else 1f,
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
                if (isDetected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shadowElevation = if (isPlaying || isDetected) 8.dp else 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
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
