package com.example.pianoscales.audio_intelligence.voice_training

import androidx.compose.foundation.layout.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.audio_intelligence.FeatureCard
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.components.PianoScalesDetailTopBar
import com.example.pianoscales.ui.theme.CardSurface
import com.example.pianoscales.ui.theme.PrimaryAccent
import com.example.pianoscales.ui.theme.PrimaryBackground
import com.example.pianoscales.ui.theme.TextMuted
import com.example.pianoscales.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTrainingScreen(
    onBack: () -> Unit,
    viewModel: VoiceTrainingViewModel = hiltViewModel()
) {
    var currentSubFeature by remember { mutableStateOf<VoiceTrainingSubFeature?>(null) }
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(currentSubFeature) {
        if (currentSubFeature != null && !hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            PianoScalesDetailTopBar(
                title = currentSubFeature?.title ?: "Voice Training",
                onBack = { 
                    if (currentSubFeature == null) onBack() else currentSubFeature = null 
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!hasPermission && currentSubFeature != null) {
                PermissionDeniedContent {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            } else if (currentSubFeature == null) {
                Text(
                    text = "Select Training Type",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                FeatureCard(
                    title = "Pitch Matching",
                    description = "Match the pitch of a reference note.",
                    onClick = { currentSubFeature = VoiceTrainingSubFeature.PITCH_MATCHING }
                )
                Spacer(modifier = Modifier.height(16.dp))
                FeatureCard(
                    title = "Free Pitch Detection",
                    description = "See which note you are singing in real-time.",
                    onClick = { currentSubFeature = VoiceTrainingSubFeature.FREE_PITCH_DETECTION }
                )
            } else {
                currentSubFeature?.let { feature ->
                    when (feature) {
                        VoiceTrainingSubFeature.PITCH_MATCHING -> PitchMatchingScreen(viewModel)
                        VoiceTrainingSubFeature.FREE_PITCH_DETECTION -> FreePitchDetectionScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Microphone permission is required for voice training.", color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

enum class VoiceTrainingSubFeature(val title: String) {
    PITCH_MATCHING("Pitch Matching"),
    FREE_PITCH_DETECTION("Free Pitch Detection")
}

@Composable
fun PitchMatchingScreen(viewModel: VoiceTrainingViewModel) {
    val targetNote by viewModel.targetNote.collectAsState()
    val detectedNote by viewModel.detectedNote.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isStable by viewModel.isStable.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.stopListening() }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (targetNote == null) {
            Text("Select a target note to match:", color = TextMuted)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(Note.C, Note.E, Note.G).forEach { note ->
                    Button(onClick = { viewModel.setTargetNote(note) }) {
                        Text(note.displayName)
                    }
                }
            }
        } else {
            Text("Target Note: ${targetNote?.displayName}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryAccent)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.playReferenceNote() }) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("Play Reference")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (!isListening) {
                Button(onClick = { viewModel.startListening() }) {
                    Text("Start Listening")
                }
            } else {
                Button(onClick = { viewModel.stopListening() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Stop Listening")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                NoteDisplay(detectedNote, isStable)
                
                if (detectedNote != null && detectedNote == targetNote && isStable) {
                    Text("Perfect Match!", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                } else if (detectedNote != null) {
                    val targetIndex = Note.entries.indexOf(targetNote!!)
                    val detectedIndex = Note.entries.indexOf(detectedNote!!)
                    val diff = detectedIndex - targetIndex
                    
                    val hint = when {
                        diff > 0 -> "Too High"
                        diff < 0 -> "Too Low"
                        else -> "Keep holding..."
                    }
                    Text(hint, color = if (diff == 0) Color.Yellow else Color.Red)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { viewModel.setTargetNote(Note.entries.random()) }) {
                Text("Change Target")
            }
        }
    }
}

@Composable
fun FreePitchDetectionScreen(viewModel: VoiceTrainingViewModel) {
    val detectedNote by viewModel.detectedNote.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isStable by viewModel.isStable.collectAsState()
    val frequency by viewModel.frequency.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.stopListening() }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isListening) {
            Button(onClick = { viewModel.startListening() }) {
                Text("Start Real-time Detection")
            }
        } else {
            NoteDisplay(detectedNote, isStable)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = String.format(Locale.getDefault(), "%.1f Hz", frequency), color = TextMuted)
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { viewModel.stopListening() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Stop")
            }
        }
    }
}

@Composable
fun NoteDisplay(note: Note?, isStable: Boolean) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                color = if (isStable) PrimaryAccent else CardSurface,
                shape = androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = note?.displayName ?: "-",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = if (isStable) Color.Black else TextPrimary
        )
    }
}
