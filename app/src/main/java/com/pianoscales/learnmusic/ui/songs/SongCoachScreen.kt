package com.pianoscales.learnmusic.ui.songs

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.ui.theme.*

@Composable
fun SongCoachScreen(
    onBack: () -> Unit,
    viewModel: SongCoachViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val song = uiState.song ?: return // Loading or not found

    val startingNote = remember(song) {
        resolveStartingNote(song)
    }

    val keyboardLayout = remember(startingNote) {
        SongKeyboardLayout.create(startingNote, numWhiteKeys = 15)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isPianoExpanded by rememberSaveable { mutableStateOf(false) }

    val pianoRatio = if (isLandscape && isPianoExpanded) 0.50f else 0.33f

    if (uiState.isCompleted) {
        if (uiState.isRecordingTiming) {
            TimingRecordedDialog(
                songTitle = song.title,
                onSaveTiming = { viewModel.saveRecordedTiming() },
                onDiscardTiming = { viewModel.cancelRecordTiming() }
            )
        } else {
            SongCompletionDialog(
                songTitle = song.title,
                onPlayAgain = { viewModel.reset() },
                onBackToSongs = onBack
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryBackground)
    ) {
        // Header with title, Listen Demo, and Record Timing actions
        SongCoachHeader(
            title = song.title,
            isDemoPlaying = uiState.isDemoPlaying,
            isRecordingTiming = uiState.isRecordingTiming,
            onDemoClick = { viewModel.toggleDemo() },
            onStartRecordTiming = { viewModel.startRecordTiming() },
            onBack = onBack,
            pianoMode = uiState.pianoMode
        )

        // Banner when Record Timing is active
        if (uiState.isRecordingTiming) {
            Surface(
                color = CardSurface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(ErrorAccent, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recording timing (${uiState.recordedTimestampsCount}/${song.notes.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Row {
                        TextButton(
                            onClick = { viewModel.cancelRecordTiming() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Cancel", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = { viewModel.saveRecordedTiming() },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", color = PrimaryBackground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Main Section: Falling Notes Visualization + Virtual Piano Keyboard
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val totalHeight = maxHeight
            val pianoHeight = totalHeight * pianoRatio
            val fallingAreaHeight = totalHeight - pianoHeight

            Column(modifier = Modifier.fillMaxSize()) {
                // Falling Piano Notes Area
                SongFallingNotesView(
                    song = song,
                    currentNoteIndex = uiState.currentNoteIndex,
                    isDemoPlaying = uiState.isDemoPlaying,
                    layout = keyboardLayout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fallingAreaHeight)
                )

                // Virtual Keyboard Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pianoHeight)
                ) {
                    SongVirtualKeyboard(
                        layout = keyboardLayout,
                        targetNote = uiState.currentNote,
                        enabled = (uiState.pianoMode == PianoMode.VIRTUAL) && !uiState.isDemoPlaying,
                        onNotePlayed = { note, octave ->
                            viewModel.onNotePlayed(note, octave)
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Expand / Collapse Arrow Icon (Only in Landscape Orientation)
                    if (isLandscape) {
                        IconButton(
                            onClick = { isPianoExpanded = !isPianoExpanded },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 8.dp, top = 4.dp)
                                .size(28.dp)
                                .background(CardSurface.copy(alpha = 0.85f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPianoExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isPianoExpanded) "Collapse Keyboard" else "Expand Keyboard",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongCoachHeader(
    title: String,
    isDemoPlaying: Boolean,
    isRecordingTiming: Boolean,
    onDemoClick: () -> Unit,
    onStartRecordTiming: () -> Unit,
    onBack: () -> Unit,
    pianoMode: PianoMode = PianoMode.VIRTUAL
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (pianoMode == PianoMode.EXTERNAL) {
                Text(
                    text = "🎤 Listening for notes...",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessAccent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!isRecordingTiming) {
            RecordTimingButton(onClick = onStartRecordTiming)
            Spacer(modifier = Modifier.width(6.dp))
            DemoButton(
                isDemoPlaying = isDemoPlaying,
                onClick = onDemoClick
            )
        }
    }
}

@Composable
fun RecordTimingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TextPrimary
        ),
        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.5f)),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = modifier.heightIn(min = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(ErrorAccent, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Record Timing",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DemoButton(
    isDemoPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDemoPlaying) ErrorAccent else PrimaryAccent,
            contentColor = TextPrimary
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier.heightIn(min = 32.dp)
    ) {
        Icon(
            imageVector = if (isDemoPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isDemoPlaying) "Stop Demo" else "Listen Demo",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SongCompletionDialog(
    songTitle: String,
    onPlayAgain: () -> Unit,
    onBackToSongs: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("🎉 You Played $songTitle!") },
        text = { Text("Amazing work. You've successfully completed the song note-by-note.") },
        confirmButton = {
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Play Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onBackToSongs) {
                Text("Back to Songs", color = TextMuted)
            }
        },
        containerColor = CardSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

@Composable
fun TimingRecordedDialog(
    songTitle: String,
    onSaveTiming: () -> Unit,
    onDiscardTiming: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("🎉 Timing Recording Complete!") },
        text = { Text("New timing for \"$songTitle\" has been captured. Save this new timing?") },
        confirmButton = {
            Button(
                onClick = onSaveTiming,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent)
            ) {
                Text("Save Timing", color = PrimaryBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscardTiming) {
                Text("Discard", color = TextMuted)
            }
        },
        containerColor = CardSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}
