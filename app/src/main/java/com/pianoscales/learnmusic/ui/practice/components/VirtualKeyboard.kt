package com.pianoscales.learnmusic.ui.practice.components

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
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReferenceKeyboard(
    onKeyClick: (Note) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val activeHighlights = remember { mutableStateMapOf<Note, Boolean>() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Reference Keyboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Text(
                text = "Tap any note to hear its sound and explore the keyboard layout.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
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
                            isHighlighted = activeHighlights[note] == true,
                            onClick = { 
                                activeHighlights[note] = true
                                coroutineScope.launch {
                                    delay(200)
                                    activeHighlights[note] = false
                                }
                                onKeyClick(note) 
                            },
                            modifier = Modifier
                                .width(whiteKeyWidth)
                                .fillMaxHeight()
                        )
                    }
                }
                
                // Black Keys Positioning
                val blackKeyWidth = whiteKeyWidth * 0.65f
                val blackKeyHeight = 110.dp
                
                // Black Keys
                val blackKeyList = listOf(
                    Note.C_SHARP to 1f,
                    Note.D_SHARP to 2f,
                    Note.F_SHARP to 4f,
                    Note.G_SHARP to 5f,
                    Note.A_SHARP to 6f
                )

                blackKeyList.forEach { (note, whiteKeyOffset) ->
                    BlackKeyAt(
                        note = note,
                        isHighlighted = activeHighlights[note] == true,
                        onKeyClick = onKeyClick,
                        onNoteTapped = { 
                            activeHighlights[note] = true
                            coroutineScope.launch {
                                delay(200)
                                activeHighlights[note] = false
                            }
                        },
                        offset = whiteKeyWidth * whiteKeyOffset - (blackKeyWidth / 2),
                        width = blackKeyWidth,
                        height = blackKeyHeight
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BlackKeyAt(
    note: Note,
    isHighlighted: Boolean,
    onKeyClick: (Note) -> Unit,
    onNoteTapped: (Note) -> Unit,
    offset: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    BlackKey(
        note = note,
        isHighlighted = isHighlighted,
        onClick = { 
            onNoteTapped(note)
            onKeyClick(note) 
        },
        modifier = Modifier
            .offset(x = offset)
            .width(width)
            .height(height)
    )
}

@Composable
private fun WhiteKey(
    note: Note,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(if (isHighlighted) PrimaryAccent else Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .border(0.5.dp, if (isHighlighted) PrimaryAccent else Color(0xFFE2E8F0), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = note.displayName.lowercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) PrimaryBackground else TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun BlackKey(
    note: Note,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(if (isHighlighted) PrimaryAccent else Color(0xFF0F172A))
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
            color = if (isHighlighted) PrimaryBackground else TextMuted.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
