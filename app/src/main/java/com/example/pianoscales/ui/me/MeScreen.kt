package com.example.pianoscales.ui.me

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.ui.theme.*
import com.example.pianoscales.util.rememberPermissionHandler
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    viewModel: MeViewModel = hiltViewModel(),
    onNavigateToPrivacy: () -> Unit,
    onNavigateToGoal: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(uiState.displayName) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val file = File(context.filesDir, "profile_image.jpg")
            FileOutputStream(file).use { out ->
                it.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            viewModel.updateProfileImage(file.absolutePath)
        }
    }

    val requestCameraPermission = rememberPermissionHandler(
        permission = android.Manifest.permission.CAMERA,
        permissionName = "Camera",
        snackbarHostState = snackbarHostState
    )

    Scaffold(
        containerColor = PrimaryBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Me", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section 1: Profile
            item {
                ProfileSection(
                    displayName = uiState.displayName,
                    imagePath = uiState.profileImagePath,
                    onImageClick = {
                        requestCameraPermission {
                            cameraLauncher.launch(null)
                        }
                    },
                    onNameClick = { 
                        tempName = uiState.displayName
                        showNameDialog = true 
                    }
                )
            }

            // Section 2: Today's Goal
            item {
                uiState.todayGoal?.let { goal ->
                    TodayGoalCard(goal = goal, onCtaClick = { onNavigateToGoal(goal.route ?: "") })
                }
            }

            // Section 3: Today's Activity
            item {
                TodayActivitySection(activities = uiState.todayActivities)
            }

            // Section 4: Progress Overview
            item {
                ProgressOverviewSection(metrics = uiState.progressMetrics)
            }

            // Section 5: Streaks
            item {
                StreakSection(
                    current = uiState.streakInfo.currentStreak,
                    best = uiState.streakInfo.bestStreak
                )
            }

            // Section 6: Settings
            item {
                SettingsSection(
                    onPrivacyClick = onNavigateToPrivacy,
                    onFeedbackClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:".toUri()
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@pianoscales.app"))
                            putExtra(Intent.EXTRA_SUBJECT, "PianoScales Feedback")
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                )
            }
            
            item {
                Text(
                    text = "Version 1.0.0",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateName(tempName)
                    showNameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileSection(
    displayName: String,
    imagePath: String?,
    onImageClick: () -> Unit,
    onNameClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(CardSurface)
                .clickable { onImageClick() },
            contentAlignment = Alignment.Center
        ) {
            if (imagePath != null) {
                val bitmap = remember(imagePath) { BitmapFactory.decodeFile(imagePath) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = TextMuted)
                }
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = TextMuted)
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .background(PrimaryAccent, CircleShape)
                    .border(2.dp, PrimaryBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBackground)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onNameClick() }
        ) {
            Text(
                text = "Hello, $displayName 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryAccent)
        }
    }
}

@Composable
private fun TodayGoalCard(goal: TodayGoal, onCtaClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryAccent.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TODAY'S GOAL",
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryAccent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = goal.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onCtaClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(goal.ctaText, color = PrimaryBackground, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TodayActivitySection(activities: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Today's Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (activities.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "You haven't practiced today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Ready for 5 minutes of music?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                activities.forEach { activity ->
                    Text(
                        text = activity,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessAccent,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Great work today! 🎉",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProgressOverviewSection(metrics: ProgressMetrics) {
    Column {
        Text(
            text = "Progress Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressMiniCard(
                modifier = Modifier.weight(1f),
                title = "Beginner",
                value = "${(metrics.beginnerJourneyProgress * 100).toInt()}%",
                icon = Icons.Default.Star,
                color = PrimaryAccent
            )
            ProgressMiniCard(
                modifier = Modifier.weight(1f),
                title = "Concepts",
                value = "${metrics.conceptsExplored}/${metrics.totalConcepts}",
                icon = Icons.Default.Info,
                color = SuccessAccent
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressMiniCard(
                modifier = Modifier.weight(1f),
                title = "Scales",
                value = "${metrics.scalesPracticed}",
                icon = Icons.Default.PlayArrow,
                color = Color(0xFF6366F1)
            )
            ProgressMiniCard(
                modifier = Modifier.weight(1f),
                title = "Training",
                value = "${metrics.earTrainingSessions + metrics.voiceTrainingSessions}",
                icon = Icons.Default.Notifications,
                color = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun ProgressMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun StreakSection(current: Int, best: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFFEF3C7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🔥", fontSize = 28.sp)
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Text(
                    text = "$current Days",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Best Streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "⭐ $best",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(onPrivacyClick: () -> Unit, onFeedbackClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column {
            SettingsItem(icon = Icons.Default.Lock, title = "Privacy Policy", onClick = onPrivacyClick)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = ElevatedSurface)
            SettingsItem(icon = Icons.Default.Email, title = "Send Feedback", onClick = onFeedbackClick)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = ElevatedSurface)
            SettingsItem(icon = Icons.Default.Info, title = "Open Source Licenses", onClick = {})
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
    }
}
