package com.pianoscales.learnmusic.ui.pianobuddy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.ble.BleConnectionState
import com.pianoscales.learnmusic.ui.components.PianoScalesHomeTopBar
import com.pianoscales.learnmusic.ui.theme.*

@Composable
fun PianoBuddyHomeScreen(
    onNavigateToFreestyle: () -> Unit,
    viewModel: PianoBuddyViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected = connectionState == BleConnectionState.CONNECTED

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            PianoScalesHomeTopBar(title = "Piano Buddy", showAvatar = false)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Bluetooth Status",
                style = MaterialTheme.typography.titleMedium,
                color = TextMuted
            )

            StatusIndicator(connectionState)

            Button(
                onClick = {
                    if (isConnected) viewModel.disconnect() else viewModel.connect()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color.Gray else PrimaryAccent
                )
            ) {
                Text(if (isConnected) "Disconnect Piano Buddy" else "Connect Piano Buddy")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = CardSurface)

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { if (isConnected) onNavigateToFreestyle() },
                enabled = isConnected,
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) CardSurface else CardSurface.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Freestyle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) TextPrimary else TextMuted
                    )
                    Text(
                        text = "Play freely and see it on your LEDs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isConnected) TextMuted else TextMuted.copy(alpha = 0.5f)
                    )
                }
            }

            if (!isConnected) {
                Text(
                    text = "Connect Piano Buddy via Bluetooth to enable Freestyle",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryAccent
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(state: BleConnectionState) {
    val (text, color) = when (state) {
        BleConnectionState.DISCONNECTED -> "Disconnected" to Color.Red
        BleConnectionState.CONNECTING -> "Connecting..." to Color.Yellow
        BleConnectionState.CONNECTED -> "Connected" to Color.Green
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
