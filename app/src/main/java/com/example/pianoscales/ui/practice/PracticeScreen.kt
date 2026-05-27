package com.example.pianoscales.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                DetectedNoteDisplay(
                    detectedNote = uiState.detectedNote,
                    frequency = uiState.detectedFrequency
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
fun DetectedNoteDisplay(detectedNote: Note?, frequency: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Detected Note:",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = detectedNote?.displayName ?: "--",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = String.format("%.1f Hz", frequency),
            style = MaterialTheme.typography.bodyMedium
        )
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
