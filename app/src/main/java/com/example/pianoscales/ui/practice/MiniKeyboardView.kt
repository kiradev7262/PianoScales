package com.example.pianoscales.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pianoscales.theory.Note

@Composable
fun MiniKeyboardView(
    targetNote: Note? = null,
    detectedNote: Note? = null,
    playingNote: Note? = null,
    modifier: Modifier = Modifier
) {
    val allNotes = Note.entries
    
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val totalWhiteKeys = allNotes.count { !it.name.contains("SHARP") }
        val whiteKeyWidth = maxWidth / totalWhiteKeys

        Row(modifier = Modifier.fillMaxSize()) {
            allNotes.filter { !it.name.contains("SHARP") }.forEach { note ->
                WhiteKey(
                    note = note,
                    isTarget = note == targetNote,
                    isDetected = note == detectedNote,
                    isPlaying = note == playingNote,
                    modifier = Modifier.width(whiteKeyWidth).fillMaxHeight()
                )
            }
        }

        // Black keys
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = whiteKeyWidth * 0.65f)) {
            allNotes.forEachIndexed { index, note ->
                if (note.name.contains("SHARP")) {
                    BlackKey(
                        note = note,
                        isTarget = note == targetNote,
                        isDetected = note == detectedNote,
                        isPlaying = note == playingNote,
                        modifier = Modifier
                            .width(whiteKeyWidth * 0.7f)
                            .fillMaxHeight(0.6f)
                    )
                }
                
                // Adjust spacing for black keys
                if (!note.name.contains("SHARP") && note != Note.E && note != Note.B) {
                    Spacer(modifier = Modifier.width(whiteKeyWidth * 0.3f))
                } else if (!note.name.contains("SHARP")) {
                    Spacer(modifier = Modifier.width(whiteKeyWidth))
                }
            }
        }
    }
}

@Composable
private fun WhiteKey(
    note: Note,
    isTarget: Boolean,
    isDetected: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isPlaying -> MaterialTheme.colorScheme.primary
        isDetected -> Color(0xFF4CAF50)
        isTarget -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.White
    }

    val textColor = when {
        isPlaying || isDetected -> Color.White
        else -> Color.Black
    }

    Box(
        modifier = modifier
            .border(0.5.dp, Color.LightGray)
            .background(backgroundColor)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = note.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BlackKey(
    note: Note,
    isTarget: Boolean,
    isDetected: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isPlaying -> MaterialTheme.colorScheme.primary
        isDetected -> Color(0xFF4CAF50)
        isTarget -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Black
    }

    val textColor = when {
        isPlaying || isDetected -> Color.White
        else -> Color.White
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(backgroundColor)
            .padding(bottom = 4.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = note.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
