package com.example.pianoscales.ui.practice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.theme.*

@Composable
fun VirtualKeyboard(
    targetNote: Note? = null,
    detectedNote: Note? = null,
    playingNote: Note? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Virtual Keyboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val whiteNotes = listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B)
                val whiteKeyWidth = maxWidth / 7
                
                // White Keys
                Row(modifier = Modifier.fillMaxSize()) {
                    whiteNotes.forEach { note ->
                        WhiteKey(
                            note = note,
                            isTarget = note == targetNote,
                            isDetected = note == detectedNote,
                            isPlaying = note == playingNote,
                            modifier = Modifier
                                .width(whiteKeyWidth)
                                .fillMaxHeight()
                        )
                    }
                }
                
                // Black Keys Positioning
                val blackKeyWidth = whiteKeyWidth * 0.65f
                val blackKeyHeight = 110.dp
                
                // C# (between C and D) - offset by 1 white key width - half of black key width
                BlackKeyAt(Note.C_SHARP, targetNote, detectedNote, playingNote, whiteKeyWidth * 1f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // D# (between D and E)
                BlackKeyAt(Note.D_SHARP, targetNote, detectedNote, playingNote, whiteKeyWidth * 2f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // F# (between F and G)
                BlackKeyAt(Note.F_SHARP, targetNote, detectedNote, playingNote, whiteKeyWidth * 4f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // G# (between G and A)
                BlackKeyAt(Note.G_SHARP, targetNote, detectedNote, playingNote, whiteKeyWidth * 5f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // A# (between A and B)
                BlackKeyAt(Note.A_SHARP, targetNote, detectedNote, playingNote, whiteKeyWidth * 6f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
            }
        }
    }
}

@Composable
private fun BoxScope.BlackKeyAt(
    note: Note,
    targetNote: Note?,
    detectedNote: Note?,
    playingNote: Note?,
    offset: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    BlackKey(
        note = note,
        isTarget = note == targetNote,
        isDetected = note == detectedNote,
        isPlaying = note == playingNote,
        modifier = Modifier
            .offset(x = offset)
            .width(width)
            .height(height)
    )
}

@Composable
private fun WhiteKey(
    note: Note,
    isTarget: Boolean,
    isDetected: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val isHighlighted = isTarget || isDetected || isPlaying
    
    val highlightColor = when {
        isPlaying -> PrimaryAccent.copy(alpha = 0.3f)
        isDetected -> SuccessAccent.copy(alpha = 0.3f)
        isTarget -> PrimaryAccent.copy(alpha = 0.2f)
        else -> Color.White
    }

    val accentColor = when {
        isPlaying || isTarget -> PrimaryAccent
        isDetected -> SuccessAccent
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(highlightColor)
            .then(
                if (isHighlighted) {
                    Modifier.border(
                        2.dp, 
                        accentColor, 
                        RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                } else Modifier.border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isHighlighted) {
            Text(
                text = note.displayName.lowercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
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
    val isHighlighted = isTarget || isDetected || isPlaying
    
    val highlightColor = when {
        isPlaying -> PrimaryAccent.copy(alpha = 0.8f)
        isDetected -> SuccessAccent.copy(alpha = 0.8f)
        isTarget -> PrimaryAccent.copy(alpha = 0.6f)
        else -> Color(0xFF0F172A) // Darker than background
    }

    val accentColor = when {
        isPlaying || isTarget -> PrimaryAccent
        isDetected -> SuccessAccent
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(highlightColor)
            .then(
                if (isHighlighted) {
                    Modifier.border(
                        1.5.dp, 
                        accentColor,
                        RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isHighlighted) {
            Text(
                text = note.displayName.lowercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
