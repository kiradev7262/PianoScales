package com.pianoscales.learnmusic.ui.practice.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.ui.theme.*

@Composable
fun PillTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = CardSurface
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryAccent else Color.Transparent,
                    label = "TabBackground"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryBackground else TextSecondary,
                    label = "TabContent"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(backgroundColor)
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun PracticeHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun LessonSummaryCard(
    lessonName: String,
    completedNotes: Int,
    totalNotes: Int,
    formula: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                        text = lessonName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$completedNotes / $totalNotes Notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                
                CircularProgressIndicator(
                    progress = { if (totalNotes > 0) completedNotes.toFloat() / totalNotes else 0f },
                    modifier = Modifier.size(48.dp),
                    color = PrimaryAccent,
                    strokeWidth = 6.dp,
                    trackColor = ElevatedSurface,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            HorizontalDivider(color = ElevatedSurface, thickness = 1.dp)
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                text = "FORMULA",
//                style = MaterialTheme.typography.labelSmall,
//                fontWeight = FontWeight.Bold,
//                color = TextMuted,
//                letterSpacing = 1.sp
//            )
//
//            Text(
//                text = formula,
//                style = MaterialTheme.typography.headlineSmall,
//                fontWeight = FontWeight.Black,
//                color = PrimaryAccent
//            )
        }
    }
}

@Composable
fun NoteChip(
    note: Note,
    fingerNumber: Int? = null,
    isPlaying: Boolean,
    isCompleted: Boolean,
    isDetected: Boolean,
    isTarget: Boolean,
    isIncorrect: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> SuccessAccent
            isIncorrect -> Color(0xFFEF4444)
            isTarget -> PrimaryAccent
            isDetected -> PrimaryAccent.copy(alpha = 0.3f)
            isPlaying -> PrimaryAccent
            else -> CardSurface
        },
        label = "ChipBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isCompleted || isIncorrect || isTarget || isPlaying -> PrimaryBackground
            else -> TextPrimary
        },
        label = "ChipContent"
    )

    val scale by animateFloatAsState(
        targetValue = if (isTarget || isDetected || isPlaying) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ChipScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.scale(scale)
    ) {
        Surface(
            modifier = Modifier
                .size(width = 64.dp, height = 80.dp)
                .then(
                    if (isDetected) Modifier.border(2.dp, PrimaryAccent, RoundedCornerShape(16.dp))
                    else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor,
            shadowElevation = if (isTarget || isDetected) 8.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = note.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = contentColor
                    )
                    if (fingerNumber != null) {
                        Text(
                            text = fingerNumber.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }
                
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp),
                        tint = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButtonCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = CardSurface,
    contentColor: Color = PrimaryAccent,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun GuidedPracticeCard(
    state: com.pianoscales.learnmusic.ui.practice.GuidedPracticeState,
    totalNotes: Int,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onPlayTarget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = if (state.isRunning && !state.lessonCompleted) BorderStroke(2.dp, PrimaryAccent) else null
    ) {
        Column(modifier = Modifier.padding(20.dp , 12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Guided Practice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if(state.isRunning && !state.lessonCompleted){
                    IconButton(
                        onClick = onCancel
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Guided Practice",
                            tint = TextPrimary
                        )
                    }
                }else if(state.lessonCompleted){
                    IconButton(
                        onClick = onReset
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Lesson",
                            tint = TextPrimary
                        )
                    }
                }

            }


            if (!state.isRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                ActionButtonCard(
                    title = "Start Guided Lesson",
                    icon = Icons.Default.PlayArrow,
                    onClick = onStart,
                    containerColor = PrimaryAccent,
                    contentColor = PrimaryBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else if (state.lessonCompleted) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 Lesson Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SuccessAccent,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "You've mastered all $totalNotes notes!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TARGET",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = PrimaryAccent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = state.targetNote?.displayName ?: "--",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryBackground
                                )
                            }
                        }
                        IconButton(onClick = onPlayTarget) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PrimaryAccent)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(start = 24.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${state.currentIndex} / $totalNotes",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        
                        LinearProgressIndicator(
                            progress = { if (totalNotes > 0) state.currentIndex.toFloat() / totalNotes else 0f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = PrimaryAccent,
                            trackColor = ElevatedSurface,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        when (val result = state.lastResult) {
                            is com.pianoscales.learnmusic.ui.practice.PracticeResult.Correct -> {
                                Text("✓ Correct", color = SuccessAccent, fontWeight = FontWeight.Bold)
                            }
                            is com.pianoscales.learnmusic.ui.practice.PracticeResult.Incorrect -> {
                                Text("✗ Try Again", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                Text("Played ${result.detected.displayName}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            null -> {
                                Text("Play the note", color = TextSecondary)
                            }
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun FingerLegendCard(
    selectedHand: com.pianoscales.learnmusic.theory.fingering.Hand,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Finger Guide",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val isLeft = selectedHand == com.pianoscales.learnmusic.theory.fingering.Hand.LEFT
            
            // Visual Hand Representation (Stylized)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                if (isLeft) {
                    FingerItem("5", "Pinky", 50.dp)
                    FingerItem("4", "Ring", 75.dp)
                    FingerItem("3", "Middle", 90.dp)
                    FingerItem("2", "Index", 80.dp)
                    FingerItem("1", "Thumb", 45.dp)
                } else {
                    FingerItem("1", "Thumb", 45.dp)
                    FingerItem("2", "Index", 80.dp)
                    FingerItem("3", "Middle", 90.dp)
                    FingerItem("4", "Ring", 75.dp)
                    FingerItem("5", "Pinky", 50.dp)
                }
            }
        }
    }
}

@Composable
private fun FingerItem(number: String, name: String, height: androidx.compose.ui.unit.Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = name, 
            style = MaterialTheme.typography.labelSmall, 
            color = TextMuted, 
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(PrimaryAccent.copy(alpha = 0.15f))
                .border(
                    width = 1.dp, 
                    color = PrimaryAccent.copy(alpha = 0.4f), 
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = CircleShape,
                color = PrimaryAccent,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = number, 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold, 
                        color = PrimaryBackground
                    )
                }
            }
        }
    }
}

@Composable
fun HandToggle(
    selectedHand: com.pianoscales.learnmusic.theory.fingering.Hand,
    onHandSelected: (com.pianoscales.learnmusic.theory.fingering.Hand) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = ElevatedSurface
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            com.pianoscales.learnmusic.theory.fingering.Hand.entries.reversed().forEach { hand ->
                val isSelected = selectedHand == hand
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryAccent else Color.Transparent,
                    label = "HandToggleBackground"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryBackground else TextSecondary,
                    label = "HandToggleContent"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(backgroundColor)
                        .clickable { onHandSelected(hand) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = hand.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun PracticeTheoryCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryAccent,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
