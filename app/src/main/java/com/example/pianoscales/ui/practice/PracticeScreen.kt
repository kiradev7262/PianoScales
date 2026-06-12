package com.example.pianoscales.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.components.PianoScalesDetailTopBar
import com.example.pianoscales.ui.practice.components.*
import com.example.pianoscales.ui.theme.*

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
    val tabs = listOf("Theory", "Learn", "Practice")

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MIC DEBUG", "Permission Granted\nStarting Guided Session Listening Engine")
            uiState.pendingActionAfterPermission?.invoke()
        } else {
            android.util.Log.d("MIC DEBUG", "Permission Denied")
        }
    }

    fun checkAndRun(entryPoint: String, action: () -> Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) -> {
                android.util.Log.d("MIC DEBUG", "Entry Point: $entryPoint\nPermission Status: GRANTED")
                action()
            }
            else -> {
                android.util.Log.d("MIC DEBUG", "Entry Point: $entryPoint\nPermission Status: NOT_GRANTED\nRequesting Permission...")
                viewModel.setPendingAction(action)
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(rootNote, conceptType) {
        viewModel.init(rootNote, conceptType)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            Column {
                PianoScalesDetailTopBar(
                    title = "${rootNote.displayName} ${conceptType.displayName}",
                    onBack = onBack,
                    isCompleted = uiState.isLessonAlreadyCompleted || uiState.guidedPractice.lessonCompleted
                )
                
                PillTabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
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
            Spacer(modifier = Modifier.height(16.dp))
            
            when (selectedTab) {
                0 -> TheoryTabContent(uiState, viewModel)
                1 -> LearnTabContent(uiState, viewModel)
                2 -> PracticeTabContent(uiState, viewModel) { entryPoint, action ->
                    checkAndRun(entryPoint, action)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (uiState.showFirstTimeCompletion) {
            AlertDialog(
                containerColor = CardSurface,
                onDismissRequest = { viewModel.dismissCompletionDialog() },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissCompletionDialog() }) {
                        Text("Awesome!", color = PrimaryAccent)
                    }
                },
                title = { Text("🎉 Lesson Completed!", color = TextPrimary) },
                text = { Text("Great job! You've mastered the ${rootNote.displayName} ${conceptType.displayName}. Your progress has been saved.", color = TextSecondary) },
                icon = { Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryAccent) }
            )
        }
    }
}

