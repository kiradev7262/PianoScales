package com.example.pianoscales.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.theory.ConceptCategory
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.theory.TheoryExplanation
import com.example.pianoscales.theory.fingering.FingerInfo
import com.example.pianoscales.theory.fingering.Hand
import com.example.pianoscales.ui.components.LessonCard
import com.example.pianoscales.ui.components.SectionHeader
import com.example.pianoscales.ui.components.TheoryCard
import com.example.pianoscales.ui.theme.PrimaryBackground
import com.example.pianoscales.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    rootNote: Note,
    conceptType: ConceptType,
    onBack: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Practice", "Learn", "Theory")

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.toggleListening()
        }
    }

    LaunchedEffect(rootNote, conceptType) {
        viewModel.init(rootNote, conceptType)
    }

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${rootNote.displayName} ${conceptType.displayName}", color = TextPrimary)
                            if (uiState.isLessonAlreadyCompleted || uiState.guidedPractice.lessonCompleted) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBackground)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PrimaryBackground,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (selectedTab) {
                0 -> PracticeTabContent(uiState, viewModel, launcher)
                1 -> LearnTabContent(uiState, viewModel)
                2 -> TheoryTabContent(uiState, viewModel)
            }
        }

        if (uiState.showFirstTimeCompletion) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCompletionDialog() },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissCompletionDialog() }) {
                        Text("Awesome!")
                    }
                },
                title = { Text("🎉 Lesson Completed!") },
                text = { Text("Great job! You've mastered the ${rootNote.displayName} ${conceptType.displayName}. Your progress has been saved.") },
                icon = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
        }
    }
}

