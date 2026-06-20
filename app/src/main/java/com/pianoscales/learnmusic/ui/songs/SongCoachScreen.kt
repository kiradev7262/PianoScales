package com.pianoscales.learnmusic.ui.songs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.ui.freestyle.FreestylePiano
import com.pianoscales.learnmusic.ui.theme.*

@Composable
fun SongCoachScreen(
    onBack: () -> Unit,
    viewModel: SongCoachViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (uiState.isCompleted) {
        SongCompletionDialog(
            songTitle = uiState.song.title,
            onPlayAgain = { viewModel.reset() },
            onBackToSongs = onBack
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryBackground)
    ) {
        // Header
        SongCoachHeader(
            title = uiState.song.title,
            currentLine = uiState.currentLineIndex + 1,
            totalLines = uiState.song.lines.size,
            onBack = onBack
        )

        // Top Section (20% height in landscape, 30% in portrait)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isLandscape) 0.23f else 0.3f)
                .padding(if (isLandscape) 8.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            NoteDisplayArea(
                line = uiState.currentLine,
                activeNoteIndex = uiState.currentNoteIndex
            )
        }

        // Bottom Section (80% height in landscape, 70% in portrait) - Keyboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isLandscape) 0.8f else 0.7f)
                .background(CardSurface)
        ) {
            FreestylePiano(
                onNoteClick = { note, octave -> 
                    viewModel.onNotePlayed(note, octave)
                },
                height = 300.dp, // Will be constrained by weight
                blackKeyHeightRatio = if (isLandscape) 0.5f else 0.55f
            )
        }
    }
}

@Composable
fun SongCoachHeader(
    title: String,
    currentLine: Int,
    totalLines: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        Text(
            text = "Line $currentLine / $totalLines",
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryAccent,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

@Composable
fun NoteDisplayArea(
    line: SongLine,
    activeNoteIndex: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        line.notes.forEachIndexed { index, noteWithOctave ->
            val isActive = index == activeNoteIndex
            val isCompleted = index < activeNoteIndex
            
            NoteItem(
                note = "${noteWithOctave.note.displayName}${noteWithOctave.octave}",
                isActive = isActive,
                isCompleted = isCompleted
            )
            
            if (index < line.notes.size - 1) {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Composable
fun NoteItem(
    note: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isActive) {
            Text("▶", color = PrimaryAccent, fontSize = 12.sp)
        } else if (isCompleted) {
            Text("✓", color = SuccessAccent, fontSize = 12.sp)
        } else {
            // Placeholder to keep baseline alignment
            Text("", fontSize = 12.sp)
        }
        
        Text(
            text = note,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
            color = when {
                isActive -> PrimaryAccent
                isCompleted -> SuccessAccent
                else -> TextMuted
            }
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
