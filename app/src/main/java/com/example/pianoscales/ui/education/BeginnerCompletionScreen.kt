package com.example.pianoscales.ui.education

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BeginnerCompletionScreen(
    onContinueToJourney: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SuccessAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = SuccessAccent,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "🎓 Beginner Journey Complete",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You've built the foundations of music learning. Explore the dashboard to continue practicing scales, theory, chords, ear training, and guided exercises.\n\nNeed a refresher? You can revisit these beginner lessons anytime.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                SummaryItem(Icons.Default.Check, "5 Lessons Finished")
                Spacer(modifier = Modifier.height(12.dp))
                SummaryItem(Icons.Default.Check, "Musical Notes Learned")
                Spacer(modifier = Modifier.height(12.dp))
                SummaryItem(Icons.Default.Check, "Major & Minor Scales Explored")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onContinueToJourney,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Explore Dashboard",
                color = PrimaryBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SuccessAccent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
}
