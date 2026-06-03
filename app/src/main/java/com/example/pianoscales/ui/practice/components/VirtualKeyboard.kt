package com.example.pianoscales.ui.practice.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    onKeyClick: (Note) -> Unit = {},
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
                            hasActiveTarget = targetNote != null,
                            onClick = { onKeyClick(note) },
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
                BlackKeyAt(Note.C_SHARP, targetNote, detectedNote, playingNote, onKeyClick, whiteKeyWidth * 1f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // D# (between D and E)
                BlackKeyAt(Note.D_SHARP, targetNote, detectedNote, playingNote, onKeyClick, whiteKeyWidth * 2f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // F# (between F and G)
                BlackKeyAt(Note.F_SHARP, targetNote, detectedNote, playingNote, onKeyClick, whiteKeyWidth * 4f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // G# (between G and A)
                BlackKeyAt(Note.G_SHARP, targetNote, detectedNote, playingNote, onKeyClick, whiteKeyWidth * 5f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // A# (between A and B)
                BlackKeyAt(Note.A_SHARP, targetNote, detectedNote, playingNote, onKeyClick, whiteKeyWidth * 6f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
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
    onKeyClick: (Note) -> Unit,
    offset: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    BlackKey(
        note = note,
        isTarget = note == targetNote,
        isDetected = note == detectedNote,
        isPlaying = note == playingNote,
        hasActiveTarget = targetNote != null,
        onClick = { onKeyClick(note) },
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
    hasActiveTarget: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHighlighted = isTarget || isDetected || isPlaying
    
    // Pulse animation for target note
    val infiniteTransition = rememberInfiniteTransition(label = "TargetPulse")
    val pulseAlpha by if (isTarget) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val highlightColor by animateColorAsState(
        targetValue = when {
            isPlaying -> PrimaryAccent.copy(alpha = 0.4f)
            isDetected && isTarget -> SuccessAccent.copy(alpha = 0.5f)
            isDetected && hasActiveTarget -> Color(0xFFEF4444).copy(alpha = 0.5f)
            isDetected -> SuccessAccent.copy(alpha = 0.3f)
            isTarget -> PrimaryAccent.copy(alpha = pulseAlpha)
            else -> Color.White
        },
        label = "HighlightColor"
    )

    val accentColor = when {
        isPlaying -> PrimaryAccent
        isDetected && isTarget -> SuccessAccent
        isDetected && hasActiveTarget -> Color(0xFFEF4444)
        isDetected -> SuccessAccent
        isTarget -> PrimaryAccent
        else -> Color(0xFFE2E8F0)
    }

    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(highlightColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .then(
                if (isHighlighted) {
                    Modifier.border(
                        if (isTarget) 3.dp else 2.dp, 
                        accentColor, 
                        RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                } else Modifier.border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = note.displayName.lowercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Bold,
            color = if (isHighlighted) accentColor else TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun BlackKey(
    note: Note,
    isTarget: Boolean,
    isDetected: Boolean,
    isPlaying: Boolean,
    hasActiveTarget: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHighlighted = isTarget || isDetected || isPlaying

    // Pulse animation for target note
    val infiniteTransition = rememberInfiniteTransition(label = "TargetPulseBlack")
    val pulseAlpha by if (isTarget) {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlphaBlack"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    
    val highlightColor by animateColorAsState(
        targetValue = when {
            isPlaying -> PrimaryAccent.copy(alpha = 0.9f)
            isDetected && isTarget -> SuccessAccent.copy(alpha = 0.8f)
            isDetected && hasActiveTarget -> Color(0xFFEF4444).copy(alpha = 0.8f)
            isDetected -> SuccessAccent.copy(alpha = 0.8f)
            isTarget -> PrimaryAccent.copy(alpha = pulseAlpha)
            else -> Color(0xFF0F172A)
        },
        label = "HighlightColorBlack"
    )

    val accentColor = when {
        isPlaying -> PrimaryAccent
        isDetected && isTarget -> SuccessAccent
        isDetected && hasActiveTarget -> Color(0xFFEF4444)
        isDetected -> SuccessAccent
        isTarget -> PrimaryAccent
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(highlightColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .then(
                if (isHighlighted) {
                    Modifier.border(
                        if (isTarget) 2.5.dp else 1.5.dp, 
                        accentColor,
                        RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = note.displayName.lowercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) Color.White else TextMuted.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
