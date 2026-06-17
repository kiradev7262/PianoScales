package com.example.pianoscales.ui.me

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pianoscales.ui.theme.PrimaryBackground
import com.example.pianoscales.ui.theme.TextPrimary
import com.example.pianoscales.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(20.dp)
        ) {
            PolicySection(
                title = "Overview",
                content = "PianoScales is designed to be a private and local learning experience. Your data belongs to you."
            )
            PolicySection(
                title = "Data Collection",
                content = "We do not require account creation. All your progress, including learned notes and scales, is stored locally on your device."
            )
            PolicySection(
                title = "Microphone Usage",
                content = "PianoScales uses the microphone to detect the pitch of your piano playing during practice sessions. This audio is processed in real-time on your device and is never recorded, stored, or transmitted."
            )
            PolicySection(
                title = "Camera Usage",
                content = "The camera is used only if you choose to take a profile photo. This photo is stored locally on your device and is not shared with anyone."
            )
            PolicySection(
                title = "Embedded Content",
                content = "The app includes embedded YouTube videos for educational purposes. Interacting with these videos is subject to YouTube's Privacy Policy."
            )
            PolicySection(
                title = "Third-Party Services",
                content = "PianoScales does not use third-party tracking, analytics, or advertising services that collect your personal information."
            )
            PolicySection(
                title = "Your Rights",
                content = "Since all data is stored locally, you have full control over it. You can clear your progress at any time within the app or by clearing the app data in your device settings."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Last Updated: November 2024",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
