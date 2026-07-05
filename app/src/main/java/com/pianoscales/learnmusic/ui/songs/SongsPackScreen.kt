package com.pianoscales.learnmusic.ui.songs

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.ui.components.PianoScalesHomeTopBar
import com.pianoscales.learnmusic.ui.theme.*
import com.pianoscales.learnmusic.util.SongExportManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsPackScreen(
    onStartSong: (Song) -> Unit,
    onCreateSong: () -> Unit,
    onEditSong: (Song) -> Unit,
    viewModel: SongsPackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.dismissExport()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.confirmExternalMode()
        } else {
            viewModel.revertToVirtualMode()
            scope.launch {
                snackbarHostState.showSnackbar("Microphone access is required for External Piano Mode.")
            }
        }
    }

    // Handle permission request when state changes to EXTERNAL but permission might not be granted
    // In a real app, we'd check permission status before triggering.
    LaunchedEffect(uiState.pianoMode) {
        if (uiState.pianoMode == PianoMode.EXTERNAL && !uiState.showOnboarding) {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    if (uiState.showOnboarding) {
        ExternalPianoOnboardingDialog(
            onAccept = { dontShowAgain ->
                viewModel.onOnboardingAccepted(dontShowAgain)
            },
            onCancel = {
                viewModel.onOnboardingCancelled()
            }
        )
    }

    Scaffold(
        containerColor = PrimaryBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PianoScalesHomeTopBar(
                title = "Play a Song",
                showAvatar = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            ExternalPianoPreferenceTile(
                isExternalMode = uiState.pianoMode == PianoMode.EXTERNAL,
                onToggle = { viewModel.toggleExternalPianoMode(it) }
            )

            Text(
                text = "Songs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Learn your favorite melodies one note at a time.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            uiState.songs.forEach { song ->
                SongTile(
                    song = song,
                    onClick = { onStartSong(song) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "My Songs",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                if (uiState.customSongs.isNotEmpty()) {
                    TextButton(onClick = { viewModel.startExport() }) {
                        Text("Export", color = PrimaryAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CreateSongTile(onClick = onCreateSong)
            
            Spacer(modifier = Modifier.height(12.dp))

            uiState.customSongs.forEach { song ->
                SongTile(
                    song = song,
                    onClick = { onStartSong(song) },
                    onEdit = { onEditSong(song) },
                    onDelete = { songToDelete = song }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Delete Song") },
            text = { Text("Are you sure you want to delete '${songToDelete?.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        songToDelete?.let { viewModel.deleteSong(it.songId) }
                        songToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (uiState.isExporting) {
        ExportSongsBottomSheet(
            customSongs = uiState.customSongs,
            selectedSongIds = uiState.selectedExportSongIds,
            onDismiss = { viewModel.dismissExport() },
            onToggleSelection = { viewModel.toggleSongSelection(it) },
            onSelectAll = { viewModel.selectAllSongs() },
            onDeselectAll = { viewModel.deselectAllSongs() },
            onExport = {
                val selectedSongs = uiState.customSongs.filter { uiState.selectedExportSongIds.contains(it.songId) }
                val intent = SongExportManager.exportSongs(context, selectedSongs)
                if (intent != null) {
                    shareLauncher.launch(Intent.createChooser(intent, "Export Songs"))
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Failed to export songs.")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSongsBottomSheet(
    customSongs: List<Song>,
    selectedSongIds: Set<String>,
    onDismiss: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onExport: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Songs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                customSongs.forEach { song ->
                    val isSelected = selectedSongIds.contains(song.songId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSelection(song.songId) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection(song.songId) },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allSelected = selectedSongIds.size == customSongs.size && customSongs.isNotEmpty()
                TextButton(onClick = { if (allSelected) onDeselectAll() else onSelectAll() }) {
                    Text(
                        text = if (allSelected) "Deselect All" else "Select All",
                        color = PrimaryAccent
                    )
                }
                
                Row {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onExport,
                        enabled = selectedSongIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAccent,
                            disabledContainerColor = PrimaryAccent.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateSongTile(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryAccent.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create New Song",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryAccent
            )
        }
    }
}


@Composable
fun ExternalPianoPreferenceTile(
    isExternalMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExternalMode) PrimaryAccent.copy(alpha = 0.1f) else CardSurface
        ),
        border = if (isExternalMode) androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎹",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "External Piano Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Switch(
                    checked = isExternalMode,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryAccent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CardSurface,
                        uncheckedBorderColor = TextMuted
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use your real piano or keyboard to play songs while Piano Scales listens through your microphone.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ExternalPianoOnboardingDialog(
    onAccept: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                "🎹 External Piano Mode",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Use any piano or keyboard near your device.\n\n" +
                    "Piano Scales will listen through your microphone and guide you note-by-note.\n\n" +
                    "For best results:\n" +
                    "• Play in a quiet environment.\n" +
                    "• Virtual keyboard input will be disabled.\n" +
                    "• Progress will advance only from notes detected through the microphone.",
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryAccent)
                    )
                    Text("Don't show again", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAccept(dontShowAgain) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = CardSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

@Composable
fun SongTile(
    song: Song,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.difficulty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryAccent
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Text(
                        text = "${song.lines.size} Lines",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted)
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = PrimaryAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
