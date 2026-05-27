package com.example.pianoscales.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.generatedNotes.forEach { note ->
                    NoteBubble(
                        note = note,
                        isActive = uiState.currentPlayingNote == note
                    )
                }
            }
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
fun NoteBubble(note: Note, isActive: Boolean) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.size(48.dp),
        shadowElevation = if (isActive) 8.dp else 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = note.displayName,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
