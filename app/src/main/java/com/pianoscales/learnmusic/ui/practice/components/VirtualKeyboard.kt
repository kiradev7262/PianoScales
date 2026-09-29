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
    highlightedNotes: List<Note> = emptyList(),
    rootNote: Note? = null,
    isExplorer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val activeHighlights = remember { mutableStateMapOf<Note, Boolean>() }

    val startNote = remember(rootNote, isExplorer) {
        if (isExplorer && rootNote != null) rootNote else calculateBestStartNote(rootNote)
    }
    val (whiteNotes, blackKeyList) = remember(startNote) { getKeyboardRange(startNote) }
    val isBlackStart = remember(startNote) { !isWhiteKey(startNote) }

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
                val totalWidth = maxWidth
                val blackKeyWidthFactor = 0.65f
                
                // If we start with a black key, we need space for half of it at the left.
                val whiteKeyWidth = if (isBlackStart) {
                    totalWidth / (7f + blackKeyWidthFactor / 2f)
                } else {
                    totalWidth / 7f
                }
                
                val blackKeyWidth = whiteKeyWidth * blackKeyWidthFactor
                val blackKeyHeight = 110.dp
                val startPadding = if (isBlackStart) blackKeyWidth / 2 else 0.dp
                
                Box(modifier = Modifier.padding(start = startPadding)) {
                    // White Keys
                    Row(modifier = Modifier.fillMaxSize()) {
                        whiteNotes.forEach { note ->
                            val isHighlighted = activeHighlights[note] == true || highlightedNotes.contains(note)
                            WhiteKey(
                                note = note,
                                isHighlighted = isHighlighted,
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
                    blackKeyList.forEach { (note, whiteKeyOffset) ->
                        val isHighlighted = activeHighlights[note] == true || highlightedNotes.contains(note)
                        BlackKeyAt(
                            note = note,
                            isHighlighted = isHighlighted,
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

private fun calculateBestStartNote(root: Note?): Note {
    if (root == null) return Note.C
    
    val whiteNotesList = listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B)
    
    // Map root to its nearest white key (on the left if black)
    val rootBase = when (root) {
        Note.C_SHARP -> Note.C
        Note.D_SHARP -> Note.D
        Note.F_SHARP -> Note.F
        Note.G_SHARP -> Note.G
        Note.A_SHARP -> Note.A
        else -> root
    }
    
    val rootIndex = whiteNotesList.indexOf(rootBase)
    val startWhiteIndex = (rootIndex - 1 + whiteNotesList.size) % whiteNotesList.size
    return whiteNotesList[startWhiteIndex]
}

private fun isWhiteKey(note: Note): Boolean {
    return note == Note.C || note == Note.D || note == Note.E || note == Note.F || 
           note == Note.G || note == Note.A || note == Note.B
}

private fun getKeyboardRange(startNote: Note): Pair<List<Note>, List<Pair<Note, Float>>> {
    val whiteNotes = mutableListOf<Note>()
    val blackKeys = mutableListOf<Pair<Note, Float>>()

    val allNotes = Note.entries
    val startOrdinal = startNote.ordinal
    
    var whiteCount = 0
    var i = 0
    
    while (whiteCount < 7) {
        val note = allNotes[(startOrdinal + i) % 12]
        if (isWhiteKey(note)) {
            whiteNotes.add(note)
            whiteCount++
        } else {
            // Include black keys. Offset is relative to whiteCount.
            // If the startNote itself is black, its whiteCount will be 0 when i is 0.
            if (whiteCount == 0 && i == 0) {
                blackKeys.add(note to 0.0f)
            } else if (whiteCount in 1..6) {
                blackKeys.add(note to whiteCount.toFloat())
            }
        }
        i++
    }
    
    return whiteNotes to blackKeys
}