@Composable
fun PracticeTabContent(
    uiState: PracticeUiState,
    viewModel: PracticeViewModel,
    launcher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PracticeProgressBar(
            current = if (uiState.guidedPractice.isRunning) uiState.guidedPractice.completedNotes.size 
                     else uiState.completedNotes.size,
            total = uiState.generatedNotes.size
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "Notes to Practice")
        
        val fingeringGuide = uiState.getCurrentFingeringGuide()

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            uiState.generatedNotes.forEachIndexed { index, note ->
                val isCompleted = if (uiState.guidedPractice.isRunning) {
                    uiState.guidedPractice.completedNotes.contains(index)
                } else {
                    uiState.completedNotes.contains(note)
                }
                val isTarget = uiState.guidedPractice.isRunning && uiState.guidedPractice.currentIndex == index

                NoteBubble(
                    note = note,
                    fingerNumber = fingeringGuide?.steps?.getOrNull(index)?.finger?.number,
                    isPlaying = uiState.currentPlayingNote == note,
                    isCompleted = isCompleted,
                    isDetected = uiState.isStablePitch && uiState.detectedNote == note,
                    isTarget = isTarget
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        MiniKeyboardView(
            targetNote = if (uiState.guidedPractice.isRunning) uiState.guidedPractice.targetNote else null,
            detectedNote = if (uiState.isStablePitch) uiState.detectedNote else null,
            playingNote = uiState.currentPlayingNote
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isListening) {
            VolumeMeter(amplitude = uiState.inputVolume)
            Spacer(modifier = Modifier.height(16.dp))
            
            DetectedNoteDisplay(
                detectedNote = uiState.detectedNote,
                frequency = uiState.detectedFrequency,
                isStable = uiState.isStablePitch
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { viewModel.playSequence() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isPlaying && !uiState.isListening
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play")
            }
            
            OutlinedButton(
                onClick = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) -> {
                            viewModel.toggleListening()
                        }
                        else -> {
                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isPlaying
            ) {
                Icon(if (uiState.isListening) Icons.Default.Close else Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isListening) "Stop" else "Listen")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        GuidedPracticeCard(
            state = uiState.guidedPractice,
            totalNotes = uiState.generatedNotes.size,
            onStart = { viewModel.startGuidedPractice() },
            onReset = { viewModel.resetGuidedPractice() },
            onPlayTarget = { viewModel.playTargetNote() }
        )
    }
}

@Composable
fun LearnTabContent(uiState: PracticeUiState, viewModel: PracticeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = "Hand Position", icon = Icons.Default.Face)
        HandSelector(
            selectedHand = uiState.selectedHand,
            onHandSelected = { viewModel.toggleHand(it) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        FingerLegendCard()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val fingeringGuide = uiState.getCurrentFingeringGuide()
        if (fingeringGuide != null) {
            TheoryCard(title = "Recommended Fingering", content = {
                Text(
                    text = uiState.theoryExplanation?.fingeringExplanation ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fingeringGuide.steps.forEach { step ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(step.note.displayName, style = MaterialTheme.typography.labelSmall)
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = step.finger.number.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            })
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Technique Tip", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Correct fingering builds muscle memory and improves speed. Start slow and ensure each finger is relaxed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun TheoryTabContent(uiState: PracticeUiState, viewModel: PracticeViewModel) {
    val theory = uiState.theoryExplanation ?: return
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TheoryCard(title = "Concept") {
            Text(theory.generalExplanation, style = MaterialTheme.typography.bodyMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TheoryCard(title = "Formula") {
            Text(
                text = theory.formula,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            theory.formulaMeaning.forEach { (key, value) ->
                Text("$key = $value", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TheoryCard(title = "Construction") {
            theory.constructionSteps.forEach { step ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(step.interval, modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(step.resultNote.displayName, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TheoryCard(title = "Interesting Fact") {
            Text(
                "The ${uiState.conceptType.displayName} is one of the most important concepts for any piano beginner to master.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun GuidedPracticeCard(
    state: GuidedPracticeState,
    totalNotes: Int,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onPlayTarget: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (state.isRunning) BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Guided Practice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (!state.isRunning) {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Guided Lesson")
                }
            } else {
                if (state.lessonCompleted) {
                    Text(
                        text = "🎉 Lesson Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalNotes / $totalNotes Notes Correct",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restart Lesson")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TARGET NOTE", style = MaterialTheme.typography.labelSmall)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = state.targetNote?.displayName ?: "--",
                                            style = MaterialTheme.typography.displayMedium,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        state.targetFinger?.let {
                                            Text(
                                                text = "Finger ${it.number}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                            state.targetFinger?.let {
                                Text(
                                    text = it.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onPlayTarget) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Target", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f).padding(start = 16.dp)
                        ) {
                            Text(
                                text = "${state.currentIndex} / $totalNotes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { state.currentIndex.toFloat() / totalNotes },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            when (val result = state.lastResult) {
                                is PracticeResult.Correct -> {
                                    Text("✅ Correct!", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                    result.expectedFinger?.let {
                                        Text("Expected Finger: ${it.number} (${it.name})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                                is PracticeResult.Incorrect -> {
                                    Text("❌ Try Again", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    Text("Expected ${result.expected.displayName}, detected ${result.detected.displayName}", style = MaterialTheme.typography.bodySmall)
                                }
                                null -> {
                                    Text("Play the note above", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                        Text("Reset Lesson")
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeProgressBar(current: Int, total: Int) {
    if (total == 0) return
    val progress = current.toFloat() / total.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ProgressBar"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$current / $total Notes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline,
        )
        if (progress >= 1f) {
            Text(
                text = "Scale Complete! 🎉",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VolumeMeter(amplitude: Float) {
    val animatedAmplitude by animateFloatAsState(
        targetValue = (amplitude * 5f).coerceIn(0f, 1f), // Scale up for better visibility
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "VolumeMeter"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Input Level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedAmplitude },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (animatedAmplitude > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
fun DetectedNoteDisplay(detectedNote: Note?, frequency: Float, isStable: Boolean) {
    val statusText = when {
        detectedNote == null -> "🎤 Listening..."
        !isStable -> "Detecting..."
        else -> "✅ Stable Note Detected"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            color = if (isStable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isStable) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isStable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = detectedNote?.displayName ?: "--",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (isStable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (frequency > 0) {
            Text(
                text = String.format("%.1f Hz", frequency),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun NoteBubble(
    note: Note,
    fingerNumber: Int? = null,
    isPlaying: Boolean,
    isCompleted: Boolean,
    isDetected: Boolean,
    isTarget: Boolean = false
) {
    val targetContainerColor = when {
        isTarget -> MaterialTheme.colorScheme.primaryContainer
        isCompleted -> MaterialTheme.colorScheme.tertiary
        isPlaying -> MaterialTheme.colorScheme.primary
        isDetected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val targetContentColor = when {
        isTarget -> MaterialTheme.colorScheme.onPrimaryContainer
        isCompleted || isPlaying -> MaterialTheme.colorScheme.onPrimary
        isDetected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 300),
        label = "ContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 300),
        label = "ContentColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDetected || isPlaying || isTarget) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "Scale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .size(width = 64.dp, height = 80.dp)
            .scale(scale)
            .then(
                if (isDetected || isTarget) Modifier.border(
                    width = if (isTarget) 3.dp else 2.dp,
                    color = if (isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shadowElevation = if (isPlaying || isDetected || isTarget) 8.dp else 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isTarget && !isCompleted) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = note.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            fingerNumber?.let { 
                Text(
                    text = "($it)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying || isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }
}
