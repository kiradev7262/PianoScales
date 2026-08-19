package com.pianoscales.learnmusic.ui.education

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.theory.generators.CircleKey
import com.pianoscales.learnmusic.theory.generators.CircleOfFifthsEngine
import com.pianoscales.learnmusic.theory.generators.RelationshipType
import com.pianoscales.learnmusic.theory.playground.PlaygroundChord
import com.pianoscales.learnmusic.theory.playground.ProgressionStyle
import com.pianoscales.learnmusic.ui.components.PianoScalesDetailTopBar
import com.pianoscales.learnmusic.ui.practice.components.ReferenceKeyboard
import com.pianoscales.learnmusic.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleOfFifthsScreen(
    onBack: () -> Unit,
    viewModel: CircleOfFifthsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            PianoScalesDetailTopBar(
                title = "Circle of Fifths",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Guided Learning CTA / Header
            if (uiState.isGuidedMode) {
                Spacer(modifier = Modifier.height(16.dp))
                GuidedLessonHeader(
                    text = uiState.lessonText
                ) {
                    viewModel.nextLessonStep()
                }
            } else if (uiState.currentLessonStep == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { viewModel.startGuidedLesson() },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryAccent),
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Guided Lesson", fontWeight = FontWeight.SemiBold)
                }
            }

            // Interactive Circle - Main Piece
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircleOfFifthsVisualization(
                    selectedKey = uiState.selectedKey,
                    visibleKeyCount = uiState.visibleKeyCount,
                    interactiveMode = uiState.interactiveMode
                ) {
                    viewModel.selectKey(it)
                }
            }

            // Mode Selector - Segmented Control Style
            InteractiveModeSelector(
                currentMode = uiState.interactiveMode,
                onModeSelected = { viewModel.setInteractiveMode(it) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info Panel - Restructured Card
            KeyInfoPanel(
                selectedKey = uiState.selectedKey,
                isMajor = uiState.isMajor,
                highlightedNotes = uiState.highlightedNotes,
                onToggleMajorMinor = { viewModel.toggleMajorMinor() },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Piano Visualization
            AnimatedVisibility(visible = uiState.interactiveMode != CircleInteractiveMode.PLAYGROUND) {
                Column {
                    Text(
                        text = "Visualization",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()
                    )
                    ReferenceKeyboard(
                        highlightedNotes = uiState.highlightedNotes,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            AnimatedVisibility(visible = uiState.interactiveMode == CircleInteractiveMode.PLAYGROUND) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    PlaygroundPanel(
                        sisterChords = uiState.sisterChords,
                        cousinChords = uiState.cousinChords,
                        isGenerating = uiState.isGeneratingProgression
                    ) {
                        viewModel.generateJam()
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (uiState.isGeneratingProgression) {
                        GenerationLoader(message = uiState.generationMessage)
                    } else if (uiState.generatedProgression != null) {
                        JamResultPanel(
                            progression = uiState.generatedProgression!!,
                            currentStyle = uiState.progressionStyle,
                            onRegenerate = { viewModel.generateJam() },
                            onStyleChange = { viewModel.setProgressionStyle(it) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GuidedLessonHeader(
    text: String,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryAccent.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                lineHeight = 20.sp
            )
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(32.dp).background(PrimaryAccent, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, 
                    contentDescription = "Next", 
                    tint = PrimaryBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun InteractiveModeSelector(
    currentMode: CircleInteractiveMode,
    onModeSelected: (CircleInteractiveMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, ElevatedSurface)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            val modes = listOf(
                CircleInteractiveMode.NONE to "Explorer",
                CircleInteractiveMode.CHORD_FAMILIES to "Families",
                CircleInteractiveMode.POWER_NOTES to "Power",
                CircleInteractiveMode.PLAYGROUND to "Playground ✨"
            )
            
            modes.forEach { (mode, label) ->
                val isSelected = currentMode == mode
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryAccent else Color.Transparent,
                    label = "bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryBackground else TextMuted,
                    label = "text"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .clickable { onModeSelected(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CircleOfFifthsVisualization(
    selectedKey: CircleKey,
    visibleKeyCount: Int,
    interactiveMode: CircleInteractiveMode,
    onKeySelected: (CircleKey) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val radius = minOf(maxWidth, maxHeight) / 2
        
        CircleOfFifthsEngine.fullCircle.forEachIndexed { index, circleKey ->
            val angle = ((index * 30.0) - 90.0) * (Math.PI / 180.0)
            val isSelected = selectedKey == circleKey
            val isVisible = index < visibleKeyCount || (index == 11 && visibleKeyCount >= 9)
            
            val scale by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "scale"
            )

            if (scale > 0f) {
                // Outer Ring (Major)
                val outerX = (radius.value * 0.82f * cos(angle)).dp
                val outerY = (radius.value * 0.82f * sin(angle)).dp
                
                val outerHighlight = when (interactiveMode) {
                    CircleInteractiveMode.CHORD_FAMILIES -> {
                        when (CircleOfFifthsEngine.getChordFamilyRelationship(selectedKey, circleKey)) {
                            RelationshipType.SISTER -> SuccessAccent
                            RelationshipType.COUSIN -> SecondaryAccent
                            else -> if (isSelected) PrimaryAccent else null
                        }
                    }
                    CircleInteractiveMode.POWER_NOTES -> {
                        if (CircleOfFifthsEngine.isPowerNote(selectedKey, circleKey)) PrimaryAccent else null
                    }
                    CircleInteractiveMode.PLAYGROUND -> if (isSelected) PrimaryAccent else null
                    CircleInteractiveMode.NONE -> if (isSelected) PrimaryAccent else null
                }

                KeyNode(
                    label = circleKey.displayName,
                    isSelected = isSelected,
                    highlightColor = outerHighlight,
                    onClick = { onKeySelected(circleKey) },
                    modifier = Modifier
                        .scale(scale)
                        .offset(x = outerX + radius - 26.dp, y = outerY + radius - 26.dp),
                    isSmall = false
                )

                // Inner Ring (Minor)
                val innerX = (radius.value * 0.52f * cos(angle)).dp
                val innerY = (radius.value * 0.52f * sin(angle)).dp
                
                val innerHighlight = when (interactiveMode) {
                    CircleInteractiveMode.CHORD_FAMILIES -> outerHighlight
                    else -> null
                }

                KeyNode(
                    label = "${circleKey.relativeMinorDisplayName}m",
                    isSelected = false,
                    highlightColor = innerHighlight,
                    onClick = { onKeySelected(circleKey) },
                    modifier = Modifier
                        .scale(scale)
                        .offset(x = innerX + radius - 20.dp, y = innerY + radius - 20.dp),
                    isSmall = true
                )
            }
        }
    }
}

@Composable
fun KeyNode(
    label: String,
    isSelected: Boolean,
    highlightColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> PrimaryAccent
            highlightColor != null -> highlightColor.copy(alpha = 0.9f)
            else -> CardSurface
        },
        label = "bgColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected || highlightColor != null) PrimaryBackground else TextPrimary,
        label = "textColor"
    )
    
    val size = if (isSmall) 40.dp else 52.dp

    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = backgroundColor,
        tonalElevation = if (isSelected) 8.dp else 2.dp,
        border = if (isSelected || highlightColor != null) null else BorderStroke(1.dp, ElevatedSurface)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = if (isSmall) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = if (isSmall) 10.sp else 14.sp
            )
        }
    }
}

@Composable
fun KeyInfoPanel(
    selectedKey: CircleKey,
    isMajor: Boolean,
    highlightedNotes: List<Note>,
    onToggleMajorMinor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, ElevatedSurface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = if (isMajor) "${selectedKey.displayName} Major" else "${selectedKey.relativeMinorDisplayName} Minor",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedKey.accidentalCount == 0) "No sharps or flats" 
                               else "${selectedKey.accidentalCount} ${if (selectedKey.isSharp) "Sharps" else "Flats"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryAccent.copy(alpha = 0.1f),
                    modifier = Modifier.clickable { onToggleMajorMinor() }
                ) {
                    Text(
                        text = if (isMajor) "→ ${selectedKey.relativeMinorDisplayName}m" else "→ ${selectedKey.displayName}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Notes in this Scale",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                highlightedNotes.forEach { note ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryBackground.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, ElevatedSurface)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = CircleOfFifthsEngine.getNoteName(note, selectedKey),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaygroundPanel(
    sisterChords: List<PlaygroundChord>,
    cousinChords: List<PlaygroundChord>,
    isGenerating: Boolean,
    onJam: () -> Unit
) {
    Column {
        Text(
            text = "MUSICIAN'S PLAYGROUND",
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Sister Chords",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(text = "Highly related diatonic chords", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ChordRow(chords = sisterChords)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Cousin Chords",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(text = "Harmonically interesting relatives", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ChordRow(chords = cousinChords)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onJam,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
            enabled = !isGenerating
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("✨ JAM FROM HERE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ChordRow(chords: List<PlaygroundChord>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chords.take(4).forEach { chord ->
            ChordCard(chord = chord, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ChordCard(chord: PlaygroundChord, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, ElevatedSurface)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = chord.romanNumeral,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = chord.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun GenerationLoader(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryAccent.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = PrimaryAccent, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun JamResultPanel(
    progression: com.pianoscales.learnmusic.theory.playground.PlaygroundProgression,
    currentStyle: ProgressionStyle,
    onRegenerate: () -> Unit,
    onStyleChange: (ProgressionStyle) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryAccent.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR JAM",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                IconButton(onClick = onRegenerate) {
                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = PrimaryAccent)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                progression.chords.forEach { chord ->
                    JamChordCard(chord = chord, modifier = Modifier.weight(1f))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Style",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressionStyle.entries.take(4).forEach { style ->
                    StyleChip(
                        style = style,
                        isSelected = style == currentStyle,
                        onClick = { onStyleChange(style) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* TODO: Play */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Practice */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SuccessAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessAccent)
                ) {
                    Icon(Icons.Rounded.Piano, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Practice")
                }
            }
        }
    }
}

@Composable
fun JamChordCard(chord: PlaygroundChord, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = PrimaryBackground,
        border = BorderStroke(1.dp, ElevatedSurface),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = chord.romanNumeral,
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryAccent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = chord.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun StyleChip(
    style: ProgressionStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) PrimaryAccent else PrimaryBackground.copy(alpha = 0.5f),
        border = if (isSelected) null else BorderStroke(1.dp, ElevatedSurface)
    ) {
        Text(
            text = style.name.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) PrimaryBackground else TextPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
