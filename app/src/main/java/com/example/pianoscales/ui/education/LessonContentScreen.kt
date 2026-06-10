package com.example.pianoscales.ui.education

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LessonContentScreen(
    lessonId: Int,
    onBack: () -> Unit,
    onLessonComplete: () -> Unit,
    viewModel: BeginnerJourneyViewModel = hiltViewModel()
) {
    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            TopAppBar(
                title = { Text("Lesson $lessonId", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (lessonId) {
                1 -> Lesson1Content(
                    onComplete = { 
                        viewModel.completeLesson(1)
                        onLessonComplete()
                    },
                    playNote = { noteName -> 
                        try {
                            val note = Note.valueOf(noteName.replace("#", "_SHARP"))
                            viewModel.playNote(note)
                        } catch (e: Exception) {}
                    }
                )
                2 -> Lesson2Content(
                    onComplete = { 
                        viewModel.completeLesson(2)
                        onLessonComplete()
                    },
                    playNote = { noteName ->
                        try {
                            val note = Note.valueOf(noteName.replace("#", "_SHARP"))
                            viewModel.playNote(note)
                        } catch (e: Exception) {}
                    }
                )
                3 -> Lesson3Content(
                    onComplete = { 
                        viewModel.completeLesson(3)
                        onLessonComplete()
                    },
                    playNote = { note, octave -> viewModel.playNote(note, octave) }
                )
                4 -> Lesson4Content(
                    onComplete = { 
                        viewModel.completeLesson(4)
                        onLessonComplete()
                    },
                    playDemo = { notes -> viewModel.playScaleDemo(notes) }
                )
                5 -> Lesson5Content(
                    onComplete = { 
                        viewModel.completeLesson(5)
                        onLessonComplete()
                    },
                    playNote = { note, octave -> viewModel.playNote(note, octave) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Lesson1Content(onComplete: () -> Unit, playNote: (String) -> Unit) {
    val notes = listOf("C", "D", "E", "F", "G", "A", "B")
    val tappedNotes = remember { mutableStateMapOf<String, Boolean>() }
    val allTapped = tappedNotes.size == notes.size

    Text(
        text = "What Are Musical Notes?",
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Music is made of notes. In Western music, we use seven main notes named after the first seven letters of the alphabet.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))
    
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 4
    ) {
        notes.forEach { note ->
            NoteChip(
                note = note,
                isTapped = tappedNotes[note] == true,
                onClick = { 
                    tappedNotes[note] = true
                    playNote(note)
                }
            )
        }
    }
    
    Spacer(modifier = Modifier.height(48.dp))
    
    Button(
        onClick = onComplete,
        enabled = allTapped,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Complete Lesson", color = PrimaryBackground, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NoteChip(note: String, isTapped: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isTapped) PrimaryAccent else CardSurface)
            .border(1.dp, if (isTapped) PrimaryAccent else PrimaryAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = note,
            style = MaterialTheme.typography.titleLarge,
            color = if (isTapped) PrimaryBackground else TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Lesson2Content(onComplete: () -> Unit, playNote: (String) -> Unit) {
    val targetSequence = listOf("C", "D", "E", "F", "G", "A", "B", "C")
    var currentSequence by remember { mutableStateOf(emptyList<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorIndex by remember { mutableIntStateOf(-1) }
    val isCorrect = currentSequence == targetSequence

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(2000)
            errorMessage = null
            errorIndex = -1
            currentSequence = emptyList()
        }
    }

    Text(
        text = "Understanding C D E F G A B",
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Notes are arranged alphabetically. After B, the pattern repeats starting at C again.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "Tap the notes in order: C to C",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryAccent
    )
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        targetSequence.forEachIndexed { index, note ->
            val isFilled = index < currentSequence.size
            val isError = index == errorIndex
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isError) Color.Red.copy(alpha = 0.5f) else if (isFilled) PrimaryAccent else CardSurface)
                    .border(1.dp, if (isError) Color.Red else PrimaryAccent.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isFilled) {
                    Text(note, fontSize = 12.sp, color = PrimaryBackground, fontWeight = FontWeight.Bold)
                } else if (isError) {
                    Text(targetSequence[index], fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage!!,
            color = Color.Red,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("A", "B", "C", "D", "E", "F", "G").forEach { note ->
            Button(
                onClick = {
                    if (currentSequence.size < targetSequence.size && errorMessage == null) {
                        if (note == targetSequence[currentSequence.size]) {
                            currentSequence = currentSequence + note
                            playNote(note)
                        } else {
                            errorIndex = currentSequence.size
                            errorMessage = "Not quite right. Try arranging the notes again."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Text(note, color = TextPrimary)
            }
        }
    }

    Spacer(modifier = Modifier.height(48.dp))

    Button(
        onClick = onComplete,
        enabled = isCorrect,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Complete Lesson", color = PrimaryBackground, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Lesson3Content(onComplete: () -> Unit, playNote: (Note, Int) -> Unit) {
    var whiteTapped by remember { mutableStateOf(false) }
    var blackTapped by remember { mutableStateOf(false) }
    
    // Tracking current key interactions for visual feedback
    var lastWhiteTappedIndex by remember { mutableIntStateOf(-1) }
    var lastBlackTappedIndex by remember { mutableIntStateOf(-1) }

    Text(
        text = "White Keys and Black Keys",
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "White keys represent 'natural' notes. Black keys are 'sharps' or 'flats'. Tapping a black key to the right of a white key makes it 'sharp' (#).",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))

    // Simple Keyboard Diagram
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(CardSurface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            val whiteNotes = listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B)
            whiteNotes.forEachIndexed { i, note ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(if (lastWhiteTappedIndex == i) PrimaryAccent else Color.White)
                        .clickable { 
                            whiteTapped = true
                            lastWhiteTappedIndex = i
                            lastBlackTappedIndex = -1
                            playNote(note, 4)
                        }
                )
            }
        }
        // Black keys overlay (simplified)
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            val blackNotes = listOf(Note.C_SHARP, Note.D_SHARP, null, Note.F_SHARP, Note.G_SHARP, Note.A_SHARP)
            blackNotes.forEachIndexed { i, note ->
                if (note != null) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(0.6f)
                            .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (lastBlackTappedIndex == i) PrimaryAccent else Color.Black)
                            .clickable { 
                                blackTapped = true
                                lastBlackTappedIndex = i
                                lastWhiteTappedIndex = -1
                                playNote(note, 4)
                            }
                    )
                    Spacer(modifier = Modifier.weight(0.5f))
                } else {
                    Spacer(modifier = Modifier.weight(2f))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    
    val helperText = when {
        whiteTapped && blackTapped -> "Great! Task complete."
        whiteTapped -> "Great! Now tap a black key."
        blackTapped -> "Great! Now tap a white key."
        else -> "Task: Tap a white key and a black key above."
    }
    
    Text(
        text = helperText,
        style = MaterialTheme.typography.bodyMedium,
        color = if (whiteTapped && blackTapped) SuccessAccent else PrimaryAccent
    )

    Spacer(modifier = Modifier.height(48.dp))

    Button(
        onClick = onComplete,
        enabled = whiteTapped && blackTapped,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Complete Lesson", color = PrimaryBackground, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Lesson4Content(onComplete: () -> Unit, playDemo: (List<Note>) -> Unit) {
    val scales = remember {
        listOf(
            "C" to listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B, Note.C),
            "C#" to listOf(Note.C_SHARP, Note.D_SHARP, Note.F, Note.F_SHARP, Note.G_SHARP, Note.A_SHARP, Note.C, Note.C_SHARP),
            "D" to listOf(Note.D, Note.E, Note.F_SHARP, Note.G, Note.A, Note.B, Note.C_SHARP, Note.D),
            "D#" to listOf(Note.D_SHARP, Note.F, Note.G, Note.G_SHARP, Note.A_SHARP, Note.C, Note.D, Note.D_SHARP),
            "E" to listOf(Note.E, Note.F_SHARP, Note.G_SHARP, Note.A, Note.B, Note.C_SHARP, Note.D_SHARP, Note.E),
            "F" to listOf(Note.F, Note.G, Note.A, Note.A_SHARP, Note.C, Note.D, Note.E, Note.F),
            "F#" to listOf(Note.F_SHARP, Note.G_SHARP, Note.A_SHARP, Note.B, Note.C_SHARP, Note.D_SHARP, Note.F, Note.F_SHARP),
            "G" to listOf(Note.G, Note.A, Note.B, Note.C, Note.D, Note.E, Note.F_SHARP, Note.G),
            "G#" to listOf(Note.G_SHARP, Note.A_SHARP, Note.C, Note.C_SHARP, Note.D_SHARP, Note.F, Note.G, Note.G_SHARP),
            "A" to listOf(Note.A, Note.B, Note.C_SHARP, Note.D, Note.E, Note.F_SHARP, Note.G_SHARP, Note.A),
            "A#" to listOf(Note.A_SHARP, Note.C, Note.D, Note.D_SHARP, Note.F, Note.G, Note.A, Note.A_SHARP),
            "B" to listOf(Note.B, Note.C_SHARP, Note.D_SHARP, Note.E, Note.F_SHARP, Note.G_SHARP, Note.A_SHARP, Note.B)
        )
    }

    val pagerState = rememberPagerState(pageCount = { scales.size })
    var demoPlayed by remember { mutableStateOf(false) }

    Text(
        text = "What Is A Scale?",
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "A scale is a sequence of notes organized by their pitch. The most common is the Major Scale, which sounds 'happy' and bright.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        text = "Explore all 12 major scales",
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryAccent
    )
    Spacer(modifier = Modifier.height(16.dp))

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 32.dp),
        pageSpacing = 16.dp
    ) { page ->
        val (name, notes) = scales[page]
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (pagerState.currentPage == page) PrimaryAccent else Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("$name Major Scale", style = MaterialTheme.typography.titleLarge, color = PrimaryAccent)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    notes.joinToString(" - ") { it.displayName },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        demoPlayed = true
                        playDemo(notes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PrimaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Listen Demo", color = PrimaryAccent)
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Page indicator
    Row(
        Modifier.height(8.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(scales.size) { iteration ->
            val color = if (pagerState.currentPage == iteration) PrimaryAccent else CardSurface
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(color)
                    .size(8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(40.dp))

    Button(
        onClick = onComplete,
        enabled = demoPlayed,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Complete Lesson", color = PrimaryBackground, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Lesson5Content(onComplete: () -> Unit, playNote: (Note, Int) -> Unit) {
    val targetNotes = listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B, Note.C)
    var currentIndex by remember { mutableIntStateOf(0) }
    val isCompleted = currentIndex >= targetNotes.size

    Text(
        text = "Your First Major Scale",
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Now, let's play the C Major Scale. Tap the notes in order to finish your beginner journey!",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))

    // Note display
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        targetNotes.forEachIndexed { index, note ->
            val isActive = index == currentIndex
            val isPassed = index < currentIndex
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPassed) SuccessAccent else if (isActive) PrimaryAccent else CardSurface)
                    .border(1.dp, if (isActive) PrimaryAccent else Color.Transparent, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    note.displayName,
                    color = if (isPassed || isActive) PrimaryBackground else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(40.dp))

    // Interactive Keyboard (Simplified for tap)
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B, Note.C).forEachIndexed { index, note ->
            val octave = if (index == 7) 5 else 4
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(1.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color.White)
                    .clickable {
                        playNote(note, octave)
                        if (currentIndex < targetNotes.size && note == targetNotes[currentIndex]) {
                            if (currentIndex == index) {
                                currentIndex++
                            }
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(note.displayName, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(48.dp))

    Button(
        onClick = onComplete,
        enabled = isCompleted,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = SuccessAccent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Finish Journey!", color = PrimaryBackground, fontWeight = FontWeight.Bold)
    }
}
