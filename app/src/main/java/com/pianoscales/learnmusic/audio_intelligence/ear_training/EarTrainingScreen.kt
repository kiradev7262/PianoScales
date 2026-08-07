package com.pianoscales.learnmusic.audio_intelligence.ear_training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.audio_intelligence.FeatureCard
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.ui.components.PianoScalesDetailTopBar
import com.pianoscales.learnmusic.ui.theme.CardSurface
import com.pianoscales.learnmusic.ui.theme.PrimaryAccent
import com.pianoscales.learnmusic.ui.theme.PrimaryBackground
import com.pianoscales.learnmusic.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarTrainingScreen(
    onBack: () -> Unit,
    viewModel: EarTrainingViewModel = hiltViewModel()
) {
    var currentSubFeature by remember { mutableStateOf<EarTrainingSubFeature?>(null) }

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            PianoScalesDetailTopBar(
                title = currentSubFeature?.title ?: "Ear Training",
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
            val subFeature = currentSubFeature
            if (subFeature == null) {
                Text(
                    text = "Select Training Type",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                FeatureCard(
                    title = "Note Recognition",
                    description = "Identify a single note played by the app.",
                    onClick = { currentSubFeature = EarTrainingSubFeature.NOTE_RECOGNITION }
                )
                Spacer(modifier = Modifier.height(16.dp))
                FeatureCard(
                    title = "Interval Training",
                    description = "Identify the interval between two notes.",
                    onClick = { currentSubFeature = EarTrainingSubFeature.INTERVAL_TRAINING }
                )
                Spacer(modifier = Modifier.height(16.dp))
                FeatureCard(
                    title = "Reference Mode",
                    description = "Identify a note relative to a reference note.",
                    onClick = { currentSubFeature = EarTrainingSubFeature.REFERENCE_MODE }
                )
            } else {
                when (subFeature) {
                    EarTrainingSubFeature.NOTE_RECOGNITION -> NoteRecognitionScreen(viewModel)
                    EarTrainingSubFeature.INTERVAL_TRAINING -> IntervalTrainingScreen(viewModel)
                    EarTrainingSubFeature.REFERENCE_MODE -> ReferenceModeScreen(viewModel)
                }
            }
        }
    }
}

enum class EarTrainingSubFeature(val title: String) {
    NOTE_RECOGNITION("Note Recognition"),
    INTERVAL_TRAINING("Interval Training"),
    REFERENCE_MODE("Reference Mode")
}

@Composable
fun NoteGrid(
    selectedNote: Note?,
    targetNote: Note?,
    isCorrect: Boolean?,
    onNoteSelected: (Note) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(Note.entries) { note ->
            val isSelected = selectedNote == note
            val isTarget = targetNote == note

            val color = when {
                isSelected && isCorrect == true -> Color.Green
                isSelected && isCorrect == false -> Color.Red
                selectedNote != null && isTarget -> Color.Green.copy(alpha = 0.5f)
                else -> CardSurface
            }

            Button(
                onClick = { onNoteSelected(note) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = if (color == CardSurface) TextPrimary else Color.White
                ),
                modifier = Modifier.height(60.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                Text(note.displayName)
            }
        }
    }
}

@Composable
fun NoteRecognitionScreen(viewModel: EarTrainingViewModel) {
    val targetNote by viewModel.targetNote.collectAsState()
    val selectedNote by viewModel.selectedNote.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (targetNote == null) {
            Button(
                onClick = { viewModel.playNewNote() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Start Session")
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = { viewModel.repeatNote() }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Repeat", tint = PrimaryAccent, modifier = Modifier.size(48.dp))
                }
                
                if (selectedNote != null) {
                    Button(onClick = { viewModel.playNewNote() }) {
                        Text("Next Note")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            NoteGrid(
                selectedNote = selectedNote,
                targetNote = targetNote,
                isCorrect = isCorrect,
                onNoteSelected = { viewModel.onNoteSelected(it) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isCorrect == true) {
                Text("Correct!", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            } else if (isCorrect == false) {
                Text("Incorrect. It was ${targetNote?.displayName}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun IntervalTrainingScreen(viewModel: EarTrainingViewModel) {
    val intervalTarget by viewModel.intervalTarget.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()

    val intervals = listOf("Minor 2nd", "Major 2nd", "Minor 3rd", "Major 3rd", "Perfect 5th")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (intervalTarget == null) {
            Button(
                onClick = { viewModel.playNewInterval() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Start Session")
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = { viewModel.playNewInterval() }) { // For simplicity, new interval is same button
                     Icon(Icons.Default.Refresh, contentDescription = "New")
                }
                
                Button(onClick = { viewModel.playNewInterval() }) {
                    Text("Next Interval")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                intervals.forEach { interval ->
                    Button(
                        onClick = { viewModel.checkInterval(interval) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardSurface,
                            contentColor = TextPrimary
                        )
                    ) {
                        Text(interval)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isCorrect == true) {
                Text("Correct!", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            } else if (isCorrect == false) {
                Text("Try Again!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun ReferenceModeScreen(viewModel: EarTrainingViewModel) {
    val targetNote by viewModel.targetNote.collectAsState()
    val referenceNote by viewModel.referenceNote.collectAsState()
    val selectedNote by viewModel.selectedNote.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (targetNote == null) {
            Button(
                onClick = { viewModel.startReferenceMode() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Start Session")
            }
        } else {
            // Reference Note Selector
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Reference Note: ${referenceNote.displayName}")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Note.entries.forEach { note ->
                        DropdownMenuItem(
                            text = { Text(note.displayName) },
                            onClick = {
                                viewModel.setReferenceNote(note)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { viewModel.playReference() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Reference", tint = PrimaryAccent)
                    }
                    Text("Reference", fontSize = 12.sp, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { viewModel.playTarget() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Target", tint = PrimaryAccent)
                    }
                    Text("Target", fontSize = 12.sp, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { viewModel.playBoth() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Play Both", tint = PrimaryAccent)
                    }
                    Text("Both", fontSize = 12.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedNote != null) {
                Button(
                    onClick = { viewModel.playNewReferenceExercise() },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Next Exercise")
                }
            }

            NoteGrid(
                selectedNote = selectedNote,
                targetNote = targetNote,
                isCorrect = isCorrect,
                onNoteSelected = { viewModel.onNoteSelected(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isCorrect == true) {
                Text("Correct!", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            } else if (isCorrect == false) {
                Text("Incorrect. It was ${targetNote?.displayName}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}
