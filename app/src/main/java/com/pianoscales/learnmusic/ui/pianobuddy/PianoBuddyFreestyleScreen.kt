package com.pianoscales.learnmusic.ui.pianobuddy

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.audio.pitch.PitchToNoteMapper
import com.pianoscales.learnmusic.ui.LocalBottomBarVisibility
import com.pianoscales.learnmusic.ui.freestyle.FreestylePiano
import com.pianoscales.learnmusic.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PianoBuddyFreestyleScreen(
    onBack: () -> Unit,
    viewModel: PianoBuddyFreestyleViewModel = hiltViewModel(),
    pbViewModel: PianoBuddyViewModel = hiltViewModel()
) {
    val inputMode by viewModel.inputMode.collectAsState()
    val connectionState by pbViewModel.connectionState.collectAsState()
    val bottomBarVisibility = LocalBottomBarVisibility.current
    val isVirtualPiano = inputMode == FreestyleInputMode.VIRTUAL_PIANO
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showTopBar = !(isLandscape && isVirtualPiano)

    // Handle bottom bar visibility
    DisposableEffect(isVirtualPiano) {
        if (isVirtualPiano) {
            bottomBarVisibility.value = false
        } else {
            bottomBarVisibility.value = true
        }
        onDispose {
            bottomBarVisibility.value = true
        }
    }

    // Redirect to home if disconnected
    LaunchedEffect(connectionState) {
        if (connectionState == com.pianoscales.learnmusic.ble.BleConnectionState.DISCONNECTED) {
            onBack()
        }
    }

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Freestyle") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBackground)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showTopBar) padding else PaddingValues(0.dp))
                .padding(if (!showTopBar) 0.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showTopBar) {
                Text(
                    text = "Input Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = inputMode == FreestyleInputMode.VIRTUAL_PIANO,
                            onClick = { viewModel.setInputMode(FreestyleInputMode.VIRTUAL_PIANO) }
                        )
                        Text("Virtual Piano")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = inputMode == FreestyleInputMode.EXTERNAL_PIANO,
                            onClick = { viewModel.setInputMode(FreestyleInputMode.EXTERNAL_PIANO) }
                        )
                        Text("External Piano")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = CardSurface)
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // In immersive landscape, add a small back button overlay or similar?
                // The user said "Allow the keyboard to occupy the full available screen."
                // I'll just show the piano.
            }

            if (inputMode == FreestyleInputMode.VIRTUAL_PIANO) {
                FreestylePiano(
                    onNoteClick = { note, octave -> viewModel.onNotePlayed(note, octave) },
                    octaves = listOf(2, 3, 4, 5, 6, 7),
                    height = if (isLandscape) 300.dp else 240.dp // Make it larger in landscape
                )
                
                if (!showTopBar) {
                    // Hidden Exit button for immersive mode
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.Start).padding(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Exit Fullscreen", fontSize = 12.sp)
                    }
                }
            } else {
                ExternalPianoContent(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ExternalPianoContent(
    modifier: Modifier = Modifier,
    viewModel: PianoBuddyFreestyleViewModel = hiltViewModel(),
    voiceViewModel: com.pianoscales.learnmusic.audio_intelligence.voice_training.VoiceTrainingViewModel = hiltViewModel()
) {
    val detectedNote by voiceViewModel.detectedNote.collectAsState()
    val frequency by voiceViewModel.frequency.collectAsState()
    val isStable by voiceViewModel.isStable.collectAsState()
    val isListening by voiceViewModel.isListening.collectAsState()

    DisposableEffect(Unit) {
        voiceViewModel.startListening()
        onDispose {
            voiceViewModel.stopListening()
        }
    }

    LaunchedEffect(frequency, isStable) {
        if (isStable) {
            val noteWithOctave = PitchToNoteMapper.mapFrequencyToNoteWithOctave(frequency)
            if (noteWithOctave != null) {
                viewModel.onNotePlayed(noteWithOctave.note, noteWithOctave.octave)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isListening) "Listening..." else "Mic off",
            style = MaterialTheme.typography.bodyLarge,
            color = PrimaryAccent
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "🎤",
            style = androidx.compose.ui.text.TextStyle(fontSize = 64.sp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = detectedNote?.displayName ?: "--",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Detected Note",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
    }
}
