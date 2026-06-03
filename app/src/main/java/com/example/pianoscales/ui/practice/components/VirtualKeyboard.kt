package com.example.pianoscales.ui.practice.components

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
                BlackKeyAt(Note.C_SHARP, onKeyClick, whiteKeyWidth * 1f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // D# (between D and E)
                BlackKeyAt(Note.D_SHARP, onKeyClick, whiteKeyWidth * 2f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // F# (between F and G)
                BlackKeyAt(Note.F_SHARP, onKeyClick, whiteKeyWidth * 4f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // G# (between G and A)
                BlackKeyAt(Note.G_SHARP, onKeyClick, whiteKeyWidth * 5f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
                
                // A# (between A and B)
                BlackKeyAt(Note.A_SHARP, onKeyClick, whiteKeyWidth * 6f - (blackKeyWidth / 2), blackKeyWidth, blackKeyHeight)
            }
        }
    }
}

@Composable
private fun BoxScope.BlackKeyAt(
    note: Note,
    onKeyClick: (Note) -> Unit,
    offset: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    BlackKey(
        note = note,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = note.displayName.lowercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun BlackKey(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(Color(0xFF0F172A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = note.displayName.lowercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMuted.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
