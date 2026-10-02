package com.pianoscales.learnmusic.ui.songs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    if (uiState.isCompleted) {
        SongCompletionDialog(
            songTitle = song.title,
            onPlayAgain = { viewModel.reset() },
            onBackToSongs = onBack
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryBackground)
    ) {
        // Header with title and compact Listen Demo action
        SongCoachHeader(
            title = song.title,
            isDemoPlaying = uiState.isDemoPlaying,
            onDemoClick = { viewModel.toggleDemo() },
            onBack = onBack,
            pianoMode = uiState.pianoMode
        )

        // Main Section: Falling Notes Visualization + Virtual Piano Keyboard
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val totalHeight = maxHeight
            val pianoHeight = totalHeight * 0.33f
            val fallingAreaHeight = totalHeight - pianoHeight

            Column(modifier = Modifier.fillMaxSize()) {
                // Falling Piano Notes (~67% height)
                SongFallingNotesView(
                    song = song,
                    currentNoteIndex = uiState.currentNoteIndex,
                    isDemoPlaying = uiState.isDemoPlaying,
                    layout = keyboardLayout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fallingAreaHeight)
                )

                // Responsive Virtual Keyboard (~33% height)
                SongVirtualKeyboard(
                    layout = keyboardLayout,
                    targetNote = uiState.currentNote,
                    enabled = (uiState.pianoMode == PianoMode.VIRTUAL) && !uiState.isDemoPlaying,
                    onNotePlayed = { note, octave ->
                        viewModel.onNotePlayed(note, octave)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pianoHeight)
                )
            }
        }
    }
}

@Composable
fun SongCoachHeader(
    title: String,
    isDemoPlaying: Boolean,
    onDemoClick: () -> Unit,
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
        DemoButton(
            isDemoPlaying = isDemoPlaying,
            onClick = onDemoClick
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

