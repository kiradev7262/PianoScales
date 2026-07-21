package com.pianoscales.learnmusic.ui.songs

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.ui.components.InfoCard
import com.pianoscales.learnmusic.ui.freestyle.FreestylePiano
import com.pianoscales.learnmusic.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongComposerScreen(
    onBack: () -> Unit,
    viewModel: SongComposerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (uiState.showTitleInput) {
        ComposerSetupScreen(
            title = uiState.title,
            onTitleChange = { viewModel.updateTitle(it) },
            pianoMode = uiState.pianoMode,
            onPianoModeChange = { viewModel.setPianoMode(it) },
            onContinue = { viewModel.startComposer() },
            onBack = onBack,
            isEditMode = uiState.isEditMode
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryBackground)
                .systemBarsPadding()
        ) {
            // Header
            ComposerHeader(
                title = uiState.title,
                onBack = { 
                    if (uiState.isEditMode) onBack() 
                    else viewModel.updateTitle(uiState.title) 
                },
                onDeleteLastNote = { viewModel.deleteLastNote() },
                onClearLine = { viewModel.clearLine() },
                onDeleteLine = { viewModel.deleteLine() },
                onFinish = { viewModel.finish(onBack) }
            )

            if (isLandscape) {
                // Landscape Layout: Side-by-side
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Panel: Info and Controls
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LineSelector(
                            currentLineIndex = uiState.currentLineIndex,
                            lineCount = uiState.lines.size,
                            onLineSelected = { viewModel.setCurrentLineIndex(it) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Scrollable Notes Display
                        Box(
                            modifier = Modifier.weight(1f, fill = false),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val scrollState = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(scrollState),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (uiState.currentLine.isEmpty()) {
                                        Text(
                                            text = "Play notes to record...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted
                                        )
                                    } else {
                                        uiState.currentLine.forEach { noteWithOctave ->
                                            Text(
                                                text = "${noteWithOctave.note.displayName}${noteWithOctave.octave}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = TextPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                        }
                                    }
                                }

                                SubtleScrollIndicator(
                                    scrollState = scrollState,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .width(100.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.addLine() },
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New Line")
                        }
                    }

                    // Right Panel: Keyboard
                    Box(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight()
                            .background(
                                color = CardSurface,
                                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FreestylePiano(
                                onNoteClick = { note, octave -> viewModel.onNotePlayed(note, octave) },
                                height = 200.dp,
                                enabled = uiState.pianoMode == PianoMode.VIRTUAL
                            )

                            if (uiState.pianoMode == PianoMode.EXTERNAL) {
                                Text(
                                    text = "Listening to External Piano...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PrimaryAccent,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Portrait Layout: Stacked
                // Current Line Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LineSelector(
                            currentLineIndex = uiState.currentLineIndex,
                            lineCount = uiState.lines.size,
                            onLineSelected = { viewModel.setCurrentLineIndex(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val scrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (uiState.currentLine.isEmpty()) {
                                Text(
                                    text = "Start playing to record notes...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            } else {
                                uiState.currentLine.forEach { noteWithOctave ->
                                    Text(
                                        text = "${noteWithOctave.note.displayName}${noteWithOctave.octave}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }

                        SubtleScrollIndicator(
                            scrollState = scrollState,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .width(120.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.addLine() },
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New Line")
                        }
                    }
                }

                // Keyboard Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.65f)
                        .background(CardSurface)
                ) {
                    Column {
                        FreestylePiano(
                            onNoteClick = { note, octave -> viewModel.onNotePlayed(note, octave) },
                            height = 240.dp,
                            enabled = uiState.pianoMode == PianoMode.VIRTUAL
                        )

                        if (!isLandscape) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                InfoCard(
                                    title = if (uiState.pianoMode == PianoMode.VIRTUAL) "Tip" else "External Mode Active",
                                    description = if (uiState.pianoMode == PianoMode.VIRTUAL) {
                                        "Rotate your device to landscape mode for a wider keyboard and the best playing experience."
                                    } else {
                                        "Play the notes on your real piano. Piano Scales is listening..."
                                    }
                                )
                            }
                        }

                        if (uiState.pianoMode == PianoMode.EXTERNAL) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Listening to External Piano...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PrimaryAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerSetupScreen(
    title: String,
    onTitleChange: (String) -> Unit,
    pianoMode: PianoMode,
    onPianoModeChange: (PianoMode) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    isEditMode: Boolean
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Song" else "Create New Song") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Song Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = TextMuted,
                    focusedLabelColor = PrimaryAccent,
                    cursorColor = PrimaryAccent
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Choose Input Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InputModeOption(
                selected = pianoMode == PianoMode.VIRTUAL,
                onClick = { onPianoModeChange(PianoMode.VIRTUAL) },
                icon = "📱",
                title = "Virtual Piano",
                description = "Record notes by tapping on the screen keyboard."
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InputModeOption(
                selected = false, // Cannot be selected
                onClick = { 
                    Toast.makeText(context, "External Piano support is coming soon.", Toast.LENGTH_SHORT).show()
                },
                icon = "🎹",
                title = "External Piano",
                description = "Record notes from your real piano via microphone.",
                enabled = false,
                isComingSoon = true
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onContinue,
                enabled = title.isNotBlank() && pianoMode != PianoMode.EXTERNAL,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Continue", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun InputModeOption(
    selected: Boolean,
    onClick: () -> Unit,
    icon: String,
    title: String,
    description: String,
    enabled: Boolean = true,
    isComingSoon: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> PrimaryAccent.copy(alpha = 0.1f)
                !enabled -> CardSurface.copy(alpha = 0.5f)
                else -> CardSurface
            }
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryAccent) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon, 
                fontSize = 32.sp,
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) PrimaryAccent else if (enabled) TextPrimary else TextMuted
                    )
                    if (isComingSoon) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = PrimaryAccent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "COMING SOON",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) TextSecondary else TextMuted
                )
            }
        }
    }
}

@Composable
fun SubtleScrollIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    if (scrollState.maxValue > 0) {
        BoxWithConstraints(
            modifier = modifier
                .height(2.dp)
                .background(TextMuted.copy(alpha = 0.1f), RoundedCornerShape(1.dp))
        ) {
            val trackWidth = maxWidth
            val thumbWidth = trackWidth * 0.3f
            val scrollFraction = scrollState.value.toFloat() / scrollState.maxValue
            val xOffset = (trackWidth - thumbWidth) * scrollFraction

            Box(
                modifier = Modifier
                    .offset(x = xOffset)
                    .width(thumbWidth)
                    .fillMaxHeight()
                    .background(PrimaryAccent.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun LineSelector(
    currentLineIndex: Int,
    lineCount: Int,
    onLineSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(vertical = 4.dp, horizontal = 12.dp)
                .background(CardSurface, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Editing Line ${currentLineIndex + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryAccent,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Line",
                tint = PrimaryAccent
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardSurface)
        ) {
            for (i in 0 until lineCount) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Line ${i + 1}",
                            color = if (i == currentLineIndex) PrimaryAccent else TextPrimary
                        )
                    },
                    onClick = {
                        onLineSelected(i)
                        expanded = false
                    },
                    trailingIcon = if (i == currentLineIndex) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryAccent) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun ComposerHeader(
    title: String,
    onBack: () -> Unit,
    onDeleteLastNote: () -> Unit,
    onClearLine: () -> Unit,
    onDeleteLine: () -> Unit,
    onFinish: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = if (isLandscape) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDeleteLastNote) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete Last Note", tint = TextMuted)
            }
            IconButton(onClick = onClearLine) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Line", tint = TextMuted)
            }
            IconButton(onClick = onDeleteLine) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Line", tint = ErrorAccent)
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = TextMuted.copy(alpha = 0.2f)
            )

            TextButton(
                onClick = onFinish,
                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryAccent)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Finish", fontWeight = FontWeight.Bold)
            }
        }
    }
}
