package com.pianoscales.learnmusic.ui.songs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.ui.freestyle.FreestylePiano
import com.pianoscales.learnmusic.ui.theme.*

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
        ) {
            // Header
            ComposerHeader(
                title = uiState.title,
                onBack = { 
                    if (uiState.isEditMode) onBack() 
                    else viewModel.updateTitle(uiState.title) 
                },
                onUndo = { viewModel.undo() },
                onFinish = { viewModel.finish(onBack) }
            )

            // Current Line Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isLandscape) 0.25f else 0.35f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Line ${uiState.currentLineIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        uiState.currentLine.forEach { noteWithOctave ->
                            Text(
                                text = "${noteWithOctave.note.displayName}${noteWithOctave.octave}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        if (uiState.currentLine.isEmpty()) {
                            Text(
                                text = "Start playing to record notes...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { viewModel.nextLine() },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Next Line")
                    }
                }
            }

            // Keyboard Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isLandscape) 0.75f else 0.65f)
                    .background(CardSurface)
            ) {
                Column {
                    FreestylePiano(
                        onNoteClick = { note, octave -> viewModel.onNotePlayed(note, octave) },
                        height = if (isLandscape) 280.dp else 240.dp,
                        enabled = uiState.pianoMode == PianoMode.VIRTUAL
                    )
                    
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
                selected = pianoMode == PianoMode.EXTERNAL,
                onClick = { onPianoModeChange(PianoMode.EXTERNAL) },
                icon = "🎹",
                title = "External Piano",
                description = "Record notes from your real piano via microphone."
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onContinue,
                enabled = title.isNotBlank(),
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
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryAccent.copy(alpha = 0.1f) else CardSurface
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryAccent) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) PrimaryAccent else TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ComposerHeader(
    title: String,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        Row {
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Refresh, contentDescription = "Undo", tint = TextMuted)
            }
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
