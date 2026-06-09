package com.example.pianoscales.audio_intelligence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pianoscales.ui.theme.CardSurface
import com.example.pianoscales.ui.theme.PrimaryAccent
import com.example.pianoscales.ui.theme.TextMuted
import com.example.pianoscales.ui.theme.TextPrimary

@Composable
fun AudioIntelligenceScreen(
    onNavigateToEarTraining: () -> Unit,
    onNavigateToVoiceTraining: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Audio Intelligence",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FeatureCard(
                    title = "Ear Training",
                    description = "Identify notes and intervals by ear.",
                    onClick = onNavigateToEarTraining
                )
            }
            item {
                FeatureCard(
                    title = "Voice Training",
                    description = "Train your pitch accuracy using the microphone.",
                    onClick = onNavigateToVoiceTraining
                )
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = TextMuted
            )
        }
    }
}
