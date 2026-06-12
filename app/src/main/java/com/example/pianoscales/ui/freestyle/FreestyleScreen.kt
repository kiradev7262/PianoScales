package com.example.pianoscales.ui.freestyle

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.components.PianoScalesHomeTopBar
import com.example.pianoscales.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PianoKey(val note: Note, val octave: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreestyleScreen(
    viewModel: FreestyleViewModel = hiltViewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            if (!isLandscape) {
                PianoScalesHomeTopBar(
                    title = "Freestyle Playground",
                    showAvatar = false
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) PaddingValues(0.dp) else padding)
                .padding(if (isLandscape) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLandscape) {
                // Mini header for landscape to save space
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Piano Explorer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "C3 - C7",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryAccent
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (isLandscape) it.weight(1f) else it },
                shape = RoundedCornerShape(if (isLandscape) 16.dp else 24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(if (isLandscape) 12.dp else 20.dp)
                        .fillMaxHeight(),
                    verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
                ) {
                    if (!isLandscape) {
                        Text(
                            text = "Piano Explorer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Explore five octaves (C3-C7). Swipe horizontally to move.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    FreestylePiano(
                        onNoteClick = { note, octave -> viewModel.playNote(note, octave) },
                        height = if (isLandscape) 280.dp else 240.dp
                    )
                }
            }
            
            if (!isLandscape) {
                Spacer(modifier = Modifier.height(24.dp))
                
                InfoCard(
                    title = "Multi-Octave Range",
                    description = "Navigate from C3 to C7. Each octave is visually distinct and fully playable."
                )
            }
        }
    }
}

@Composable
fun FreestylePiano(
    onNoteClick: (Note, Int) -> Unit,
    height: androidx.compose.ui.unit.Dp = 240.dp
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val activeHighlights = remember { mutableStateMapOf<String, Boolean>() }
    
    val whiteKeyWidth = 60.dp
    val blackKeyWidth = whiteKeyWidth * 0.65f
    val blackKeyHeight = height * 0.55f
    
    val octaves = listOf(3, 4, 5, 6, 7)
    val whiteNotesPerOctave = listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B)
    val blackKeyOffsets = listOf(
        Note.C_SHARP to 1f,
        Note.D_SHARP to 2f,
        Note.F_SHARP to 4f,
        Note.G_SHARP to 5f,
        Note.A_SHARP to 6f
    )

    // Calculate center scroll position (Middle of C4-C5 range)
    LaunchedEffect(Unit) {
        // Scroll to approx C4 (Octave 4 start)
        val scrollTarget = (1 * 7 * whiteKeyWidth.value).dp // 1 full octave (3) before octave 4
        scrollState.scrollTo((scrollTarget.value * 2.5).toInt()) // Approximate centering
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .horizontalScroll(scrollState)
    ) {
        // White Keys
        Row(modifier = Modifier.fillMaxHeight()) {
            octaves.forEach { octave ->
                whiteNotesPerOctave.forEach { note ->
                    val keyId = "${note.name}$octave"
                    WhiteKey(
                        note = note,
                        octave = octave,
                        isHighlighted = activeHighlights[keyId] == true,
                        onClick = { 
                            activeHighlights[keyId] = true
                            coroutineScope.launch {
                                delay(200)
                                activeHighlights[keyId] = false
                            }
                            onNoteClick(note, octave) 
                        },
                        modifier = Modifier
                            .width(whiteKeyWidth)
                            .fillMaxHeight()
                    )
                }
            }
            // Add final C8 or just stop at C7. The request said C3-C7 inclusive.
            // Note: If we end at B7, C7 is the last C.
        }
        
        // Black Keys
        octaves.forEachIndexed { octaveIdx, octave ->
            blackKeyOffsets.forEach { (note, whiteKeyOffset) ->
                val overallOffset = whiteKeyWidth * (octaveIdx * 7 + whiteKeyOffset) - (blackKeyWidth / 2)
                val keyId = "${note.name}$octave"
                
                BlackKey(
                    note = note,
                    octave = octave,
                    isHighlighted = activeHighlights[keyId] == true,
                    onClick = { 
                        activeHighlights[keyId] = true
                        coroutineScope.launch {
                            delay(200)
                            activeHighlights[keyId] = false
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
    octave: Int,
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
            text = "${note.displayName}$octave",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) PrimaryBackground else TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun BlackKey(
    note: Note,
    octave: Int,
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
            text = "${note.displayName}$octave",
            style = MaterialTheme.typography.labelSmall,
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