@Composable
fun PracticeTabContent(
    uiState: PracticeUiState,
    viewModel: PracticeViewModel,
    checkAndRun: (String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LessonSummaryCard(
            lessonName = "${uiState.rootNote.displayName} ${uiState.conceptType.displayName}",
            completedNotes = if (uiState.guidedPractice.isRunning) uiState.guidedPractice.completedNotes.size
                            else uiState.completedNotes.size,
            totalNotes = uiState.generatedNotes.size,
            formula = uiState.theoryExplanation?.formula ?: "--"
        )

        Spacer(modifier = Modifier.height(16.dp))


        if (uiState.isListening) {
            VolumeMeter(amplitude = uiState.inputVolume)
            Spacer(modifier = Modifier.height(16.dp))
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notes to Practice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButtonCard(
                    title = "Play",
                    icon = Icons.Default.PlayArrow,
                    onClick = { viewModel.playSequence() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isPlaying && !uiState.isListening
                )

                ActionButtonCard(
                    title = if (uiState.isListening) "Stop" else "Listen",
                    icon = if (uiState.isListening) Icons.Default.Close else Icons.Default.Info,
                    onClick = {
                        if (uiState.isListening) {
                            viewModel.toggleListening()
                        } else {
                            checkAndRun("Manual Listen") { viewModel.startListeningWithPermission() }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isPlaying,
                    containerColor = if (uiState.isListening) SuccessAccent.copy(alpha = 0.2f) else CardSurface,
                    contentColor = if (uiState.isListening) SuccessAccent else PrimaryAccent
                )
            }

        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val fingeringGuide = uiState.getCurrentFingeringGuide()
        val lazyListState = rememberLazyListState()

        // Auto-scroll logic
        LaunchedEffect(uiState.currentPlayingIndex, uiState.guidedPractice.currentIndex) {
            val rawIndex = if (uiState.guidedPractice.isRunning) {
                uiState.guidedPractice.currentIndex
            } else if (uiState.isPlaying) {
                uiState.currentPlayingIndex
            } else {
                -1
            }

            val indexToScroll = if (rawIndex != -1) {
                rawIndex.coerceAtMost(uiState.generatedNotes.size - 1)
            } else {
                -1
            }

            if (indexToScroll != -1) {
                val activeNote = uiState.generatedNotes.getOrNull(indexToScroll)
                val octave = if (uiState.isPlaying) {
                    uiState.currentPlayingOctave
                } else {
                    // Calculate octave for guided practice
                    var lastNoteOrdinal = -1
                    var currentOctave = 4
                    for (i in 0..indexToScroll) {
                        val note = uiState.generatedNotes.getOrNull(i) ?: break
                        if (i > 0 && note.ordinal <= lastNoteOrdinal) {
                            currentOctave++
                        }
                        lastNoteOrdinal = note.ordinal
                    }
                    currentOctave
                }

                android.util.Log.d("SCROLL DEBUG", """
                    [SCROLL DEBUG]
                    Active Note: ${activeNote?.displayName}$octave
                    Index: ${indexToScroll + 1}
                    Scroll Trigger: TRUE
                    Action: SmoothScrollToItem
                """.trimIndent())
                
                // Predictive scroll: scroll to a position that keeps the active note visible and somewhat centered
                val targetScrollIndex = (indexToScroll - 1).coerceAtLeast(0)
                lazyListState.animateScrollToItem(targetScrollIndex)
            }
        }

        LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(uiState.generatedNotes) { index, note ->
                val isCompleted = if (uiState.guidedPractice.isRunning) {
                    uiState.guidedPractice.completedNotes.contains(index)
                } else {
                    uiState.completedNotes.contains(note)
                }
                val isTarget = uiState.guidedPractice.isRunning && uiState.guidedPractice.currentIndex == index
                val isDetected = uiState.isStablePitch && uiState.detectedNote == note
                
                val lastResult = uiState.guidedPractice.lastResult
                val isIncorrect = uiState.guidedPractice.isRunning && 
                                 index == uiState.guidedPractice.currentIndex && 
                                 lastResult is PracticeResult.Incorrect

                NoteChip(
                    note = note,
                    fingerNumber = fingeringGuide?.steps?.getOrNull(index)?.finger?.number,
                    isPlaying = uiState.currentPlayingNote == note,
                    isCompleted = isCompleted,
                    isDetected = isDetected,
                    isTarget = isTarget,
                    isIncorrect = isIncorrect
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GuidedPracticeCard(
            state = uiState.guidedPractice,
            totalNotes = uiState.generatedNotes.size,
            onStart = { checkAndRun("Guided Session") { viewModel.startGuidedPracticeWithPermission() } },
            onReset = { checkAndRun("Guided Session") { viewModel.startGuidedPracticeWithPermission() } },
            onCancel = { viewModel.stopGuidedPractice() },
            onPlayTarget = { viewModel.playTargetNote() }
        )


        Spacer(modifier = Modifier.height(16.dp))



        ReferenceKeyboard(
            onKeyClick = { viewModel.onKeyClick(it) }
        )


    }
}

@Composable
fun LearnTabContent(uiState: PracticeUiState, viewModel: PracticeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PracticeTheoryCard(title = "Hand Selection") {
            HandToggle(
                selectedHand = uiState.selectedHand,
                onHandSelected = { viewModel.toggleHand(it) }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        FingerLegendCard(selectedHand = uiState.selectedHand)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val fingeringGuide = uiState.getCurrentFingeringGuide()
        if (fingeringGuide != null) {
            PracticeTheoryCard(title = "Recommended Fingering") {
                Text(
                    text = uiState.theoryExplanation?.fingeringExplanation ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fingeringGuide.steps.forEach { step ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                step.note.displayName, 
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = PrimaryAccent,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = step.finger.number.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        PracticeTheoryCard(title = "Technique Tip") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryAccent)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Correct fingering builds muscle memory and improves speed. Start slow and ensure each finger is relaxed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
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
            .padding(horizontal = 20.dp)
    ) {
        PracticeTheoryCard(title = "Concept") {
            Text(theory.generalExplanation, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PracticeTheoryCard(title = "Formula") {
            Text(
                text = theory.formula,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = PrimaryAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            theory.formulaMeaning.forEach { (key, value) ->
                Text("$key = $value", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PracticeTheoryCard(title = "Construction") {
            theory.constructionSteps.forEach { step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        step.interval, 
                        modifier = Modifier.width(48.dp), 
                        fontWeight = FontWeight.Bold, 
                        color = PrimaryAccent
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = TextMuted
                    )
                    Text(
                        step.resultNote.displayName, 
                        modifier = Modifier.padding(start = 12.dp), 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PracticeTheoryCard(title = "Pattern Breakdown") {
            Text(
                "The ${uiState.conceptType.displayName} follows a specific pattern of intervals that defines its unique musical character.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun VolumeMeter(amplitude: Float) {
    val animatedAmplitude by animateFloatAsState(
        targetValue = (amplitude * 5f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "VolumeMeter"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Input Level",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedAmplitude },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (animatedAmplitude > 0.8f) Color(0xFFEF4444) else PrimaryAccent,
            trackColor = ElevatedSurface,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun DetectedNoteDisplay(detectedNote: Note?, isStable: Boolean) {
    val statusText = when {
        detectedNote == null -> "🎤 Listening..."
        !isStable -> "Detecting..."
        else -> "✓ Stable Note Detected"
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
            color = if (isStable) SuccessAccent else TextSecondary,
            fontWeight = if (isStable) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isStable) SuccessAccent.copy(alpha = 0.2f) else CardSurface,
            modifier = Modifier.size(120.dp),
            border = if (isStable) androidx.compose.foundation.BorderStroke(2.dp, SuccessAccent) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = detectedNote?.displayName ?: "--",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isStable) SuccessAccent else TextMuted
                )
            }
        }
    }
}
