package com.pianoscales.learnmusic.ui.pianobuddy

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.BuildConfig
import com.pianoscales.learnmusic.ble.BleConnectionState
import com.pianoscales.learnmusic.ble.BleDevice
import com.pianoscales.learnmusic.ui.components.PianoScalesHomeTopBar
import com.pianoscales.learnmusic.ui.theme.*

@Composable
fun PianoBuddyHomeScreen(
    onNavigateToFreestyle: () -> Unit,
    viewModel: PianoBuddyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isConnected = connectionState == BleConnectionState.CONNECTED

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.startScan()
        }
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.startScan()
    }

    fun handleConnect() {
        val hasPermissions = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermissions) {
            viewModel.startScan()
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusIndicator(connectionState)

            // Connection Actions
            when (connectionState) {
                BleConnectionState.BLUETOOTH_DISABLED -> {
                    Button(
                        onClick = {
                            bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("Enable Bluetooth")
                    }
                }
                BleConnectionState.TIMEOUT -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { handleConnect() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Retry Connection")
                        }
                        var showHelpDialog by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent)
                        ) {
                            Text("Help & Troubleshooting")
                        }
                        
                        if (showHelpDialog) {
                            AlertDialog(
                                onDismissRequest = { showHelpDialog = false },
                                title = { Text("Connection Help") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("1. Ensure your Piano Buddy (ESP32) is powered on.")
                                        Text("2. Make sure it's within 5-10 meters.")
                                        Text("3. Check if it's already connected to another device.")
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showHelpDialog = false }) {
                                        Text("Got it")
                                    }
                                }
                            )
                        }
                    }
                }
                BleConnectionState.FAILED, BleConnectionState.DISCONNECTED, BleConnectionState.IDLE -> {
                    Button(
                        onClick = { handleConnect() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (connectionState == BleConnectionState.IDLE) "Connect Piano Buddy" else "Retry Connection")
                    }
                }
                BleConnectionState.CONNECTED -> {
                    Button(
                        onClick = { viewModel.disconnect() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Disconnect Piano Buddy")
                    }
                }
                else -> {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            }

            // Discovered Devices List
            AnimatedVisibility(
                visible = (connectionState == BleConnectionState.SCANNING || connectionState == BleConnectionState.DEVICE_FOUND) 
                        && discoveredDevices.isNotEmpty()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Discovered Devices",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurface)
                    ) {
                        items(discoveredDevices) { device ->
                            DeviceItem(device) {
                                viewModel.stopScan()
                                viewModel.connectToDevice(device)
                            }
                            HorizontalDivider(color = PrimaryBackground.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CardSurface)

            // Freestyle Entry
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
        BleConnectionState.IDLE -> "Ready to connect" to Color.Gray
        BleConnectionState.CHECKING_BLUETOOTH -> "Checking Bluetooth..." to Color.Yellow
        BleConnectionState.BLUETOOTH_DISABLED -> "Bluetooth is turned off" to Color.Red
        BleConnectionState.CHECKING_PERMISSIONS -> "Checking permissions..." to Color.Yellow
        BleConnectionState.SCANNING -> "Searching for Piano Buddy..." to Color.Cyan
        BleConnectionState.DEVICE_FOUND -> "Piano Buddy found" to Color.Blue
        BleConnectionState.CONNECTING -> "Connecting..." to Color.Yellow
        BleConnectionState.DISCOVERING_SERVICES -> "Discovering services..." to Color.Yellow
        BleConnectionState.CONNECTED -> "Connected" to Color.Green
        BleConnectionState.FAILED -> "Connection failed" to Color.Red
        BleConnectionState.DISCONNECTED -> "Disconnected" to Color.Red
        BleConnectionState.TIMEOUT -> "No Piano Buddy found nearby" to Color.Red
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun DeviceItem(device: BleDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = device.name ?: "Unknown Device",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            if (BuildConfig.DEBUG) {
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
        Text(
            text = "RSSI: ${device.rssi}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (device.rssi > -70) Color.Green else Color.Yellow
        )
    }
}

