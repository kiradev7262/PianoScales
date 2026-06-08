package com.example.pianoscales.ui.freestyle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreestyleScreen(
    viewModel: FreestyleViewModel = hiltViewModel()
) {
    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            TopAppBar(
                title = { Text("Freestyle Playground", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Piano Explorer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Explore two octaves of the piano. Tap any key to play.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    FreestylePiano(onNoteClick = { note, octave -> viewModel.playNote(note, octave) })
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            InfoCard(
                title = "Freestyle Mode",
                description = "This is a non-learning mode where you can experiment with sounds. Your progress in Journey mode is not affected by anything you play here."
            )
        }
    }
}

@Composable
fun FreestylePiano(
    onNoteClick: (Note, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val activeHighlights = remember { mutableStateMapOf<Int, Boolean>() } // Using index as key to support multiple octaves

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .horizontalScroll(scrollState)
    ) {
        val octavesCount = 2
        val whiteNotesPerOctave = listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B)
        
        // White Keys
        Row(modifier = Modifier.fillMaxHeight()) {
            for (octaveIdx in 0 until octavesCount) {
                val octave = octaveIdx + 4
                whiteNotesPerOctave.forEachIndexed { noteIdx, note ->
                    val globalIndex = octaveIdx * 7 + noteIdx
                    WhiteKey(
                        note = note,
                        isHighlighted = activeHighlights[globalIndex] == true,
                        onClick = { 
                            activeHighlights[globalIndex] = true
                            coroutineScope.launch {
                                delay(200)
                                activeHighlights[globalIndex] = false
                            }
                            onNoteClick(note, octave) 
                        },
                        modifier = Modifier
                            .width(60.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
        
        // Black Keys
        val whiteKeyWidth = 60.dp
        val blackKeyWidth = whiteKeyWidth * 0.65f
        val blackKeyHeight = 130.dp
        
        val blackKeyOffsets = listOf(
            Note.C_SHARP to 1f,
            Note.D_SHARP to 2f,
            Note.F_SHARP to 4f,
            Note.G_SHARP to 5f,
            Note.A_SHARP to 6f
        )

        for (octaveIdx in 0 until octavesCount) {
            val octave = octaveIdx + 4
            blackKeyOffsets.forEach { (note, whiteKeyOffset) ->
                val overallOffset = whiteKeyWidth * (octaveIdx * 7 + whiteKeyOffset) - (blackKeyWidth / 2)
                val highlightKey = (octaveIdx + 1) * 100 + note.ordinal // Unique key for highlighting
                
                BlackKey(
                    note = note,
                    isHighlighted = activeHighlights[highlightKey] == true,
                    onClick = { 
                        activeHighlights[highlightKey] = true
                        coroutineScope.launch {
                            delay(200)
                            activeHighlights[highlightKey] = false
                        }
                        onNoteClick(note, octave)
                    },
                    modifier = Modifier
                        .offset(x = overallOffset)
                        .width(blackKeyWidth)
                        .height(blackKeyHeight)
                )
            }
        }
    }
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

@Composable
private fun InfoCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryAccent)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
