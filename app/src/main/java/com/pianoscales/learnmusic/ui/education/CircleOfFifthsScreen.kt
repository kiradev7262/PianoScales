package com.pianoscales.learnmusic.ui.education

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.theory.generators.CircleKey
import com.pianoscales.learnmusic.theory.generators.CircleOfFifthsEngine
import com.pianoscales.learnmusic.theory.generators.RelationshipType
import com.pianoscales.learnmusic.ui.components.PianoScalesDetailTopBar
import com.pianoscales.learnmusic.ui.practice.components.ReferenceKeyboard
import com.pianoscales.learnmusic.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleOfFifthsScreen(
    onBack: () -> Unit,
    viewModel: CircleOfFifthsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Guided Learning Header
            if (uiState.isGuidedMode) {
                GuidedLessonHeader(
                    text = uiState.lessonText
                ) {
                    viewModel.nextLessonStep()
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else if (uiState.currentLessonStep == 0) {
                Button(
                    onClick = { viewModel.startGuidedLesson() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Guided Lesson", color = PrimaryBackground)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Interactive Circle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp),
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

            InteractiveModeSelector(
                currentMode = uiState.interactiveMode,
                onModeSelected = { viewModel.setInteractiveMode(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info Panel
            KeyInfoPanel(
                selectedKey = uiState.selectedKey,
                isMajor = uiState.isMajor,
                highlightedNotes = uiState.highlightedNotes,
                onToggleMajorMinor = { viewModel.toggleMajorMinor() }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Piano Visualization
            ReferenceKeyboard(
                highlightedNotes = uiState.highlightedNotes,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun GuidedLessonHeader(
    text: String,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryAccent.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryAccent)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = PrimaryAccent)
            }
        }
    }
}

@Composable
fun InteractiveModeSelector(
    currentMode: CircleInteractiveMode,
    onModeSelected: (CircleInteractiveMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val modes = listOf(
            CircleInteractiveMode.NONE to "Explorer",
            CircleInteractiveMode.CHORD_FAMILIES to "Chord Families",
            CircleInteractiveMode.POWER_NOTES to "Power Notes"
        )
        
        modes.forEach { (mode, label) ->
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeSelected(mode) },
                label = { Text(label, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryAccent.copy(alpha = 0.2f),
                    selectedLabelColor = PrimaryAccent,
                    selectedLeadingIconColor = PrimaryAccent
                )
            )
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
                
                // For Chord Families, Relative Minors of Sisters are also Sisters
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
            highlightColor != null -> highlightColor.copy(alpha = 0.8f)
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
        border = if (isSelected || highlightColor != null) null else androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = if (isSmall) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = if (isSmall) 11.sp else 14.sp
            )
        }
    }
}

@Composable
fun KeyInfoPanel(
    selectedKey: CircleKey,
    isMajor: Boolean,
    highlightedNotes: List<Note>,
    onToggleMajorMinor: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isMajor) "${selectedKey.displayName} Major" else "${selectedKey.relativeMinorDisplayName} Minor",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
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
                        text = if (isMajor) "Relative Minor: ${selectedKey.relativeMinorDisplayName}m" else "Relative Major: ${selectedKey.displayName}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Scale Notes",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                highlightedNotes.forEach { note ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBackground,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
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
